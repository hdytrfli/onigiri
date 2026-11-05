package dev.hdytrfli.onigiri.feature.film.presentation.music

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.hdytrfli.onigiri.models.FindroidMusicAlbum
import dev.hdytrfli.onigiri.models.FindroidMusicArtist
import dev.hdytrfli.onigiri.models.FindroidMusicSong
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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MusicState(
    val songs: List<FindroidMusicSong> = emptyList(),
    val artists: List<FindroidMusicArtist> = emptyList(),
    val albums: List<FindroidMusicAlbum> = emptyList(),
    val songDisplayFields: Set<MusicDisplayField> = emptySet(),
    val artistDisplayFields: Set<MusicDisplayField> = emptySet(),
    val albumDisplayFields: Set<MusicDisplayField> = emptySet(),
    val songSortBy: MusicSortField = MusicSortField.Title,
    val artistSortBy: MusicSortField = MusicSortField.Title,
    val albumSortBy: MusicSortField = MusicSortField.Title,
    val songSortOrder: SortOrder = SortOrder.ASCENDING,
    val artistSortOrder: SortOrder = SortOrder.ASCENDING,
    val albumSortOrder: SortOrder = SortOrder.ASCENDING,
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
    private val swrStore: dev.hdytrfli.onigiri.swr.SwrStore,
) : ViewModel() {
    private val _state = MutableStateFlow(MusicState())
    val state = _state.asStateFlow()

    /** Changes only when the playing track changes, not on every progress tick. */
    val currentSongId =
        playerManager.state
            .map { it.current?.id }
            .distinctUntilChanged()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = playerManager.state.value.current?.id,
            )

    private var loadJob: Job? = null
    private var loaded = false

    fun load(force: Boolean = false) {
        if (!force && loaded) return
        loadJob?.cancel()
        val songSortBy = sortField(appPreferences.getValue(appPreferences.musicSongSortBy))
        val artistSortBy = sortField(appPreferences.getValue(appPreferences.musicArtistSortBy))
        val albumSortBy = sortField(appPreferences.getValue(appPreferences.musicAlbumSortBy))
        val songSortOrder =
            SortOrder.fromString(appPreferences.getValue(appPreferences.musicSongSortOrder))
        val artistSortOrder =
            SortOrder.fromString(appPreferences.getValue(appPreferences.musicArtistSortOrder))
        val albumSortOrder =
            SortOrder.fromString(appPreferences.getValue(appPreferences.musicAlbumSortOrder))
        val songDisplayFields =
            MusicDisplayField.fromKeys(
                appPreferences.getValue(appPreferences.musicSongDisplayFields)
            )
        val artistDisplayFields =
            MusicDisplayField.fromKeys(
                appPreferences.getValue(appPreferences.musicArtistDisplayFields)
            )
        val albumDisplayFields =
            MusicDisplayField.fromKeys(
                appPreferences.getValue(appPreferences.musicAlbumDisplayFields)
            )

        _state.update {
            it.copy(
                songDisplayFields = songDisplayFields,
                artistDisplayFields = artistDisplayFields,
                albumDisplayFields = albumDisplayFields,
                songSortBy = songSortBy,
                artistSortBy = artistSortBy,
                albumSortBy = albumSortBy,
                songSortOrder = songSortOrder,
                artistSortOrder = artistSortOrder,
                albumSortOrder = albumSortOrder,
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
                        songs = sortSongs(songs, songSortBy, songSortOrder),
                        artists = sortArtists(artists, artistSortBy, artistSortOrder),
                        albums = sortAlbums(albums, albumSortBy, albumSortOrder),
                        songDisplayFields = songDisplayFields,
                        artistDisplayFields = artistDisplayFields,
                        albumDisplayFields = albumDisplayFields,
                        songSortBy = songSortBy,
                        artistSortBy = artistSortBy,
                        albumSortBy = albumSortBy,
                        songSortOrder = songSortOrder,
                        artistSortOrder = artistSortOrder,
                        albumSortOrder = albumSortOrder,
                    )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e) }
            }
        }
    }

    fun changeSorting(tab: MusicTab, field: MusicSortField, order: SortOrder) {
        when (tab) {
            MusicTab.SONGS -> {
                if (field !in SongSortFields) return
                appPreferences.setValue(appPreferences.musicSongSortBy, field.toString())
                appPreferences.setValue(appPreferences.musicSongSortOrder, order.toString())
                _state.update {
                    it.copy(
                        songs = sortSongs(it.songs, field, order),
                        songSortBy = field,
                        songSortOrder = order,
                    )
                }
            }
            MusicTab.ARTISTS -> {
                if (field !in ArtistSortFields) return
                appPreferences.setValue(appPreferences.musicArtistSortBy, field.toString())
                appPreferences.setValue(appPreferences.musicArtistSortOrder, order.toString())
                _state.update {
                    it.copy(
                        artists = sortArtists(it.artists, field, order),
                        artistSortBy = field,
                        artistSortOrder = order,
                    )
                }
            }
            MusicTab.ALBUMS -> {
                if (field !in AlbumSortFields) return
                appPreferences.setValue(appPreferences.musicAlbumSortBy, field.toString())
                appPreferences.setValue(appPreferences.musicAlbumSortOrder, order.toString())
                _state.update {
                    it.copy(
                        albums = sortAlbums(it.albums, field, order),
                        albumSortBy = field,
                        albumSortOrder = order,
                    )
                }
            }
        }
    }

    fun toggleDisplayField(tab: MusicTab, field: MusicDisplayField) {
        when (tab) {
            MusicTab.SONGS -> {
                if (field !in SongDisplayFields) return
                val fields = _state.value.songDisplayFields.toggled(field)
                appPreferences.setValue(
                    appPreferences.musicSongDisplayFields,
                    fields.map { it.key }.toSet(),
                )
                _state.update { it.copy(songDisplayFields = fields) }
            }
            MusicTab.ARTISTS -> {
                if (field !in ArtistDisplayFields) return
                val fields = _state.value.artistDisplayFields.toggled(field)
                appPreferences.setValue(
                    appPreferences.musicArtistDisplayFields,
                    fields.map { it.key }.toSet(),
                )
                _state.update { it.copy(artistDisplayFields = fields) }
            }
            MusicTab.ALBUMS -> {
                if (field !in AlbumDisplayFields) return
                val fields = _state.value.albumDisplayFields.toggled(field)
                appPreferences.setValue(
                    appPreferences.musicAlbumDisplayFields,
                    fields.map { it.key }.toSet(),
                )
                _state.update { it.copy(albumDisplayFields = fields) }
            }
        }
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
                    swrStore.invalidateMediaLists()
                }
        }
    }

    fun retry() = load(force = true)

    private fun sortField(value: String): MusicSortField = runCatching {
        MusicSortField.valueOf(value)
    }
        .getOrDefault(MusicSortField.Title)

    private fun Set<MusicDisplayField>.toggled(field: MusicDisplayField): Set<MusicDisplayField> =
        toMutableSet().apply { if (!add(field)) remove(field) }

    private fun sortSongs(
        songs: List<FindroidMusicSong>,
        field: MusicSortField,
        order: SortOrder,
    ): List<FindroidMusicSong> =
        songs
            .sortedWith(
                when (field) {
                    MusicSortField.Duration -> compareBy<FindroidMusicSong> { it.runtimeTicks }
                    MusicSortField.Artist ->
                        compareBy<FindroidMusicSong> { it.artistName.orEmpty().lowercase() }
                    MusicSortField.Album ->
                        compareBy<FindroidMusicSong> { it.albumName.orEmpty().lowercase() }
                    MusicSortField.Year ->
                        compareBy<FindroidMusicSong> { it.productionYear ?: Int.MAX_VALUE }
                    else -> compareBy<FindroidMusicSong> { it.name.lowercase() }
                }.thenBy { it.name.lowercase() }
            )
            .ordered(order)

    private fun sortAlbums(
        albums: List<FindroidMusicAlbum>,
        field: MusicSortField,
        order: SortOrder,
    ): List<FindroidMusicAlbum> =
        albums
            .sortedWith(
                when (field) {
                    MusicSortField.Artist ->
                        compareBy<FindroidMusicAlbum> { it.artistName.orEmpty().lowercase() }
                    MusicSortField.Year ->
                        compareBy<FindroidMusicAlbum> { it.productionYear ?: Int.MAX_VALUE }
                    else -> compareBy<FindroidMusicAlbum> { it.name.lowercase() }
                }.thenBy { it.name.lowercase() }
            )
            .ordered(order)

    private fun sortArtists(
        artists: List<FindroidMusicArtist>,
        field: MusicSortField,
        order: SortOrder,
    ): List<FindroidMusicArtist> =
        artists
            .sortedWith(
                when (field) {
                    MusicSortField.AlbumCount ->
                        compareBy<FindroidMusicArtist> { it.albumCount ?: 0 }
                    else -> compareBy<FindroidMusicArtist> { it.name.lowercase() }
                }.thenBy { it.name.lowercase() }
            )
            .ordered(order)

    private fun <T> List<T>.ordered(order: SortOrder): List<T> =
        if (order == SortOrder.DESCENDING) reversed() else this
}
