package dev.hdytrfli.onigiri.feature.film.presentation.music

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.hdytrfli.onigiri.models.FindroidMusicAlbum
import dev.hdytrfli.onigiri.models.FindroidMusicArtist
import dev.hdytrfli.onigiri.models.FindroidMusicSong
import dev.hdytrfli.onigiri.repository.JellyfinRepository
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MusicState(
    val songs: List<FindroidMusicSong> = emptyList(),
    val artists: List<FindroidMusicArtist> = emptyList(),
    val albums: List<FindroidMusicAlbum> = emptyList(),
    val isLoading: Boolean = false,
    val error: Exception? = null,
)

@HiltViewModel
class MusicViewModel @Inject constructor(private val repository: JellyfinRepository) : ViewModel() {
    private val _state = MutableStateFlow(MusicState())
    val state = _state.asStateFlow()

    private var loadJob: Job? = null
    private var loaded = false

    fun load(force: Boolean = false) {
        if (!force && loaded) return
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            try {
                val (songs, artists, albums) =
                    coroutineScope {
                        val songs = async { repository.getMusicSongs() }
                        val artists = async { repository.getMusicArtists() }
                        val albums = async { repository.getMusicAlbums() }
                        Triple(songs.await(), artists.await(), albums.await())
                    }

                loaded = true
                _state.value =
                    MusicState(
                        songs = songs,
                        artists = artists,
                        albums = albums,
                    )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e) }
            }
        }
    }

    fun retry() = load(force = true)
}
