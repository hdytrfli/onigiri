package dev.hdytrfli.onigiri.feature.film.presentation.music

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import dev.hdytrfli.onigiri.LocalTabReselect
import dev.hdytrfli.onigiri.MusicRoute
import dev.hdytrfli.onigiri.core.R as CoreR
import dev.hdytrfli.onigiri.feature.film.R as FilmR
import dev.hdytrfli.onigiri.feature.film.presentation.DisplayInfoDialog
import dev.hdytrfli.onigiri.feature.film.presentation.components.EmptyStateOverlay
import dev.hdytrfli.onigiri.feature.film.presentation.components.FullPageError
import dev.hdytrfli.onigiri.feature.film.presentation.components.ItemCountBadge
import dev.hdytrfli.onigiri.feature.film.presentation.components.ListOptionsMenu
import dev.hdytrfli.onigiri.feature.film.presentation.components.SectionHeader
import dev.hdytrfli.onigiri.feature.film.presentation.components.SortByDialog
import dev.hdytrfli.onigiri.feature.film.presentation.components.section
import dev.hdytrfli.onigiri.feature.film.presentation.library.ListDisplayField
import dev.hdytrfli.onigiri.ui.components.AppButtonSize
import dev.hdytrfli.onigiri.ui.components.AppButtonVariant
import dev.hdytrfli.onigiri.ui.components.AppDropdownMenu
import dev.hdytrfli.onigiri.ui.components.AppIconButton
import dev.hdytrfli.onigiri.ui.components.AppLoadingIndicator
import dev.hdytrfli.onigiri.ui.components.AppPullToRefreshBox
import dev.hdytrfli.onigiri.ui.components.LocalRootPageContentInset
import dev.hdytrfli.onigiri.ui.components.MenuActionItem
import dev.hdytrfli.onigiri.ui.components.OfflineModeReload
import dev.hdytrfli.onigiri.ui.components.PageSearchField
import dev.hdytrfli.onigiri.ui.components.PageToolbar
import dev.hdytrfli.onigiri.ui.components.PullToRefreshButton
import dev.hdytrfli.onigiri.ui.components.RootPage
import dev.hdytrfli.onigiri.ui.components.TabContent
import dev.hdytrfli.onigiri.ui.theme.spacings
import dev.hdytrfli.onigiri.ui.utils.GridSize
import dev.hdytrfli.onigiri.ui.utils.LocalGridGap
import dev.hdytrfli.onigiri.ui.utils.gridColumns
import dev.hdytrfli.onigiri.ui.utils.rememberSafePadding

private enum class MusicTab {
    SONGS,
    ARTISTS,
    ALBUMS,
}

@Composable
fun MusicScreen(
    onArtistClick: (artistId: String) -> Unit,
    onAlbumClick: (albumId: String) -> Unit,
    viewModel: MusicViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableStateOf(MusicTab.SONGS) }
    var searchExpanded by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showDisplayDialog by rememberSaveable { mutableStateOf(false) }
    var showSortDialog by rememberSaveable { mutableStateOf(false) }
    val pullState = rememberPullToRefreshState()

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

    RootPage(
        header = {
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
                Box(modifier = Modifier.fillMaxSize()) {
                    when (MusicTab.entries[tabIndex]) {
                        MusicTab.SONGS ->
                            SongsGrid(
                                songs = songs,
                                displayFields = state.displayFields,
                                loading = state.isLoading,
                                error = state.error,
                                contentPadding = contentPadding,
                                onRefresh = viewModel::retry,
                                onSongClick = viewModel::playSong,
                                onPlayNext = viewModel::playNext,
                                onAddToQueue = viewModel::addToQueue,
                                onToggleFavorite = viewModel::toggleFavorite,
                                onArtistClick = onArtistClick,
                                onAlbumClick = onAlbumClick,
                            )
                        MusicTab.ARTISTS ->
                            ArtistsGrid(
                                artists = artists,
                                loading = state.isLoading,
                                error = state.error,
                                contentPadding = contentPadding,
                                onRefresh = viewModel::retry,
                                onArtistClick = onArtistClick,
                            )
                        MusicTab.ALBUMS ->
                            AlbumsGrid(
                                albums = albums,
                                displayFields = state.displayFields,
                                loading = state.isLoading,
                                error = state.error,
                                contentPadding = contentPadding,
                                onRefresh = viewModel::retry,
                                onAlbumClick = onAlbumClick,
                            )
                    }

                    if (state.isLoading && !state.hasContent()) {
                        AppLoadingIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                }
            }
        }
    }

    if (showSortDialog) {
        SortByDialog(
            currentSortBy = state.sortBy,
            currentSortOrder = state.sortOrder,
            onUpdate = viewModel::changeSorting,
            onDismissRequest = { showSortDialog = false },
            availableSortOptions = MUSIC_SORT_OPTIONS,
        )
    }

    if (showDisplayDialog) {
        DisplayInfoDialog(
            fields = state.displayFields,
            onToggle = viewModel::toggleDisplayField,
            onDismiss = { showDisplayDialog = false },
            availableFields = setOf(ListDisplayField.Year),
        )
    }
}

