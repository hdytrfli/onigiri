package dev.hdytrfli.onigiri.feature.film.presentation.music

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.hdytrfli.onigiri.LocalTabReselect
import dev.hdytrfli.onigiri.MusicRoute
import dev.hdytrfli.onigiri.core.R as CoreR
import dev.hdytrfli.onigiri.feature.film.R as FilmR
import dev.hdytrfli.onigiri.feature.film.presentation.components.EmptyStateOverlay
import dev.hdytrfli.onigiri.feature.film.presentation.components.FullPageError
import dev.hdytrfli.onigiri.feature.film.presentation.components.ItemCountBadge
import dev.hdytrfli.onigiri.feature.film.presentation.components.ListOptionsMenu
import dev.hdytrfli.onigiri.feature.film.presentation.components.MusicRowAction
import dev.hdytrfli.onigiri.feature.film.presentation.components.MusicTrackRow
import dev.hdytrfli.onigiri.feature.film.presentation.components.SectionHeader
import dev.hdytrfli.onigiri.feature.film.presentation.components.rememberMediaDeleteController
import dev.hdytrfli.onigiri.feature.film.presentation.components.section
import dev.hdytrfli.onigiri.ui.components.AppIconButton
import dev.hdytrfli.onigiri.ui.components.AppPullToRefreshBox
import dev.hdytrfli.onigiri.ui.components.LocalRootPageContentInset
import dev.hdytrfli.onigiri.ui.components.OfflineModeReload
import dev.hdytrfli.onigiri.ui.components.PageSearchField
import dev.hdytrfli.onigiri.ui.components.PageToolbar
import dev.hdytrfli.onigiri.ui.components.PullToRefreshButton
import dev.hdytrfli.onigiri.ui.components.TabContent
import dev.hdytrfli.onigiri.ui.components.TopBarPageShell
import dev.hdytrfli.onigiri.ui.theme.Red
import dev.hdytrfli.onigiri.ui.theme.spacings
import dev.hdytrfli.onigiri.ui.utils.GridSize
import dev.hdytrfli.onigiri.ui.utils.LocalGridGap
import dev.hdytrfli.onigiri.ui.utils.gridColumns
import dev.hdytrfli.onigiri.ui.utils.rememberSafePadding

