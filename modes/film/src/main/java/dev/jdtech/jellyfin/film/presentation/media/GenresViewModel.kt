package dev.jdtech.jellyfin.film.presentation.media

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.models.FindroidGenre
import dev.jdtech.jellyfin.repository.JellyfinRepository
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jellyfin.sdk.model.api.BaseItemKind

@HiltViewModel
class GenresViewModel @Inject constructor(private val repository: JellyfinRepository) :
    ViewModel() {
    private val _state = MutableStateFlow<List<FindroidGenre>>(emptyList())
    val state = _state.asStateFlow()

    private var loadedKind: MediaKind? = null

    fun load(kind: MediaKind) {
        if (loadedKind == kind) return
        loadedKind = kind
        viewModelScope.launch(Dispatchers.IO) {
            _state.emit(
                try {
                    repository.getGenres(
                        includeTypes =
                            listOf(
                                if (kind == MediaKind.Movies) BaseItemKind.MOVIE
                                else BaseItemKind.SERIES
                            )
                    )
                } catch (_: Exception) {
                    emptyList()
                }
            )
        }
    }
}