@Composable
private fun SongsGrid(
    songs: List<dev.hdytrfli.onigiri.models.FindroidMusicSong>,
    displayFields: Set<ListDisplayField>,
    loading: Boolean,
    error: Exception?,
    contentPadding: PaddingValues,
    onRefresh: () -> Unit,
    onSongClick: (java.util.UUID) -> Unit,
    onPlayNext: (java.util.UUID) -> Unit,
    onAddToQueue: (java.util.UUID) -> Unit,
    onToggleFavorite: (java.util.UUID) -> Unit,
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
                    showYear = ListDisplayField.Year in displayFields,
                    onPlay = { onSongClick(song.id) },
                    onPlayNext = { onPlayNext(song.id) },
                    onAddToQueue = { onAddToQueue(song.id) },
                    onToggleFavorite = { onToggleFavorite(song.id) },
                    onViewArtist = song.artistId?.let { { onArtistClick(it.toString()) } },
                    onViewAlbum = song.albumId?.let { { onAlbumClick(it.toString()) } },
                )
                HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.2f))
            }
        }
    }
}

@Composable
private fun SongListRow(
    song: dev.hdytrfli.onigiri.models.FindroidMusicSong,
    showYear: Boolean,
    onPlay: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onToggleFavorite: () -> Unit,
    onViewArtist: (() -> Unit)?,
    onViewAlbum: (() -> Unit)?,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier =
            Modifier.fillMaxWidth()
                .clickable(onClick = onPlay)
                .padding(horizontal = MaterialTheme.spacings.gutter, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.medium),
    ) {
        Box(
            modifier =
                Modifier.size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            if (song.images.primary != null) {
                AsyncImage(
                    model = song.images.primary,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(
                    painter = painterResource(CoreR.drawable.ic_music),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text =
                    listOfNotNull(
                            song.artistName,
                            song.albumName,
                            song.productionYear.takeIf { showYear }?.toString(),
                        )
                        .joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = formatDuration(song.runtimeTicks),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box {
            AppIconButton(
                onClick = { menuExpanded = true },
                variant = AppButtonVariant.Ghost,
                size = AppButtonSize.Decorator,
                content = {
                    Icon(
                        painter = painterResource(CoreR.drawable.ic_ellipsis),
                        contentDescription = stringResource(CoreR.string.more_options),
                    )
                },
            )
            AppDropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                alignEnd = true,
                anchorWidth = AppButtonSize.Decorator.value,
            ) {
                MenuActionItem(
                    label = stringResource(CoreR.string.play),
                    icon = CoreR.drawable.ic_play,
                    onClick = {
                        menuExpanded = false
                        onPlay()
                    },
                )
                MenuActionItem(
                    label = stringResource(FilmR.string.music_play_next),
                    icon = CoreR.drawable.ic_skip_forward,
                    onClick = {
                        menuExpanded = false
                        onPlayNext()
                    },
                )
                MenuActionItem(
                    label = stringResource(FilmR.string.music_add_to_queue),
                    icon = CoreR.drawable.ic_plus,
                    onClick = {
                        menuExpanded = false
                        onAddToQueue()
                    },
                )
                MenuActionItem(
                    label =
                        stringResource(
                            if (song.favorite) CoreR.string.remove_from_favorites
                            else CoreR.string.add_to_favorites
                        ),
                    icon =
                        if (song.favorite) CoreR.drawable.ic_heart_filled
                        else CoreR.drawable.ic_heart,
                    onClick = {
                        menuExpanded = false
                        onToggleFavorite()
                    },
                )
                MenuActionItem(
                    label = stringResource(FilmR.string.music_artist_label),
                    icon = CoreR.drawable.ic_user,
                    enabled = onViewArtist != null,
                    onClick = {
                        menuExpanded = false
                        onViewArtist?.invoke()
                    },
                )
                MenuActionItem(
                    label = stringResource(FilmR.string.music_album_label),
                    icon = CoreR.drawable.ic_music,
                    enabled = onViewAlbum != null,
                    onClick = {
                        menuExpanded = false
                        onViewAlbum?.invoke()
                    },
                )
            }
        }
    }
}

private fun formatDuration(ticks: Long): String {
    val seconds = (ticks / 10_000_000L).coerceAtLeast(0L)
    return "%d:%02d".format(seconds / 60, seconds % 60)
}

@Composable
private fun ArtistsGrid(
    artists: List<dev.hdytrfli.onigiri.models.FindroidMusicArtist>,
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
                                artist.albumCount?.let {
                                    stringResource(FilmR.string.music_album_count, it)
                                },
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
    displayFields: Set<ListDisplayField>,
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
                                    album.artistName
                                        ?: stringResource(FilmR.string.music_unknown_artist),
                                    if (ListDisplayField.Year in displayFields) {
                                        album.productionYear
                                    } else {
                                        null
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