@Composable
fun MusicScreen(
    onArtistClick: (artistId: String) -> Unit,
    onAlbumClick: (albumId: String) -> Unit,
    viewModel: MusicViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentSongId by viewModel.currentSongId.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableStateOf(MusicTab.SONGS) }
    var searchExpanded by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showDisplayDialog by rememberSaveable { mutableStateOf(false) }
    var showSortDialog by rememberSaveable { mutableStateOf(false) }
    val pullState = rememberPullToRefreshState()
    val deleteController = rememberMediaDeleteController()

    LaunchedEffect(Unit) { viewModel.load() }
    OfflineModeReload { viewModel.load(force = true) }

    BackHandler(enabled = searchExpanded) {
        searchExpanded = false
        searchQuery = ""
    }

    val tabReselect = LocalTabReselect.current
    LaunchedEffect(tabReselect) {
        tabReselect.reselects(MusicRoute).collect {
            selectedTab = MusicTab.entries[(selectedTab.ordinal + 1) % MusicTab.entries.size]
        }
    }

    val query = searchQuery.trim()
    val songs = state.songs.filter { query.isBlank() || it.name.contains(query, true) }
    val artists = state.artists.filter { query.isBlank() || it.name.contains(query, true) }
    val albums = state.albums.filter { query.isBlank() || it.name.contains(query, true) }

    TopBarPageShell(
        topBar = {
            AnimatedContent(targetState = searchExpanded, label = "music-search") { expanded ->
                if (expanded) {
                    PageSearchField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        onClose = {
                            searchExpanded = false
                            searchQuery = ""
                        },
                    )
                } else {
                    PageToolbar(
                        tabs =
                            listOf(
                                stringResource(FilmR.string.music_songs),
                                stringResource(FilmR.string.music_artists),
                                stringResource(FilmR.string.music_albums),
                            ),
                        tabIcons =
                            listOf(
                                CoreR.drawable.ic_music,
                                CoreR.drawable.ic_user,
                                CoreR.drawable.ic_package_2,
                            ),
                        selectedIndex = selectedTab.ordinal,
                        onSelect = { selectedTab = MusicTab.entries[it] },
                        dropdownOnCompact = true,
                        actions = {
                            PullToRefreshButton(
                                state = pullState,
                                isRefreshing = state.isLoading,
                                onRefresh = viewModel::retry,
                            )
                            AppIconButton(
                                onClick = { searchExpanded = true },
                                content = {
                                    Icon(
                                        painter = painterResource(CoreR.drawable.ic_search),
                                        contentDescription = stringResource(CoreR.string.search),
                                    )
                                },
                            )
                            ListOptionsMenu(
                                onDisplayInfoClick = { showDisplayDialog = true },
                                onSortClick = { showSortDialog = true },
                            )
                        },
                    )
                }
            }
        }
    ) {
        val safePadding = rememberSafePadding()
        val contentPadding =
            PaddingValues(
                start = safePadding.start + MaterialTheme.spacings.gutter,
                top = LocalRootPageContentInset.current,
                end = safePadding.end + MaterialTheme.spacings.gutter,
                bottom = safePadding.bottom + MaterialTheme.spacings.gutter,
            )

        TabContent(selectedTab = selectedTab.ordinal) { tabIndex ->
            AppPullToRefreshBox(
                isRefreshing = state.isLoading,
                onRefresh = viewModel::retry,
                state = pullState,
                showIndicator = false,
                modifier = Modifier.fillMaxSize(),
            ) {
                when (MusicTab.entries[tabIndex]) {
                    MusicTab.SONGS ->
                        SongsGrid(
                            songs = songs,
                            currentSongId = currentSongId,
                            displayFields = state.songDisplayFields,
                            loading = state.isLoading,
                            error = state.error,
                            contentPadding = contentPadding,
                            onRefresh = viewModel::retry,
                            onSongClick = viewModel::playSong,
                            onPlayNext = viewModel::playNext,
                            onAddToQueue = viewModel::addToQueue,
                            onToggleFavorite = viewModel::toggleFavorite,
                            onDelete =
                                if (deleteController.canDelete) {
                                    { song ->
                                        deleteController.request(song.id, song.name) {
                                            viewModel.load(force = true)
                                        }
                                    }
                                } else {
                                    null
                                },
                            onArtistClick = onArtistClick,
                            onAlbumClick = onAlbumClick,
                        )
                    MusicTab.ARTISTS ->
                        ArtistsGrid(
                            artists = artists,
                            displayFields = state.artistDisplayFields,
                            loading = state.isLoading,
                            error = state.error,
                            contentPadding = contentPadding,
                            onRefresh = viewModel::retry,
                            onArtistClick = onArtistClick,
                        )
                    MusicTab.ALBUMS ->
                        AlbumsGrid(
                            albums = albums,
                            displayFields = state.albumDisplayFields,
                            loading = state.isLoading,
                            error = state.error,
                            contentPadding = contentPadding,
                            onRefresh = viewModel::retry,
                            onAlbumClick = onAlbumClick,
                        )
                }
            }
        }
    }

    val songSortOptions =
        listOf(
            MusicSortField.Title to stringResource(CoreR.string.sort_by_options_0),
            MusicSortField.Duration to stringResource(FilmR.string.music_duration_label),
            MusicSortField.Artist to stringResource(FilmR.string.music_artist_label),
            MusicSortField.Album to stringResource(FilmR.string.music_album_label),
            MusicSortField.Year to stringResource(CoreR.string.display_year),
        )
    val artistSortOptions =
        listOf(
            MusicSortField.Title to stringResource(CoreR.string.sort_by_options_0),
            MusicSortField.AlbumCount to stringResource(FilmR.string.music_album_count_label),
        )
    val albumSortOptions =
        listOf(
            MusicSortField.Title to stringResource(CoreR.string.sort_by_options_0),
            MusicSortField.Artist to stringResource(FilmR.string.music_artist_label),
            MusicSortField.Year to stringResource(CoreR.string.display_year),
        )
    val songDisplayOptions =
        listOf(
            MusicDisplayField.Duration to stringResource(FilmR.string.music_duration_label),
            MusicDisplayField.Artist to stringResource(FilmR.string.music_artist_label),
            MusicDisplayField.Album to stringResource(FilmR.string.music_album_label),
            MusicDisplayField.Year to stringResource(CoreR.string.display_year),
        )
    val artistDisplayOptions =
        listOf(MusicDisplayField.AlbumCount to stringResource(FilmR.string.music_album_count_label))
    val albumDisplayOptions =
        listOf(
            MusicDisplayField.Artist to stringResource(FilmR.string.music_artist_label),
            MusicDisplayField.Year to stringResource(CoreR.string.display_year),
        )

    if (showSortDialog) {
        val options =
            when (selectedTab) {
                MusicTab.SONGS -> songSortOptions
                MusicTab.ARTISTS -> artistSortOptions
                MusicTab.ALBUMS -> albumSortOptions
            }
        val currentSortBy =
            when (selectedTab) {
                MusicTab.SONGS -> state.songSortBy
                MusicTab.ARTISTS -> state.artistSortBy
                MusicTab.ALBUMS -> state.albumSortBy
            }
        val currentSortOrder =
            when (selectedTab) {
                MusicTab.SONGS -> state.songSortOrder
                MusicTab.ARTISTS -> state.artistSortOrder
                MusicTab.ALBUMS -> state.albumSortOrder
            }
        MusicSortDialog(
            options = options,
            currentSortBy = currentSortBy,
            currentSortOrder = currentSortOrder,
            onUpdate = { field, order -> viewModel.changeSorting(selectedTab, field, order) },
            onDismissRequest = { showSortDialog = false },
        )
    }

    if (showDisplayDialog) {
        when (selectedTab) {
            MusicTab.SONGS ->
                MusicDisplayDialog(
                    options = songDisplayOptions,
                    selected = state.songDisplayFields,
                    onToggle = { viewModel.toggleDisplayField(MusicTab.SONGS, it) },
                    onDismiss = { showDisplayDialog = false },
                )
            MusicTab.ARTISTS ->
                MusicDisplayDialog(
                    options = artistDisplayOptions,
                    selected = state.artistDisplayFields,
                    onToggle = { viewModel.toggleDisplayField(MusicTab.ARTISTS, it) },
                    onDismiss = { showDisplayDialog = false },
                )
            MusicTab.ALBUMS ->
                MusicDisplayDialog(
                    options = albumDisplayOptions,
                    selected = state.albumDisplayFields,
                    onToggle = { viewModel.toggleDisplayField(MusicTab.ALBUMS, it) },
                    onDismiss = { showDisplayDialog = false },
                )
        }
    }
}

