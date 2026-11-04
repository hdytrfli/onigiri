package dev.hdytrfli.onigiri.feature.film.presentation.music

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.hdytrfli.onigiri.feature.film.presentation.library.ListDisplayField
import dev.hdytrfli.onigiri.models.FindroidMusicAlbum
import dev.hdytrfli.onigiri.models.FindroidMusicArtist
import dev.hdytrfli.onigiri.models.FindroidMusicSong
import dev.hdytrfli.onigiri.models.SortBy
import dev.hdytrfli.onigiri.models.SortOrder
import dev.hdytrfli.onigiri.player.local.music.MusicPlayerManager
import dev.hdytrfli.onigiri.repository.JellyfinRepository
import dev.hdytrfli.onigiri.settings.domain.AppPreferences
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

val MUSIC_SORT_OPTIONS = listOf(SortBy.NAME, SortBy.RELEASE_DATE)

data class MusicState(
    val songs: List<FindroidMusicSong> = emptyList(),
    val artists: List<FindroidMusicArtist> = emptyList(),
    val albums: List<FindroidMusicAlbum> = emptyList(),
    val displayFields: Set<ListDisplayField> = emptySet(),
    val sortBy: SortBy = SortBy.NAME,
    val sortOrder: SortOrder = SortOrder.ASCENDING,
    val isLoading: Boolean = false,
    val error: Exception? = null,
)

@HiltViewModel
class MusicViewModel
@Inject
constructor(
    private val repository: JellyfinRepository,
    private val appPreferences: AppPreferences,
    private val playerManager: MusicPlayerManager,
) : ViewModel() {
    private val _state = MutableStateFlow(MusicState())
    val state = _state.asStateFlow()

    private var loadJob: Job? = null
    private var loaded = false

    fun load(force: Boolean = false) {
        if (!force && loaded) return
        loadJob?.cancel()
        val storedSortBy = SortBy.fromString(appPreferences.getValue(appPreferences.musicSortBy))
        val sortBy = storedSortBy.takeIf { it in MUSIC_SORT_OPTIONS } ?: SortBy.NAME
        val sortOrder = SortOrder.fromString(appPreferences.getValue(appPreferences.musicSortOrder))
        val displayFields =
            ListDisplayField.fromKeys(appPreferences.getValue(appPreferences.listDisplayFields))
        if (sortBy != storedSortBy) {
            appPreferences.setValue(appPreferences.musicSortBy, sortBy.toString())
        }

        _state.update {
            it.copy(
                displayFields = displayFields,
                sortBy = sortBy,
                sortOrder = sortOrder,
                isLoading = true,
                error = null,
            )
        }

        loadJob = viewModelScope.launch {
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
                        songs = sortSongs(songs, sortBy, sortOrder),
                        artists = sortArtists(artists, sortOrder),
                        albums = sortAlbums(albums, sortBy, sortOrder),
                        displayFields = displayFields,
                        sortBy = sortBy,
                        sortOrder = sortOrder,
                    )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e) }
            }
        }
    }

    fun changeSorting(sortBy: SortBy, sortOrder: SortOrder) {
        if (sortBy !in MUSIC_SORT_OPTIONS) return
        appPreferences.setValue(appPreferences.musicSortBy, sortBy.toString())
        appPreferences.setValue(appPreferences.musicSortOrder, sortOrder.toString())
        _state.update {
            it.copy(
                songs = sortSongs(it.songs, sortBy, sortOrder),
                artists = sortArtists(it.artists, sortOrder),
                albums = sortAlbums(it.albums, sortBy, sortOrder),
                sortBy = sortBy,
                sortOrder = sortOrder,
            )
        }
    }

    fun toggleDisplayField(field: ListDisplayField) {
        val fields = _state.value.displayFields.toMutableSet()
        if (!fields.add(field)) {
            fields.remove(field)
        }
        appPreferences.setValue(
            appPreferences.listDisplayFields,
            fields.map { it.key }.toSet(),
        )
        _state.update { it.copy(displayFields = fields) }
    }

    fun playSong(songId: java.util.UUID) {
        val songs = _state.value.songs
        val index = songs.indexOfFirst { it.id == songId }
        if (index >= 0) {
            playerManager.playSongs(songs = songs, startIndex = index)
        }
    }

    fun playNext(songId: java.util.UUID) {
        _state.value.songs
            .firstOrNull { it.id == songId }
            ?.let { song -> playerManager.playNext(listOf(song)) }
    }

    fun addToQueue(songId: java.util.UUID) {
        _state.value.songs
            .firstOrNull { it.id == songId }
            ?.let { song -> playerManager.addToQueue(listOf(song)) }
    }

    fun toggleFavorite(songId: java.util.UUID) {
        val song = _state.value.songs.firstOrNull { it.id == songId } ?: return
        viewModelScope.launch {
            val favorite = !song.favorite
            runCatching {
                if (favorite) {
                    repository.markAsFavorite(songId)
                } else {
                    repository.unmarkAsFavorite(songId)
                }
            }
                .onSuccess {
                    _state.update { state ->
                        state.copy(
                            songs =
                                state.songs.map {
                                    if (it.id == songId) it.copy(favorite = favorite) else it
                                }
                        )
                    }
                }
        }
    }

    fun retry() = load(force = true)

    private fun sortSongs(
        songs: List<FindroidMusicSong>,
        sortBy: SortBy,
        sortOrder: SortOrder,
    ): List<FindroidMusicSong> =
        when (sortBy) {
            SortBy.RELEASE_DATE ->
                songs.sortedWith(
                    compareBy<FindroidMusicSong> { it.productionYear ?: Int.MAX_VALUE }
                        .thenBy { it.name.lowercase() }
                )
            else -> songs.sortedBy { it.name.lowercase() }
        }.let { if (sortOrder == SortOrder.DESCENDING) it.reversed() else it }

    private fun sortAlbums(
        albums: List<FindroidMusicAlbum>,
        sortBy: SortBy,
        sortOrder: SortOrder,
    ): List<FindroidMusicAlbum> =
        when (sortBy) {
            SortBy.RELEASE_DATE ->
                albums.sortedWith(
                    compareBy<FindroidMusicAlbum> { it.productionYear ?: Int.MAX_VALUE }
                        .thenBy { it.name.lowercase() }
                )
            else -> albums.sortedBy { it.name.lowercase() }
        }.let { if (sortOrder == SortOrder.DESCENDING) it.reversed() else it }

    private fun sortArtists(
        artists: List<FindroidMusicArtist>,
        sortOrder: SortOrder,
    ): List<FindroidMusicArtist> =
        artists
            .sortedBy { it.name.lowercase() }
            .let { if (sortOrder == SortOrder.DESCENDING) it.reversed() else it }
}
