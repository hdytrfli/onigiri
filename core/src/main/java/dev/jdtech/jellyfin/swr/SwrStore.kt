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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flatMapLatest
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
    private val logicalEntries = ConcurrentHashMap<SwrKey, MutableStateFlow<SwrEntry<Any?>>>()
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

    @OptIn(ExperimentalCoroutinesApi::class)
    @Suppress("UNCHECKED_CAST")
    fun <T> observe(key: SwrKey, serializer: KSerializer<T>): StateFlow<SwrEntry<T>> {
        val logical = logicalEntries.computeIfAbsent(key) { MutableStateFlow(SwrEntry()) }
        scope.launch {
            appPreferences
                .observe(appPreferences.offlineMode)
                .flatMapLatest { offlineMode -> rawFlow(scoped(key, offlineMode)) }
                .collect { logical.value = it }
        }
        scope.launch { ensureLoaded(key, serializer) }
        return logical as StateFlow<SwrEntry<T>>
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
        val storageKey = scoped(key)
        if (inFlight[storageKey]?.isActive == true) return
        inFlight[storageKey] = scope.launch {
            val flow = rawFlow(storageKey)
            try {
                ensureLoaded(key, serializer)
                val current = flow.value
                if (!force && current.data != null && !current.isStale) {
                    return@launch
                }
                flow.value = current.copy(isRefreshing = true, error = null, isOffline = false)

                val result = fetcher()
                val fetchedAt = System.currentTimeMillis()
                flow.value =
                    SwrEntry(
                        data = result,
                        updatedAt = fetchedAt,
                        isStale = false,
                    )
                database.putQueryCache(
                    QueryCacheEntity(
                        cacheKey = storageKey.value,
                        payload = json.encodeToString(serializer, result),
                        updatedAt = fetchedAt,
                    )
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                val current = flow.value
                val offline =
                    appPreferences.getValue(appPreferences.offlineMode) || e is IOException
                flow.value =
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
                inFlight.remove(storageKey)
            }
        }
    }

    /**
     * Updates a cached payload immediately; the caller runs the repository mutation alongside it.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> mutate(key: SwrKey, serializer: KSerializer<T>, update: (T?) -> T?) {
        val storageKey = scoped(key)
        scope.launch {
            ensureLoaded(key, serializer)
            val flow = rawFlow(storageKey)
            val current = flow.value as SwrEntry<T>
            val data = update(current.data)
            val updatedAt = System.currentTimeMillis()
            val updated =
                current.copy(
                    data = data,
                    updatedAt = updatedAt,
                    isStale = false,
                    error = null,
                ) as SwrEntry<Any?>
            flow.value = updated
            if (data != null) {
                database.putQueryCache(
                    QueryCacheEntity(
                        cacheKey = storageKey.value,
                        payload = json.encodeToString(serializer, data),
                        updatedAt = updatedAt,
                    )
                )
            }
        }
    }

    /** Marks one entry stale in both online and offline caches, without discarding its data. */
    fun invalidate(key: SwrKey) {
        scope.launch {
            MODES.forEach { mode ->
                val storageKey = SwrKey("$mode|${key.value}")
                database.invalidateQueryCache(storageKey.value)
                rawFlow(storageKey).value = rawFlow(storageKey).value.copy(isStale = true)
            }
        }
    }

    /** Marks every entry under [prefix] stale in both modes. */
    fun invalidatePrefix(prefix: String) {
        scope.launch {
            MODES.forEach { mode -> database.invalidateQueryCachePrefix("$mode|$prefix") }
            entries.keys
                .filter { it.value.substringAfter('|').startsWith(prefix) }
                .forEach { rawFlow(it).value = rawFlow(it).value.copy(isStale = true) }
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
        logicalEntries.values.forEach { it.value = SwrEntry() }
        loadedKeys.clear()
        scope.launch { database.clearQueryCache() }
    }

    private fun scoped(key: SwrKey): SwrKey =
        scoped(key, appPreferences.getValue(appPreferences.offlineMode))

    private fun scoped(key: SwrKey, offlineMode: Boolean): SwrKey =
        SwrKey("${if (offlineMode) "offline" else "online"}|${key.value}")

    private fun rawFlow(key: SwrKey): MutableStateFlow<SwrEntry<Any?>> =
        entries.computeIfAbsent(key) { MutableStateFlow(SwrEntry()) }

    private suspend fun <T> ensureLoaded(key: SwrKey, serializer: KSerializer<T>) {
        val storageKey = scoped(key)
        if (loadedKeys.contains(storageKey)) return
        val flow = rawFlow(storageKey)
        loadMutex.withLock {
            if (loadedKeys.contains(storageKey)) return
            val entity = database.getQueryCache(storageKey.value)
            if (entity != null) {
                val data = runCatching {
                    json.decodeFromString(serializer, entity.payload)
                }
                    .getOrNull()
                if (data != null) {
                    val now = System.currentTimeMillis()
                    flow.value =
                        SwrEntry(
                            data = data,
                            updatedAt = entity.updatedAt,
                            isStale =
                                entity.updatedAt == 0L ||
                                    now - entity.updatedAt >= DEFAULT_STALE_AFTER_MS,
                        )
                }
            }
            loadedKeys.add(storageKey)
        }
    }

    private companion object {
        const val DEFAULT_STALE_AFTER_MS = 5 * 60 * 1000L
        val MODES = listOf("online", "offline")
    }
}