@Composable
private fun SongsGrid(
    songs: List<dev.hdytrfli.onigiri.models.FindroidMusicSong>,
    currentSongId: java.util.UUID?,
    displayFields: Set<MusicDisplayField>,
    loading: Boolean,
    error: Exception?,
    contentPadding: PaddingValues,
    onRefresh: () -> Unit,
    onSongClick: (java.util.UUID) -> Unit,
    onPlayNext: (java.util.UUID) -> Unit,
    onAddToQueue: (java.util.UUID) -> Unit,
    onToggleFavorite: (java.util.UUID) -> Unit,
    onDelete: ((dev.hdytrfli.onigiri.models.FindroidMusicSong) -> Unit)?,
    onArtistClick: (artistId: String) -> Unit,
    onAlbumClick: (albumId: String) -> Unit,
) {
    val sectionTitle = stringResource(FilmR.string.music_all_songs)
    if (error != null && songs.isEmpty()) {
        FullPageError(onRefresh = onRefresh, isRefreshing = loading, error = error)
        return
    }

    EmptyStateOverlay(
        visible = songs.isEmpty(),
        loading = loading,
        context = stringResource(FilmR.string.music_context_songs),
        title = sectionTitle,
        onRefresh = onRefresh,
        contentPadding = contentPadding,
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
        ) {
            item {
                SectionHeader(
                    title = sectionTitle,
                    trailingContent = {
                        ItemCountBadge(
                            count = songs.size,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    },
                )
            }
            itemsIndexed(items = songs, key = { _, song -> song.id }) { _, song ->
                SongListRow(
                    song = song,
                    isCurrent = song.id == currentSongId,
                    displayFields = displayFields,
                    onPlay = { onSongClick(song.id) },
                    onPlayNext = { onPlayNext(song.id) },
                    onAddToQueue = { onAddToQueue(song.id) },
                    onToggleFavorite = { onToggleFavorite(song.id) },
                    onDelete = onDelete?.let { { it(song) } },
                    onViewArtist = song.artistId?.let { { onArtistClick(it.toString()) } },
                    onViewAlbum = song.albumId?.let { { onAlbumClick(it.toString()) } },
                )
            }
        }
    }
}

