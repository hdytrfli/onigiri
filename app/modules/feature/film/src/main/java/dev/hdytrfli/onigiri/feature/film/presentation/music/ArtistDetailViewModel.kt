package dev.hdytrfli.onigiri.feature.film.presentation.music

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.hdytrfli.onigiri.models.FindroidMusicAlbum
import dev.hdytrfli.onigiri.models.FindroidMusicArtist
import dev.hdytrfli.onigiri.repository.JellyfinRepository
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ArtistDetailState(
    val artist: FindroidMusicArtist? = null,
    val albums: List<FindroidMusicAlbum> = emptyList(),
    val isLoading: Boolean = false,
    val error: Exception? = null,
)

@HiltViewModel
class ArtistDetailViewModel @Inject constructor(private val repository: JellyfinRepository) :
    ViewModel() {
    private val _state = MutableStateFlow(ArtistDetailState())
    val state = _state.asStateFlow()

    private var loadJob: Job? = null
    private var loadedId: UUID? = null

    fun load(artistId: UUID, force: Boolean = false) {
        if (!force && loadedId == artistId) return
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            try {
                val (artist, albums) =
                    coroutineScope {
                        val artist = async { repository.getMusicArtist(artistId) }
                        val albums = async { repository.getMusicAlbums(artistId) }
                        artist.await() to albums.await()
                    }

                loadedId = artistId
                _state.value =
                    ArtistDetailState(
                        artist = artist,
                        albums = albums,
                    )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e) }
            }
        }
    }

    fun retry(artistId: UUID) = load(artistId, force = true)
}
