package dev.hdytrfli.onigiri.feature.film.presentation.music

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import dev.hdytrfli.onigiri.feature.film.presentation.DisplayInfoDialog
import dev.hdytrfli.onigiri.feature.film.presentation.components.EmptyStateOverlay
import dev.hdytrfli.onigiri.feature.film.presentation.components.FullPageError
import dev.hdytrfli.onigiri.feature.film.presentation.components.ItemCountBadge
import dev.hdytrfli.onigiri.feature.film.presentation.components.ListOptionsMenu
import dev.hdytrfli.onigiri.feature.film.presentation.components.SortByDialog
import dev.hdytrfli.onigiri.feature.film.presentation.components.section
import dev.hdytrfli.onigiri.feature.film.presentation.library.ListDisplayField
import dev.hdytrfli.onigiri.ui.components.AppIconButton
import dev.hdytrfli.onigiri.ui.components.AppLoadingIndicator
import dev.hdytrfli.onigiri.ui.components.AppPullToRefreshBox
import dev.hdytrfli.onigiri.ui.components.LocalRootPageContentInset
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
        LazyVerticalGrid(
            columns = GridCells.Fixed(gridColumns(GridSize.POSTER)),
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
            horizontalArrangement = Arrangement.spacedBy(LocalGridGap.current),
            verticalArrangement = Arrangement.spacedBy(LocalGridGap.current),
        ) {
            if (songs.isNotEmpty()) {
                section(
                    title = sectionTitle,
                    trailingContent = {
                        ItemCountBadge(
                            count = songs.size,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    },
                ) {
                    items(items = songs, key = { it.id }) { song ->
                        MusicCard(
                            title = song.name,
                            subtitle =
                                buildSubtitle(
                                    song.artistName ?: song.albumName,
                                    if (ListDisplayField.Year in displayFields) {
                                        song.productionYear
                                    } else {
                                        null
                                    },
                                ),
                            image = song.images.primary,
                            circular = false,
                            placeholderIcon = CoreR.drawable.ic_music,
                        )
                    }
                }
            }
        }
    }
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