@Composable
private fun SongListRow(
    song: dev.hdytrfli.onigiri.models.FindroidMusicSong,
    isCurrent: Boolean,
    displayFields: Set<MusicDisplayField>,
    onPlay: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: (() -> Unit)?,
    onViewArtist: (() -> Unit)?,
    onViewAlbum: (() -> Unit)?,
) {
    MusicTrackRow(
        title = song.name,
        subtitle =
            listOfNotNull(
                    song.artistName.takeIf { MusicDisplayField.Artist in displayFields },
                    song.albumName.takeIf { MusicDisplayField.Album in displayFields },
                    song.productionYear
                        .takeIf { MusicDisplayField.Year in displayFields }
                        ?.toString(),
                )
                .joinToString(" · "),
        imageUri = song.images.primary,
        isCurrent = isCurrent,
        onClick = onPlay,
        trailingText =
            formatDuration(song.runtimeTicks).takeIf {
                MusicDisplayField.Duration in displayFields
            },
        actions =
            listOf(
                MusicRowAction(
                    label =
                        stringResource(
                            if (song.favorite) CoreR.string.remove_from_favorites
                            else CoreR.string.add_to_favorites
                        ),
                    icon =
                        if (song.favorite) CoreR.drawable.ic_heart_filled
                        else CoreR.drawable.ic_heart,
                    iconTint = if (song.favorite) Red else null,
                    dismissOnClick = false,
                    onClick = onToggleFavorite,
                ),
                MusicRowAction(
                    label = stringResource(FilmR.string.music_play_next),
                    icon = CoreR.drawable.ic_skip_forward,
                    onClick = onPlayNext,
                ),
                MusicRowAction(
                    label = stringResource(FilmR.string.music_add_to_queue),
                    icon = CoreR.drawable.ic_plus,
                    onClick = onAddToQueue,
                ),
                MusicRowAction(
                    label = stringResource(FilmR.string.music_discover_album),
                    icon = CoreR.drawable.ic_lucide_gallery_vertical_end,
                    enabled = onViewAlbum != null,
                    onClick = { onViewAlbum?.invoke() },
                ),
            ) +
                if (onDelete != null) {
                    listOf(
                        MusicRowAction(
                            label = stringResource(FilmR.string.delete_media_action),
                            icon = CoreR.drawable.ic_lucide_trash,
                            onClick = onDelete,
                        )
                    )
                } else {
                    emptyList()
                },
        trailingDivider = true,
    )
}

private fun formatDuration(ticks: Long): String {
    val seconds = (ticks / 10_000_000L).coerceAtLeast(0L)
    return "%d:%02d".format(seconds / 60, seconds % 60)
}

