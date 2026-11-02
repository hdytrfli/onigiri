package dev.hdytrfli.onigiri.feature.film.presentation.books

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.hdytrfli.onigiri.feature.film.presentation.library.ListDisplayField
import dev.hdytrfli.onigiri.models.FindroidCollection
import dev.hdytrfli.onigiri.models.FindroidItem
import dev.hdytrfli.onigiri.models.SortBy
import dev.hdytrfli.onigiri.models.SortOrder
import dev.hdytrfli.onigiri.repository.JellyfinRepository
import dev.hdytrfli.onigiri.settings.domain.AppPreferences
import dev.hdytrfli.onigiri.swr.QueryKeys
import dev.hdytrfli.onigiri.swr.SwrEntry
import dev.hdytrfli.onigiri.swr.SwrKey
import dev.hdytrfli.onigiri.swr.SwrStore
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.jellyfin.sdk.model.api.BaseItemKind

val BOOK_SORT_OPTIONS = listOf(SortBy.NAME, SortBy.RELEASE_DATE)

@Serializable
data class BooksCache(
    val libraries: List<FindroidCollection> = emptyList(),
    val books: List<FindroidItem> = emptyList(),
)

data class BooksState(
    val libraries: List<FindroidCollection> = emptyList(),
    val items: Map<UUID, List<FindroidItem>> = emptyMap(),
    val displayFields: Set<ListDisplayField> = emptySet(),
    val sortBy: SortBy = SortBy.NAME,
    val sortOrder: SortOrder = SortOrder.ASCENDING,
    val isLoading: Boolean = false,
    val error: Exception? = null,
)

/**
 * The Books root page: every library of type Books, with its books. Libraries are few, so they are
 * all loaded at once and the page switches between them with its toolbar tabs.
 */
@HiltViewModel
class BooksViewModel
@Inject
constructor(
    private val repository: JellyfinRepository,
    private val appPreferences: AppPreferences,
    private val swrStore: SwrStore,
) : ViewModel() {
    private val _state = MutableStateFlow(BooksState())
    val state = _state.asStateFlow()

    private val serializer = BooksCache.serializer()
    private var observeJob: Job? = null
    private var observedKey: SwrKey? = null
    private var openedItemId: UUID? = null

    fun load(force: Boolean = false) {
        val storedSortBy = SortBy.fromString(appPreferences.getValue(appPreferences.bookSortBy))
        val sortBy = storedSortBy.takeIf { it in BOOK_SORT_OPTIONS } ?: SortBy.NAME
        val sortOrder = SortOrder.fromString(appPreferences.getValue(appPreferences.bookSortOrder))
        if (sortBy != storedSortBy) {
            appPreferences.setValue(appPreferences.bookSortBy, sortBy.toString())
        }
        _state.value =
            _state.value.copy(
                displayFields =
                    ListDisplayField.fromKeys(
                        appPreferences.getValue(appPreferences.listDisplayFields)
                    ),
                sortBy = sortBy,
                sortOrder = sortOrder,
            )
        val key = QueryKeys.books(sortBy, sortOrder)
        if (key != observedKey) {
            observedKey = key
            observeJob?.cancel()
            observeJob = viewModelScope.launch {
                swrStore.observe(key, serializer).collect(::applyEntry)
            }
        }
        swrStore.refresh(key, serializer, force = force) { fetchBooks(sortBy, sortOrder) }
    }

    /** Cached like every other list: SWR decides whether this entry needs revalidation. */
    fun loadIfNeeded() {
        load()
    }

    fun onItemOpened(itemId: UUID) {
        openedItemId = itemId
    }

    fun refreshOpenedItem() {
        val itemId = openedItemId ?: return
        openedItemId = null
        viewModelScope.launch {
            val item = repository.getItem(itemId) ?: return@launch
            val key = QueryKeys.books(_state.value.sortBy, _state.value.sortOrder)
            swrStore.mutate(key, serializer) { cache ->
                cache?.copy(books = cache.books.map { if (it.id == item.id) item else it })
            }
            _state.value =
                _state.value.copy(
                    items =
                        _state.value.items.mapValues { (_, items) ->
                            items.map { cached -> if (cached.id == item.id) item else cached }
                        }
                )
        }
    }

    fun changeSorting(sortBy: SortBy, sortOrder: SortOrder) {
        if (sortBy !in BOOK_SORT_OPTIONS) return
        appPreferences.setValue(appPreferences.bookSortBy, sortBy.toString())
        appPreferences.setValue(appPreferences.bookSortOrder, sortOrder.toString())
        _state.value = _state.value.copy(sortBy = sortBy, sortOrder = sortOrder)
        load(force = true)
    }

    fun toggleDisplayField(field: ListDisplayField) {
        val fields = _state.value.displayFields.toMutableSet()
        if (!fields.add(field)) {
            fields.remove(field)
        }
        appPreferences.setValue(
            appPreferences.listDisplayFields,
            fields.map { it.key }.toSet(),
        )
        _state.value = _state.value.copy(displayFields = fields)
    }

    fun retry() {
        load(force = true)
    }

    private fun applyEntry(entry: SwrEntry<BooksCache>) {
        val cache = entry.data
        if (cache == null) {
            _state.value =
                _state.value.copy(
                    libraries =
                        if (entry.isRefreshing || entry.error != null) {
                            _state.value.libraries
                        } else {
                            emptyList()
                        },
                    items =
                        if (entry.isRefreshing || entry.error != null) {
                            _state.value.items
                        } else {
                            emptyMap()
                        },
                    isLoading = entry.isRefreshing,
                    error = entry.error as? Exception,
                )
            return
        }
        _state.value =
            _state.value.copy(
                libraries = cache.libraries,
                items =
                    if (cache.books.isEmpty()) {
                        emptyMap()
                    } else {
                        mapOf((cache.libraries.firstOrNull()?.id ?: UUID(0, 0)) to cache.books)
                    },
                isLoading = entry.isRefreshing,
                error = entry.error as? Exception,
            )
    }

    private suspend fun fetchBooks(sortBy: SortBy, sortOrder: SortOrder): BooksCache {
        // One server-wide query is much faster than walking every library separately.
        val books =
            repository.getItems(
                parentId = null,
                includeTypes = listOf(BaseItemKind.BOOK, BaseItemKind.AUDIO_BOOK),
                recursive = true,
                sortBy = sortBy,
                sortOrder = sortOrder,
            )
        val libraries = if (books.isEmpty()) emptyList() else repository.getLibraries()
        return BooksCache(libraries = libraries, books = books)
    }
}
