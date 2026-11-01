package dev.jdtech.jellyfin.film.presentation.media

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.models.SortBy
import dev.jdtech.jellyfin.repository.JellyfinRepository
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jellyfin.sdk.model.api.BaseItemKind

data class GenreSection(
    val id: UUID,
    val name: String,
    val items: List<FindroidItem>,
)

data class GenresState(
    val sections: List<GenreSection> = emptyList(),
    val isLoading: Boolean = false,
    val error: Exception? = null,
)

@HiltViewModel
class GenresViewModel @Inject constructor(private val repository: JellyfinRepository) :
    ViewModel() {
    private val _state = MutableStateFlow(GenresState())
    val state = _state.asStateFlow()

    private var loadedKind: MediaKind? = null

    fun load(kind: MediaKind, force: Boolean = false) {
        if (loadedKind == kind && !force) return
        if (_state.value.isLoading) return
        // Switching between Movies and Shows must not show the other kind's sections while the
        // new list loads.
        if (loadedKind != null && loadedKind != kind) _state.value = GenresState()
        loadedKind = kind
        viewModelScope.launch(Dispatchers.IO) {
            // Keep the current sections on screen while revalidating, so a pull-to-refresh or
            // retry behaves like the other list pages instead of blanking the tab.
            _state.emit(_state.value.copy(isLoading = true, error = null))
            try {
                val includeType =
                    if (kind == MediaKind.Movies) BaseItemKind.MOVIE else BaseItemKind.SERIES
                val genres = repository.getGenres(includeTypes = listOf(includeType))
                val sections = genres.mapNotNull { genre ->
                    val items =
                        try {
                            repository.getItems(
                                includeTypes = listOf(includeType),
                                recursive = true,
                                sortBy = SortBy.NAME,
                                genreIds = listOf(genre.id),
                                limit = SECTION_LIMIT,
                            )
                        } catch (_: Exception) {
                            emptyList()
                        }
                    if (items.isEmpty()) {
                        null
                    } else {
                        GenreSection(id = genre.id, name = genre.name, items = items)
                    }
                }
                _state.emit(_state.value.copy(sections = sections, isLoading = false))
            } catch (e: Exception) {
                _state.emit(_state.value.copy(isLoading = false, error = e))
            }
        }
    }

    fun retry(kind: MediaKind) {
        load(kind, force = true)
    }

    private companion object {
        const val SECTION_LIMIT = 18
    }
}
