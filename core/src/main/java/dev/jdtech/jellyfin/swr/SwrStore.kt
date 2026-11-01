package dev.jdtech.jellyfin.swr

import dev.jdtech.jellyfin.database.QueryCacheEntity
import dev.jdtech.jellyfin.database.ServerDatabaseDao
import dev.jdtech.jellyfin.settings.domain.AppPreferences
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

/** The visible state of one cached query. */
data class SwrEntry<T>(
    val data: T? = null,
    val error: Throwable? = null,
    val isRefreshing: Boolean = false,
    val updatedAt: Long = 0L,
    val isStale: Boolean = true,
    val isOffline: Boolean = false,
) {
    val hasData: Boolean
        get() = data != null
}

/** Emitted when a refresh fails, for the app-level snackbar. */
data class SwrFailure(
    val key: SwrKey,
    val error: Throwable,
    val isOffline: Boolean,
    val hasData: Boolean,
)

/**
 * A small app-wide stale-while-revalidate store.
 *
 * Reads return the last persisted value immediately. Refreshes are keyed, deduplicated, retain the
 * old value on failure, and persist every successful payload in Room. Mutations update a cached
 * payload in place; invalidations mark it stale without discarding it.
 */
@Singleton
class SwrStore
@Inject
constructor(
    private val database: ServerDatabaseDao,
    private val appPreferences: AppPreferences,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        classDiscriminator = "_type"
    }

    private val entries = ConcurrentHashMap<SwrKey, MutableStateFlow<SwrEntry<Any?>>>()
    private val loadedKeys = ConcurrentHashMap.newKeySet<SwrKey>()
    private val loadMutex = Mutex()
    private val inFlight = ConcurrentHashMap<SwrKey, Job>()

    private val _failures =
        MutableSharedFlow<SwrFailure>(
            extraBufferCapacity = 16,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )
    val failures = _failures.asSharedFlow()

    init {
        // Offline mode changes the repository that answers a query, so every cached result must be
        // revalidated instead of silently serving the other mode's data.
        scope.launch {
            appPreferences.observe(appPreferences.offlineMode).drop(1).collect { invalidateAll() }
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> observe(key: SwrKey, serializer: KSerializer<T>): StateFlow<SwrEntry<T>> {
        val flow = flow(key)
        scope.launch { ensureLoaded(key, serializer) }
        return flow as StateFlow<SwrEntry<T>>
    }

    /**
     * Shows cached data immediately and fetches only when [force] is true or the cached entry is
     * older than the global stale threshold. An in-flight request for the same key is reused.
     */
    fun <T> refresh(
        key: SwrKey,
        serializer: KSerializer<T>,
        force: Boolean = false,
        fetcher: suspend () -> T,
    ) {
        if (inFlight[key]?.isActive == true) return
        inFlight[key] = scope.launch {
            try {
                ensureLoaded(key, serializer)
                val current = flow(key).value
                if (!force && current.data != null && !current.isStale) {
                    return@launch
                }
                flow(key).value = current.copy(isRefreshing = true, error = null, isOffline = false)

                val result = fetcher()
                val fetchedAt = System.currentTimeMillis()
                flow(key).value =
                    SwrEntry(
                        data = result,
                        updatedAt = fetchedAt,
                        isStale = false,
                    )
                database.putQueryCache(
                    QueryCacheEntity(
                        cacheKey = key.value,
                        payload = json.encodeToString(serializer, result),
                        updatedAt = fetchedAt,
                    )
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                val current = flow(key).value
                val offline =
                    appPreferences.getValue(appPreferences.offlineMode) || e is IOException
                flow(key).value =
                    current.copy(
                        error = e,
                        isRefreshing = false,
                        isOffline = offline,
                        isStale = if (current.data != null) current.isStale else true,
                    )
                _failures.emit(
                    SwrFailure(
                        key = key,
                        error = e,
                        isOffline = offline,
                        hasData = current.data != null,
                    )
                )
            } finally {
                inFlight.remove(key)
            }
        }
    }

    /**
     * Updates a cached payload immediately; the caller runs the repository mutation alongside it.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> mutate(key: SwrKey, serializer: KSerializer<T>, update: (T?) -> T?) {
        scope.launch {
            ensureLoaded(key, serializer)
            val current = flow(key).value as SwrEntry<T>
            val data = update(current.data)
            val updatedAt = System.currentTimeMillis()
            val updated =
                current.copy(
                    data = data,
                    updatedAt = updatedAt,
                    isStale = false,
                    error = null,
                ) as SwrEntry<Any?>
            flow(key).value = updated
            if (data != null) {
                database.putQueryCache(
                    QueryCacheEntity(
                        cacheKey = key.value,
                        payload = json.encodeToString(serializer, data),
                        updatedAt = updatedAt,
                    )
                )
            }
        }
    }

    /** Marks one entry stale without discarding its data, and persists that fact. */
    fun invalidate(key: SwrKey) {
        scope.launch {
            database.invalidateQueryCache(key.value)
            flow(key).value = flow(key).value.copy(isStale = true)
        }
    }

    /** Marks every entry under [prefix] stale, e.g. all library lists after a favorite change. */
    fun invalidatePrefix(prefix: String) {
        scope.launch {
            database.invalidateQueryCachePrefix(prefix)
            entries.keys
                .filter { it.value.startsWith(prefix) }
                .forEach { flow(it).value = flow(it).value.copy(isStale = true) }
        }
    }

    /** Marks every entry stale; the next read revalidates through the current repository mode. */
    fun invalidateAll() {
        scope.launch {
            database.invalidateAllQueryCache()
            entries.values.forEach { it.value = it.value.copy(isStale = true) }
        }
    }

    /** Invalidates every list that can show favorite/played state after a media mutation. */
    fun invalidateMediaLists() {
        invalidate(QueryKeys.Favorites)
        invalidate(QueryKeys.Downloads)
        invalidate(QueryKeys.Home)
        invalidatePrefix(QueryKeys.LibraryPrefix)
        invalidatePrefix(QueryKeys.GenresPrefix)
        invalidatePrefix(QueryKeys.CollectionsPrefix)
        invalidatePrefix(QueryKeys.ItemPrefix)
    }

    fun clear() {
        inFlight.values.forEach { it.cancel() }
        inFlight.clear()
        entries.values.forEach { it.value = SwrEntry() }
        loadedKeys.clear()
        scope.launch { database.clearQueryCache() }
    }

    private fun flow(key: SwrKey): MutableStateFlow<SwrEntry<Any?>> =
        entries.computeIfAbsent(key) { MutableStateFlow(SwrEntry()) }

    private suspend fun <T> ensureLoaded(key: SwrKey, serializer: KSerializer<T>) {
        if (loadedKeys.contains(key)) return
        loadMutex.withLock {
            if (loadedKeys.contains(key)) return
            val entity = database.getQueryCache(key.value)
            if (entity != null) {
                val data = runCatching {
                    json.decodeFromString(serializer, entity.payload)
                }
                    .getOrNull()
                if (data != null) {
                    val now = System.currentTimeMillis()
                    flow(key).value =
                        SwrEntry(
                            data = data,
                            updatedAt = entity.updatedAt,
                            isStale =
                                entity.updatedAt == 0L ||
                                    now - entity.updatedAt >= DEFAULT_STALE_AFTER_MS,
                        )
                }
            }
            loadedKeys.add(key)
        }
    }

    private companion object {
        const val DEFAULT_STALE_AFTER_MS = 5 * 60 * 1000L
    }
}
