package dev.hdytrfli.onigiri.feature.film.presentation.music

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.hdytrfli.onigiri.feature.film.R as FilmR
import dev.hdytrfli.onigiri.feature.film.presentation.components.DetailAction
import dev.hdytrfli.onigiri.feature.film.presentation.components.DetailActionsMenu
import dev.hdytrfli.onigiri.feature.film.presentation.components.FullPageError
import dev.hdytrfli.onigiri.feature.film.presentation.components.GridRows
import dev.hdytrfli.onigiri.feature.film.presentation.components.ItemCountBadge
import dev.hdytrfli.onigiri.feature.film.presentation.components.ItemTitle
import dev.hdytrfli.onigiri.feature.film.presentation.components.ItemTopBar
import dev.hdytrfli.onigiri.feature.film.presentation.components.OverviewText
import dev.hdytrfli.onigiri.feature.film.presentation.components.Section
import dev.hdytrfli.onigiri.ui.components.AppLoadingIndicator
import dev.hdytrfli.onigiri.ui.components.OfflineModeReload
import dev.hdytrfli.onigiri.ui.theme.spacings
import dev.hdytrfli.onigiri.ui.utils.GridSize
import dev.hdytrfli.onigiri.ui.utils.gridColumns
import dev.hdytrfli.onigiri.ui.utils.rememberSafePadding
import java.util.UUID

@Composable
fun ArtistDetailScreen(
    artistId: String,
    navigateBack: () -> Unit,
    onAlbumClick: (albumId: String) -> Unit,
    navigateToEditImages: (itemId: String) -> Unit,
    navigateToIdentify: (itemId: String) -> Unit,
    viewModel: ArtistDetailViewModel = hiltViewModel(),
) {
    val id = remember(artistId) { UUID.fromString(artistId) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    LifecycleResumeEffect(id) {
        viewModel.load(id, force = true)
        onPauseOrDispose {}
    }
    OfflineModeReload { viewModel.load(id, force = true) }

    ArtistDetailLayout(
        state = state,
        onBack = navigateBack,
        onAlbumClick = onAlbumClick,
        onEditImages = { state.artist?.let { navigateToEditImages(it.id.toString()) } },
        onIdentify = { state.artist?.let { navigateToIdentify(it.id.toString()) } },
        onRetry = { viewModel.retry(id) },
        onIdentifyApplied = { viewModel.load(id, force = true) },
    )
}

@Composable
private fun ArtistDetailLayout(
    state: ArtistDetailState,
    onBack: () -> Unit,
    onAlbumClick: (albumId: String) -> Unit,
    onEditImages: () -> Unit,
    onIdentify: () -> Unit,
    onRetry: () -> Unit,
    onIdentifyApplied: () -> Unit,
) {
    val safePadding = rememberSafePadding()
    val paddingStart = safePadding.start + MaterialTheme.spacings.gutter
    val paddingEnd = safePadding.end + MaterialTheme.spacings.gutter
    val paddingBottom = safePadding.bottom + MaterialTheme.spacings.gutter
    val listState = rememberLazyListState()
    val artist = state.artist

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            state.isLoading && artist == null ->
                AppLoadingIndicator(modifier = Modifier.align(Alignment.Center))
            state.error != null && artist == null ->
                FullPageError(
                    onRefresh = onRetry,
                    isRefreshing = state.isLoading,
                    error = state.error,
                )
            artist != null ->
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = paddingBottom),
                ) {
                    item {
                        MusicDetailHeader(
                            images = artist.images,
                            lazyListState = listState,
                        ) {
                            Column(
                                modifier =
                                    Modifier.align(Alignment.BottomStart)
                                        .padding(start = paddingStart, end = paddingEnd)
                            ) {
                                ItemTitle(
                                    title = artist.name,
                                    logoUri = artist.images.logo,
                                )
                                artist.albumCount?.let {
                                    Text(
                                        text = stringResource(FilmR.string.music_album_count, it),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }

                    if (artist.overview.isNotBlank()) {
                        item {
                            Spacer(Modifier.height(MaterialTheme.spacings.medium))
                            OverviewText(
                                text = artist.overview,
                                maxCollapsedLines = 3,
                                modifier = Modifier.padding(horizontal = paddingStart),
                            )
                        }
                    }

                    item {
                        Spacer(Modifier.height(MaterialTheme.spacings.medium))
                        Section(
                            title = stringResource(FilmR.string.music_albums),
                            titleModifier = Modifier.padding(horizontal = paddingStart),
                            trailingContent = {
                                ItemCountBadge(
                                    count = state.albums.size,
                                    modifier = Modifier.padding(start = 8.dp),
                                )
                            },
                        ) {
                            if (state.albums.isEmpty()) {
                                Text(
                                    text = stringResource(FilmR.string.music_no_albums),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = paddingStart),
                                )
                            } else {
                                Row(
                                    modifier =
                                        Modifier.fillMaxWidth()
                                            .padding(start = paddingStart, end = paddingEnd)
                                ) {
                                    GridRows(
                                        items = state.albums,
                                        columns = gridColumns(GridSize.POSTER),
                                        modifier = Modifier.fillMaxWidth(),
                                    ) { album ->
                                        MusicCard(
                                            title = album.name,
                                            subtitle =
                                                listOfNotNull(
                                                        album.artistName
                                                            ?: stringResource(
                                                                FilmR.string.music_unknown_artist
                                                            ),
                                                        album.productionYear?.toString(),
                                                    )
                                                    .joinToString(" · "),
                                            image = album.images.primary,
                                            circular = false,
                                            onClick = { onAlbumClick(album.id.toString()) },
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item { Spacer(Modifier.height(MaterialTheme.spacings.medium)) }
                }
        }

        if (artist != null) {
            ItemTopBar(
                hasBackButton = true,
                onBackClick = onBack,
                trailingContent = {
                    DetailActionsMenu(
                        itemId = artist.id,
                        actions = setOf(DetailAction.IDENTIFY, DetailAction.EDIT_IMAGES),
                        onIdentify = onIdentify,
                        onEditImages = onEditImages,
                        onIdentifyApplied = onIdentifyApplied,
                    )
                },
            )
        }
    }
}