@Composable
private fun ArtistsGrid(
    artists: List<dev.hdytrfli.onigiri.models.FindroidMusicArtist>,
    displayFields: Set<MusicDisplayField>,
    loading: Boolean,
    error: Exception?,
    contentPadding: PaddingValues,
    onRefresh: () -> Unit,
    onArtistClick: (artistId: String) -> Unit,
) {
    val sectionTitle = stringResource(FilmR.string.music_all_artists)
    if (error != null && artists.isEmpty()) {
        FullPageError(onRefresh = onRefresh, isRefreshing = loading, error = error)
        return
    }

    EmptyStateOverlay(
        visible = artists.isEmpty(),
        loading = loading,
        context = stringResource(FilmR.string.music_context_artists),
        title = sectionTitle,
        onRefresh = onRefresh,
        contentPadding = contentPadding,
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(gridColumns(GridSize.POSTER)),
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
            horizontalArrangement = Arrangement.spacedBy(LocalGridGap.current),
            verticalArrangement = Arrangement.spacedBy(LocalGridGap.current),
        ) {
            if (artists.isNotEmpty()) {
                section(
                    title = sectionTitle,
                    trailingContent = {
                        ItemCountBadge(
                            count = artists.size,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    },
                ) {
                    items(items = artists, key = { it.id }) { artist ->
                        MusicCard(
                            title = artist.name,
                            subtitle =
                                artist.albumCount
                                    ?.takeIf { MusicDisplayField.AlbumCount in displayFields }
                                    ?.let { stringResource(FilmR.string.music_album_count, it) },
                            image = artist.images.primary ?: artist.images.logo,
                            circular = true,
                            placeholderIcon = CoreR.drawable.ic_user,
                            onClick = { onArtistClick(artist.id.toString()) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AlbumsGrid(
    albums: List<dev.hdytrfli.onigiri.models.FindroidMusicAlbum>,
    displayFields: Set<MusicDisplayField>,
    loading: Boolean,
    error: Exception?,
    contentPadding: PaddingValues,
    onRefresh: () -> Unit,
    onAlbumClick: (albumId: String) -> Unit,
) {
    val sectionTitle = stringResource(FilmR.string.music_all_albums)
    if (error != null && albums.isEmpty()) {
        FullPageError(onRefresh = onRefresh, isRefreshing = loading, error = error)
        return
    }

    EmptyStateOverlay(
        visible = albums.isEmpty(),
        loading = loading,
        context = stringResource(FilmR.string.music_context_albums),
        title = sectionTitle,
        onRefresh = onRefresh,
        contentPadding = contentPadding,
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(gridColumns(GridSize.POSTER)),
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
            horizontalArrangement = Arrangement.spacedBy(LocalGridGap.current),
            verticalArrangement = Arrangement.spacedBy(LocalGridGap.current),
        ) {
            if (albums.isNotEmpty()) {
                section(
                    title = sectionTitle,
                    trailingContent = {
                        ItemCountBadge(
                            count = albums.size,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    },
                ) {
                    items(items = albums, key = { it.id }) { album ->
                        MusicCard(
                            title = album.name,
                            subtitle =
                                buildSubtitle(
                                    album.artistName?.takeIf {
                                        MusicDisplayField.Artist in displayFields
                                    }
                                        ?: stringResource(FilmR.string.music_unknown_artist)
                                            .takeIf { MusicDisplayField.Artist in displayFields },
                                    album.productionYear.takeIf {
                                        MusicDisplayField.Year in displayFields
                                    },
                                ),
                            image = album.images.primary,
                            circular = false,
                            placeholderIcon = CoreR.drawable.ic_music,
                            onClick = { onAlbumClick(album.id.toString()) },
                        )
                    }
                }
            }
        }
    }
}

private fun buildSubtitle(text: String?, year: Int?): String? =
    listOfNotNull(text, year?.toString()).takeIf { it.isNotEmpty() }?.joinToString(" · ")

private fun MusicState.hasContent(): Boolean =
    songs.isNotEmpty() || artists.isNotEmpty() || albums.isNotEmpty()
