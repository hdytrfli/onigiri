package dev.hdytrfli.onigiri.feature.film.presentation.music

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.hdytrfli.onigiri.models.FindroidMusicAlbum
import dev.hdytrfli.onigiri.models.FindroidMusicSong
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

data class AlbumDetailState(
    val album: FindroidMusicAlbum? = null,
    val songs: List<FindroidMusicSong> = emptyList(),
    val isLoading: Boolean = false,
    val error: Exception? = null,
)

@HiltViewModel
class AlbumDetailViewModel @Inject constructor(private val repository: JellyfinRepository) :
    ViewModel() {
    private val _state = MutableStateFlow(AlbumDetailState())
    val state = _state.asStateFlow()

    private var loadJob: Job? = null
    private var loadedId: UUID? = null

    fun load(albumId: UUID, force: Boolean = false) {
        if (!force && loadedId == albumId) return
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            try {
                val (album, songs) =
                    coroutineScope {
                        val album = async { repository.getMusicAlbum(albumId) }
                        val songs = async { repository.getMusicSongs(albumId) }
                        album.await() to songs.await()
                    }

                loadedId = albumId
                _state.value =
                    AlbumDetailState(
                        album = album,
                        songs = songs,
                    )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e) }
            }
        }
    }

    fun retry(albumId: UUID) = load(albumId, force = true)
}
