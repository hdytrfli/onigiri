package dev.hdytrfli.onigiri.feature.film.presentation.music

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.hdytrfli.onigiri.models.FindroidMusicAlbum
import dev.hdytrfli.onigiri.models.FindroidMusicArtist
import dev.hdytrfli.onigiri.player.local.music.MusicPlayerManager
import dev.hdytrfli.onigiri.repository.JellyfinRepository
import dev.hdytrfli.onigiri.settings.domain.AppPreferences
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
    val similarArtists: List<FindroidMusicArtist> = emptyList(),
    val showRecommendations: Boolean = true,
    val isLoading: Boolean = false,
    val error: Exception? = null,
)

@HiltViewModel
class ArtistDetailViewModel
@Inject
constructor(
    private val repository: JellyfinRepository,
    private val playerManager: MusicPlayerManager,
    private val appPreferences: AppPreferences,
    private val swrStore: dev.hdytrfli.onigiri.swr.SwrStore,
) : ViewModel() {
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
                val showRecommendations =
                    appPreferences.getValue(appPreferences.showRecommendations)
                val similarArtists =
                    if (showRecommendations) {
                        runCatching { repository.getSimilarArtists(artistId) }
                            .getOrDefault(emptyList())
                    } else {
                        emptyList()
                    }
                _state.value =
                    ArtistDetailState(
                        artist = artist,
                        albums = albums,
                        similarArtists = similarArtists,
                        showRecommendations = showRecommendations,
                    )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e) }
            }
        }
    }

    fun play(artistId: UUID, shuffle: Boolean = false) {
        viewModelScope.launch {
            val songs = repository.getMusicSongsByArtist(artistId)
            if (songs.isNotEmpty()) {
                playerManager.playSongs(songs = songs, shuffle = shuffle)
            }
        }
    }

    fun playNext(artistId: UUID) {
        viewModelScope.launch {
            val songs = repository.getMusicSongsByArtist(artistId)
            if (songs.isNotEmpty()) {
                playerManager.playNext(songs)
            }
        }
    }

    fun addToQueue(artistId: UUID) {
        viewModelScope.launch {
            val songs = repository.getMusicSongsByArtist(artistId)
            if (songs.isNotEmpty()) {
                playerManager.addToQueue(songs)
            }
        }
    }

    fun toggleFavorite(artistId: UUID) {
        val artist = _state.value.artist ?: return
        viewModelScope.launch {
            val favorite = !artist.favorite
            runCatching {
                if (favorite) {
                    repository.markAsFavorite(artistId)
                } else {
                    repository.unmarkAsFavorite(artistId)
                }
            }
                .onSuccess {
                    _state.update { it.copy(artist = it.artist?.copy(favorite = favorite)) }
                    swrStore.invalidateMediaLists()
                }
        }
    }

    fun retry(artistId: UUID) = load(artistId, force = true)
}
