package dev.jdtech.jellyfin.film.presentation.books

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.film.presentation.library.ListDisplayField
import dev.jdtech.jellyfin.models.CollectionType
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

    fun load() {
        if (loaded) return
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
                val libraries = repository.getLibraries().filter { it.type == CollectionType.Books }
                val items = libraries.associate { library ->
                    library.id to
                        repository.getItems(
                            parentId = library.id,
                            includeTypes = listOf(BaseItemKind.BOOK),
                            recursive = true,
                            sortBy = SortBy.NAME,
                        )
                }
                _state.emit(
                    _state.value.copy(
                        libraries = libraries,
                        items = items,
                        isLoading = false,
                    )
                )
            } catch (e: Exception) {
                _state.emit(_state.value.copy(error = e, isLoading = false))
            }
        }
    }

    fun retry() {
        loaded = false
        load()
    }
}
