package dev.jdtech.jellyfin.film.presentation.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.models.FindroidMovie
import dev.jdtech.jellyfin.models.FindroidShow
import dev.jdtech.jellyfin.models.SortBy
import dev.jdtech.jellyfin.repository.JellyfinRepository
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jellyfin.sdk.model.api.BaseItemKind

data class CollectionsState(
    val movies: List<FindroidItem> = emptyList(),
    val shows: List<FindroidItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: Exception? = null,
)

/**
 * The Collections page groups box sets by what they contain: Jellyfin's box-set DTO carries no
 * movie/show kind, so each set is classified by its first movie or series child.
 */
@HiltViewModel
class CollectionsViewModel @Inject constructor(private val repository: JellyfinRepository) :
    ViewModel() {
    private val _state = MutableStateFlow(CollectionsState())
    val state = _state.asStateFlow()

    private var loaded = false

    fun load() {
        if (loaded) return
        loaded = true
        viewModelScope.launch(Dispatchers.IO) {
            _state.emit(_state.value.copy(isLoading = true, error = null))
            try {
                val boxSets =
                    repository.getItems(
                        includeTypes = listOf(BaseItemKind.BOX_SET),
                        recursive = true,
                        sortBy = SortBy.NAME,
                    )
                val movies = mutableListOf<FindroidItem>()
                val shows = mutableListOf<FindroidItem>()
                for (boxSet in boxSets) {
                    val first =
                        repository
                            .getItems(
                                parentId = boxSet.id,
                                includeTypes = listOf(BaseItemKind.MOVIE, BaseItemKind.SERIES),
                                recursive = false,
                                limit = 1,
                            )
                            .firstOrNull()
                    when (first) {
                        is FindroidMovie -> movies += boxSet
                        is FindroidShow -> shows += boxSet
                        else -> Unit
                    }
                }
                _state.emit(_state.value.copy(movies = movies, shows = shows, isLoading = false))
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
