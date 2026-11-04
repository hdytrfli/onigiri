package dev.hdytrfli.onigiri.feature.film.presentation.music

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.hdytrfli.onigiri.models.FindroidMusicAlbum
import dev.hdytrfli.onigiri.models.FindroidMusicSong
import dev.hdytrfli.onigiri.player.local.music.MusicPlayerManager
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
class AlbumDetailViewModel
@Inject
constructor(
    private val repository: JellyfinRepository,
    private val playerManager: MusicPlayerManager,
) : ViewModel() {
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
                        songs = songs.sortedBy { it.indexNumber ?: Int.MAX_VALUE },
                    )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e) }
            }
        }
    }

    fun play(songId: UUID? = null, shuffle: Boolean = false) {
        val songs = _state.value.songs
        if (songs.isEmpty()) return
        val index = songId?.let { id -> songs.indexOfFirst { it.id == id } } ?: 0
        playerManager.playSongs(
            songs = songs,
            startIndex = index.coerceAtLeast(0),
            shuffle = shuffle,
        )
    }

    fun playNext() {
        val songs = _state.value.songs
        if (songs.isNotEmpty()) {
            playerManager.playNext(songs)
        }
    }

    fun addToQueue() {
        val songs = _state.value.songs
        if (songs.isNotEmpty()) {
            playerManager.addToQueue(songs)
        }
    }

    fun toggleFavorite(albumId: UUID) {
        val album = _state.value.album ?: return
        viewModelScope.launch {
            val favorite = !album.favorite
            runCatching {
                if (favorite) {
                    repository.markAsFavorite(albumId)
                } else {
                    repository.unmarkAsFavorite(albumId)
                }
            }
                .onSuccess {
                    _state.update { it.copy(album = it.album?.copy(favorite = favorite)) }
                }
        }
    }

    fun retry(albumId: UUID) = load(albumId, force = true)
}
