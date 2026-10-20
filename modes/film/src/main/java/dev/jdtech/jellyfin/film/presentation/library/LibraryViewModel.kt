package dev.jdtech.jellyfin.film.presentation.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.models.CollectionType
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

    fun loadItems() {
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

        viewModelScope.launch {
            _state.emit(_state.value.copy(isLoading = true, error = null))

            initSorting()

            try {
                val items =
                    jellyfinRepository
                        .getItemsPaging(
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
                            onTotalCount = { total ->
                                _state.value = _state.value.copy(itemCount = total)
                            },
                        )
                        .cachedIn(viewModelScope)
                _state.emit(_state.value.copy(items = items))
            } catch (e: Exception) {
                _state.emit(_state.value.copy(error = e))
            }
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
