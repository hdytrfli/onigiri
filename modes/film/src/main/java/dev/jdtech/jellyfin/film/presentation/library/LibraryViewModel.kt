package dev.jdtech.jellyfin.film.presentation.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.models.CollectionType
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.models.SortBy
import dev.jdtech.jellyfin.models.SortOrder
import dev.jdtech.jellyfin.repository.JellyfinRepository
import dev.jdtech.jellyfin.settings.domain.AppPreferences
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jellyfin.sdk.model.api.BaseItemKind

@HiltViewModel
class LibraryViewModel
@Inject
constructor(
    private val jellyfinRepository: JellyfinRepository,
    private val appPreferences: AppPreferences,
) : ViewModel() {
    private val _state = MutableStateFlow(LibraryState())
    val state = _state.asStateFlow()

    var parentId: UUID? = null
    lateinit var libraryType: CollectionType
    var genreIds: List<UUID> = emptyList()

    lateinit var sortBy: SortBy
    lateinit var sortOrder: SortOrder

    private var searchQuery: String = ""
    private var searchJob: Job? = null

    fun setup(
        parentId: UUID?,
        libraryType: CollectionType,
        genreIds: List<UUID> = emptyList(),
    ) {
        this.parentId = parentId
        this.libraryType = libraryType
        this.genreIds = genreIds
        _state.value =
            _state.value.copy(
                displayFields =
                    ListDisplayField.fromKeys(
                        appPreferences.getValue(appPreferences.listDisplayFields)
                    )
            )
    }

    fun loadItems(cachedItems: List<FindroidItem> = emptyList()) {
        val itemType =
            when (libraryType) {
                CollectionType.Movies -> listOf(BaseItemKind.MOVIE)
                CollectionType.TvShows -> listOf(BaseItemKind.SERIES)
                CollectionType.BoxSets -> listOf(BaseItemKind.BOX_SET)
                CollectionType.Mixed,
                CollectionType.Folders ->
                    listOf(BaseItemKind.FOLDER, BaseItemKind.MOVIE, BaseItemKind.SERIES)
                else -> null
            }

        val recursive = itemType == null || !itemType.contains(BaseItemKind.FOLDER)

        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.emit(
                _state.value.copy(
                    items = if (_state.value.items.isEmpty()) cachedItems else _state.value.items,
                    isLoading = true,
                    error = null,
                )
            )

            initSorting()

            try {
                // The whole list is fetched into the ViewModel rather than paged: the cache has
                // to be available synchronously when the page is revisited, otherwise the grid
                // lays out empty first and loses the scroll position it restores.
                val items =
                    jellyfinRepository.getItems(
                        parentId = parentId,
                        includeTypes = itemType,
                        recursive = recursive,
                        sortBy =
                            if (
                                libraryType == CollectionType.TvShows &&
                                    sortBy == SortBy.DATE_PLAYED
                            )
                                SortBy.SERIES_DATE_PLAYED
                            else sortBy, // Jellyfin uses a different enum for sorting series by
                        // data played
                        sortOrder = sortOrder,
                        searchTerm = searchQuery.ifBlank { null },
                        genreIds = genreIds.takeIf { it.isNotEmpty() },
                    )
                _state.emit(_state.value.copy(items = items, isLoading = false))
            } catch (e: Exception) {
                _state.emit(_state.value.copy(isLoading = false, error = e))
            }
        }
    }

    /** Loads on a screen's first entry; a revisit with a cached list does not reload it. */
    fun loadItemsIfNeeded() {
        if (_state.value.items.isEmpty() && !_state.value.isLoading) {
            loadItems()
        }
    }

    /** The current list fetch, cancelled when the query changes so only the last one lands. */
    private var loadJob: Job? = null

    /** The item whose detail page was last opened; refreshed once when this page resumes. */
    private var openedItemId: UUID? = null

    /**
     * Re-fetches only the item whose detail page was opened, so its updated play state shows on
     * return without reloading the list (which would reset the scroll position).
     */
    private fun refreshOpenedItem() {
        val itemId = openedItemId ?: return
        openedItemId = null
        viewModelScope.launch {
            val item = jellyfinRepository.getItem(itemId) ?: return@launch
            _state.value =
                _state.value.copy(
                    items =
                        _state.value.items.map { cached ->
                            if (cached.id == item.id) item else cached
                        }
                )
        }
    }

    private suspend fun initSorting() {
        if (!::sortBy.isInitialized || !::sortOrder.isInitialized) {
            sortBy = SortBy.fromString(appPreferences.getValue(appPreferences.sortBy))
            sortOrder = SortOrder.fromString(appPreferences.getValue(appPreferences.sortOrder))
            _state.emit(_state.value.copy(sortBy = sortBy, sortOrder = sortOrder))
        }
    }

    private fun setSorting(sortBy: SortBy, sortOrder: SortOrder) {
        this.sortBy = sortBy
        this.sortOrder = sortOrder
        viewModelScope.launch {
            _state.emit(_state.value.copy(sortBy = sortBy, sortOrder = sortOrder))
            appPreferences.setValue(appPreferences.sortBy, sortBy.toString())
            appPreferences.setValue(appPreferences.sortOrder, sortOrder.toString())
        }
    }

    fun onAction(action: LibraryAction) {
        when (action) {
            is LibraryAction.OnItemClick -> openedItemId = action.item.id
            is LibraryAction.RefreshOpenedItem -> refreshOpenedItem()
            is LibraryAction.RefreshItems -> loadItems()
            is LibraryAction.ChangeSorting -> {
                if (action.sortBy != this.sortBy || action.sortOrder != this.sortOrder) {
                    setSorting(sortBy = action.sortBy, sortOrder = action.sortOrder)
                    loadItems()
                }
            }
            is LibraryAction.ToggleDisplayField -> {
                val fields = _state.value.displayFields.toMutableSet()
                if (!fields.add(action.field)) {
                    fields.remove(action.field)
                }
                appPreferences.setValue(
                    appPreferences.listDisplayFields,
                    fields.map { it.key }.toSet(),
                )
                _state.value = _state.value.copy(displayFields = fields)
            }
            is LibraryAction.SetSearchQuery -> {
                searchQuery = action.query
                _state.value = _state.value.copy(searchQuery = action.query)
                searchJob?.cancel()
                searchJob = viewModelScope.launch {
                    delay(SEARCH_DEBOUNCE_MS)
                    loadItems()
                }
            }
            is LibraryAction.ToggleSearch -> {
                val expanded = !_state.value.searchExpanded
                if (!expanded) {
                    searchQuery = ""
                    searchJob?.cancel()
                    loadItems()
                }
                _state.value =
                    _state.value.copy(
                        searchExpanded = expanded,
                        searchQuery = if (expanded) _state.value.searchQuery else "",
                    )
            }
            else -> Unit
        }
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 300L
    }
}
