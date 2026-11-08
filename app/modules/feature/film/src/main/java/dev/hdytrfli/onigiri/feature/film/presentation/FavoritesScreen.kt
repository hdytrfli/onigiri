package dev.hdytrfli.onigiri.feature.film.presentation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.hdytrfli.onigiri.core.R as CoreR
import dev.hdytrfli.onigiri.feature.film.R as FilmR
import dev.hdytrfli.onigiri.feature.film.presentation.collection.CollectionAction
import dev.hdytrfli.onigiri.feature.film.presentation.collection.CollectionState
import dev.hdytrfli.onigiri.feature.film.presentation.components.CollectionGrid
import dev.hdytrfli.onigiri.feature.film.presentation.components.EmptyStateOverlay
import dev.hdytrfli.onigiri.feature.film.presentation.components.FullPageError
import dev.hdytrfli.onigiri.feature.film.presentation.components.MusicRowAction
import dev.hdytrfli.onigiri.feature.film.presentation.components.MusicTrackRow
import dev.hdytrfli.onigiri.feature.film.presentation.components.rememberMediaDeleteController
import dev.hdytrfli.onigiri.feature.film.presentation.favorites.FavoritesViewModel
import dev.hdytrfli.onigiri.feature.film.presentation.music.MusicCard
import dev.hdytrfli.onigiri.models.FindroidItem
import dev.hdytrfli.onigiri.models.FindroidMusicFavorite
import dev.hdytrfli.onigiri.models.MusicFavoriteKind
import dev.hdytrfli.onigiri.models.mediaSections
import dev.hdytrfli.onigiri.ui.components.AppPullToRefreshBox
import dev.hdytrfli.onigiri.ui.components.LocalRootPageContentInset
import dev.hdytrfli.onigiri.ui.components.OfflineModeReload
import dev.hdytrfli.onigiri.ui.preview.dummyMovies
import dev.hdytrfli.onigiri.ui.theme.FindroidTheme
import dev.hdytrfli.onigiri.ui.theme.spacings
import dev.hdytrfli.onigiri.ui.utils.rememberSafePadding

@Composable
fun FavoritesScreen(
    onItemClick: (item: FindroidItem) -> Unit,
    viewModel: FavoritesViewModel = hiltViewModel(),
    pullToRefreshState: PullToRefreshState = rememberPullToRefreshState(),
    showPullIndicator: Boolean = true,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentSongId by viewModel.currentSongId.collectAsStateWithLifecycle()
    val deleteController = rememberMediaDeleteController()

    LaunchedEffect(true) { viewModel.loadItemsIfNeeded() }
    OfflineModeReload { viewModel.loadItems(force = true) }

    FavoritesScreenLayout(
        state = state,
        currentSongId = currentSongId,
        onItemClick = onItemClick,
        onPlaySong = viewModel::playSong,
        onPlayNext = viewModel::playNext,
        onAddToQueue = viewModel::addToQueue,
        onRemoveFavorite = viewModel::removeFavorite,
        onDeleteSong =
            if (deleteController.canDelete) {
                { item ->
                    deleteController.request(item.id, item.name) {
                        viewModel.loadItems(force = true)
                    }
                }
            } else {
                null
            },
        onRetryClick = { viewModel.loadItems(force = true) },
        pullToRefreshState = pullToRefreshState,
        showPullIndicator = showPullIndicator,
    )
}

