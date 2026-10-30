package dev.jdtech.jellyfin.film.presentation.books

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.film.presentation.library.ListDisplayField
import dev.jdtech.jellyfin.models.FindroidCollection
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.models.SortBy
import dev.jdtech.jellyfin.repository.JellyfinRepository
import dev.jdtech.jellyfin.settings.domain.AppPreferences
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jellyfin.sdk.model.api.BaseItemKind

data class BooksState(
    val libraries: List<FindroidCollection> = emptyList(),
    val items: Map<UUID, List<FindroidItem>> = emptyMap(),
    val displayFields: Set<ListDisplayField> = emptySet(),
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
) : ViewModel() {
    private val _state = MutableStateFlow(BooksState())
    val state = _state.asStateFlow()

    private var loaded = false
    private var openedItemId: UUID? = null

    fun load(force: Boolean = false) {
        if (!force && loaded) return
        loaded = true
        viewModelScope.launch(Dispatchers.IO) {
            _state.emit(
                _state.value.copy(
                    isLoading = true,
                    error = null,
                    displayFields =
                        ListDisplayField.fromKeys(
                            appPreferences.getValue(appPreferences.listDisplayFields)
                        ),
                )
            )
            try {
                // One server-wide query is much faster than walking every library separately.
                val books =
                    repository.getItems(
                        parentId = null,
                        includeTypes = listOf(BaseItemKind.BOOK, BaseItemKind.AUDIO_BOOK),
                        recursive = true,
                        sortBy = SortBy.NAME,
                    )
                val libraries = if (books.isEmpty()) emptyList() else repository.getLibraries()
                _state.emit(
                    _state.value.copy(
                        libraries = libraries,
                        items =
                            if (books.isEmpty()) {
                                emptyMap()
                            } else {
                                mapOf((libraries.firstOrNull()?.id ?: UUID(0, 0)) to books)
                            },
                        isLoading = false,
                    )
                )
            } catch (e: Exception) {
                _state.emit(_state.value.copy(error = e, isLoading = false))
            }
        }
    }

    /** Cached like the movie/show list: load once, refresh only the opened item on return. */
    fun loadIfNeeded() {
        if (_state.value.items.isEmpty() && !_state.value.isLoading) load()
    }

    fun onItemOpened(itemId: UUID) {
        openedItemId = itemId
    }

    fun refreshOpenedItem() {
        val itemId = openedItemId ?: return
        openedItemId = null
        viewModelScope.launch(Dispatchers.IO) {
            val item = repository.getItem(itemId) ?: return@launch
            _state.value =
                _state.value.copy(
                    items =
                        _state.value.items.mapValues { (_, items) ->
                            items.map { cached -> if (cached.id == item.id) item else cached }
                        }
                )
        }
    }

    fun retry() {
        loaded = false
        load()
    }
}
