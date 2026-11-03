package dev.hdytrfli.onigiri.feature.film.presentation.music

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.hdytrfli.onigiri.core.R as CoreR
import dev.hdytrfli.onigiri.feature.film.R as FilmR
import dev.hdytrfli.onigiri.feature.film.presentation.components.EmptyStateOverlay
import dev.hdytrfli.onigiri.feature.film.presentation.components.FullPageError
import dev.hdytrfli.onigiri.ui.components.AppLoadingIndicator
import dev.hdytrfli.onigiri.ui.components.AppPullToRefreshBox
import dev.hdytrfli.onigiri.ui.components.LocalRootPageContentInset
import dev.hdytrfli.onigiri.ui.components.OfflineModeReload
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
    val pullState = rememberPullToRefreshState()

    LaunchedEffect(Unit) { viewModel.load() }
    OfflineModeReload { viewModel.load(force = true) }

    RootPage(
        header = {
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
                },
            )
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
                                state = state,
                                contentPadding = contentPadding,
                                onRefresh = viewModel::retry,
                            )
                        MusicTab.ARTISTS ->
                            ArtistsGrid(
                                state = state,
                                contentPadding = contentPadding,
                                onRefresh = viewModel::retry,
                                onArtistClick = onArtistClick,
                            )
                        MusicTab.ALBUMS ->
                            AlbumsGrid(
                                state = state,
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
}

@Composable
private fun SongsGrid(
    state: MusicState,
    contentPadding: PaddingValues,
    onRefresh: () -> Unit,
) {
    if (state.error != null && state.songs.isEmpty()) {
        FullPageError(
            onRefresh = onRefresh,
            isRefreshing = state.isLoading,
            error = state.error,
        )
        return
    }

    EmptyStateOverlay(
        visible = state.songs.isEmpty(),
        loading = state.isLoading,
        context = stringResource(FilmR.string.music_context_songs),
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
            items(items = state.songs, key = { it.id }) { song ->
                MusicCard(
                    title = song.name,
                    subtitle = song.artistName ?: song.albumName,
                    image = song.images.primary,
                    circular = false,
                    placeholderIcon = CoreR.drawable.ic_music,
                )
            }
        }
    }
}

@Composable
private fun ArtistsGrid(
    state: MusicState,
    contentPadding: PaddingValues,
    onRefresh: () -> Unit,
    onArtistClick: (artistId: String) -> Unit,
) {
    if (state.error != null && state.artists.isEmpty()) {
        FullPageError(
            onRefresh = onRefresh,
            isRefreshing = state.isLoading,
            error = state.error,
        )
        return
    }

    EmptyStateOverlay(
        visible = state.artists.isEmpty(),
        loading = state.isLoading,
        context = stringResource(FilmR.string.music_context_artists),
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
            items(items = state.artists, key = { it.id }) { artist ->
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

@Composable
private fun AlbumsGrid(
    state: MusicState,
    contentPadding: PaddingValues,
    onRefresh: () -> Unit,
    onAlbumClick: (albumId: String) -> Unit,
) {
    if (state.error != null && state.albums.isEmpty()) {
        FullPageError(
            onRefresh = onRefresh,
            isRefreshing = state.isLoading,
            error = state.error,
        )
        return
    }

    EmptyStateOverlay(
        visible = state.albums.isEmpty(),
        loading = state.isLoading,
        context = stringResource(FilmR.string.music_context_albums),
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
            items(items = state.albums, key = { it.id }) { album ->
                MusicCard(
                    title = album.name,
                    subtitle =
                        listOfNotNull(
                                album.artistName
                                    ?: stringResource(FilmR.string.music_unknown_artist),
                                album.productionYear?.toString(),
                            )
                            .joinToString(" · "),
                    image = album.images.primary,
                    circular = false,
                    placeholderIcon = CoreR.drawable.ic_music,
                    onClick = { onAlbumClick(album.id.toString()) },
                )
            }
        }
    }
}

private fun MusicState.hasContent(): Boolean =
    songs.isNotEmpty() || artists.isNotEmpty() || albums.isNotEmpty()