@Composable
private fun FavoritesScreenLayout(
    state: CollectionState,
    currentSongId: java.util.UUID?,
    onItemClick: (item: FindroidItem) -> Unit,
    onPlaySong: (FindroidMusicFavorite) -> Unit,
    onPlayNext: (FindroidMusicFavorite) -> Unit,
    onAddToQueue: (FindroidMusicFavorite) -> Unit,
    onRemoveFavorite: (FindroidMusicFavorite) -> Unit,
    onDeleteSong: ((FindroidMusicFavorite) -> Unit)? = null,
    onRetryClick: () -> Unit,
    pullToRefreshState: PullToRefreshState = rememberPullToRefreshState(),
    showPullIndicator: Boolean = true,
) {
    val safePadding = rememberSafePadding()

    val paddingStart = safePadding.start + MaterialTheme.spacings.gutter
    val paddingEnd = safePadding.end + MaterialTheme.spacings.gutter
    val paddingBottom = safePadding.bottom + MaterialTheme.spacings.gutter
    val contentPadding =
        PaddingValues(
            start = paddingStart,
            top = LocalRootPageContentInset.current,
            end = paddingEnd,
            bottom = paddingBottom,
        )

    if (state.error != null && state.items.isEmpty()) {
        FullPageError(
            onRefresh = onRetryClick,
            isRefreshing = state.isLoading,
            error = state.error,
        )
        return
    }

    AppPullToRefreshBox(
        isRefreshing = state.isLoading,
        onRefresh = onRetryClick,
        modifier = Modifier.fillMaxSize(),
        state = pullToRefreshState,
        showIndicator = showPullIndicator,
    ) {
        EmptyStateOverlay(
            visible = state.sections.isEmpty(),
            loading = state.isLoading,
            context = stringResource(CoreR.string.empty_context_favorites),
            onRefresh = onRetryClick,
            title = stringResource(CoreR.string.title_favorite),
            contentPadding = contentPadding,
        ) {
            CollectionGrid(
                sections = state.sections,
                contentPadding = contentPadding,
                onAction = { action ->
                    if (action is CollectionAction.OnItemClick) {
                        onItemClick(action.item)
                    }
                },
                displayFields = state.displayFields,
                musicFavoriteContent = { item ->
                    when (item.kind) {
                        MusicFavoriteKind.Song ->
                            MusicTrackRow(
                                title = item.name,
                                subtitle = item.subtitle.orEmpty(),
                                imageUri = item.images.primary,
                                isCurrent = item.id == currentSongId,
                                actions =
                                    listOf(
                                        MusicRowAction(
                                            label =
                                                stringResource(CoreR.string.remove_from_favorites),
                                            icon = CoreR.drawable.ic_heart_filled,
                                            iconTint = MaterialTheme.colorScheme.error,
                                            onClick = { onRemoveFavorite(item) },
                                        ),
                                        MusicRowAction(
                                            label = stringResource(FilmR.string.music_play_next),
                                            icon = CoreR.drawable.ic_skip_forward,
                                            onClick = { onPlayNext(item) },
                                        ),
                                        MusicRowAction(
                                            label = stringResource(FilmR.string.music_add_to_queue),
                                            icon = CoreR.drawable.ic_plus,
                                            onClick = { onAddToQueue(item) },
                                        ),
                                    ) +
                                        if (onDeleteSong != null) {
                                            listOf(
                                                MusicRowAction(
                                                    label =
                                                        stringResource(
                                                            FilmR.string.delete_media_action
                                                        ),
                                                    icon = CoreR.drawable.ic_trash,
                                                    iconTint = MaterialTheme.colorScheme.error,
                                                    onClick = { onDeleteSong(item) },
                                                )
                                            )
                                        } else {
                                            emptyList()
                                        },
                                onClick = { onPlaySong(item) },
                            )
                        MusicFavoriteKind.Album,
                        MusicFavoriteKind.Artist ->
                            MusicCard(
                                title = item.name,
                                subtitle = item.subtitle,
                                image = item.images.primary,
                                circular = item.kind == MusicFavoriteKind.Artist,
                                placeholderIcon =
                                    if (item.kind == MusicFavoriteKind.Artist) {
                                        CoreR.drawable.ic_user
                                    } else {
                                        CoreR.drawable.ic_music
                                    },
                                onClick = { onItemClick(item) },
                            )
                    }
                },
            )
        }
    }
}

@PreviewScreenSizes
@Composable
private fun FavoritesScreenLayoutPreview() {
    FindroidTheme {
        FavoritesScreenLayout(
            state = CollectionState(sections = mediaSections(dummyMovies)),
            currentSongId = null,
            onItemClick = {},
            onPlaySong = {},
            onPlayNext = {},
            onAddToQueue = {},
            onRemoveFavorite = {},
            onRetryClick = {},
        )
    }
}
