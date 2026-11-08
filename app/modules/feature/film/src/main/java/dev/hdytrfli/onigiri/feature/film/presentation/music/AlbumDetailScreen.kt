package dev.hdytrfli.onigiri.feature.film.presentation.music

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import dev.hdytrfli.onigiri.core.R as CoreR
import dev.hdytrfli.onigiri.feature.film.R as FilmR
import dev.hdytrfli.onigiri.feature.film.presentation.components.DetailAction
import dev.hdytrfli.onigiri.feature.film.presentation.components.DetailActionsMenu
import dev.hdytrfli.onigiri.feature.film.presentation.components.FullPageError
import dev.hdytrfli.onigiri.feature.film.presentation.components.ItemCountBadge
import dev.hdytrfli.onigiri.feature.film.presentation.components.ItemTitle
import dev.hdytrfli.onigiri.feature.film.presentation.components.ItemTopBar
import dev.hdytrfli.onigiri.feature.film.presentation.components.OverviewText
import dev.hdytrfli.onigiri.feature.film.presentation.components.Section
import dev.hdytrfli.onigiri.models.FindroidMusicSong
import dev.hdytrfli.onigiri.ui.components.AppLoadingIndicator
import dev.hdytrfli.onigiri.ui.components.OfflineModeReload
import dev.hdytrfli.onigiri.ui.components.TopBarPageShell
import dev.hdytrfli.onigiri.ui.theme.spacings
import dev.hdytrfli.onigiri.ui.utils.rememberSafePadding
import java.util.UUID

@Composable
fun AlbumDetailScreen(
    albumId: String,
    navigateBack: () -> Unit,
    onArtistClick: (artistId: String) -> Unit,
    navigateToEditImages: (itemId: String) -> Unit,
    navigateToIdentify: (itemId: String) -> Unit,
    viewModel: AlbumDetailViewModel = hiltViewModel(),
) {
    val id = remember(albumId) { UUID.fromString(albumId) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    LifecycleResumeEffect(id) {
        viewModel.load(id, force = true)
        onPauseOrDispose {}
    }
    OfflineModeReload { viewModel.load(id, force = true) }

    AlbumDetailLayout(
        state = state,
        onBack = navigateBack,
        onArtistClick = onArtistClick,
        onEditImages = { state.album?.let { navigateToEditImages(it.id.toString()) } },
        onIdentify = { state.album?.let { navigateToIdentify(it.id.toString()) } },
        onPlay = { viewModel.play() },
        onShuffle = { viewModel.play(shuffle = true) },
        onFavorite = { viewModel.toggleFavorite(id) },
        onPlayNext = viewModel::playNext,
        onAddToQueue = viewModel::addToQueue,
        onSongClick = { songId -> viewModel.play(songId) },
        onRetry = { viewModel.retry(id) },
        onIdentifyApplied = { viewModel.load(id, force = true) },
    )
}

@Composable
private fun AlbumDetailLayout(
    state: AlbumDetailState,
    onBack: () -> Unit,
    onArtistClick: (artistId: String) -> Unit,
    onEditImages: () -> Unit,
    onIdentify: () -> Unit,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    onFavorite: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onSongClick: (UUID) -> Unit,
    onRetry: () -> Unit,
    onIdentifyApplied: () -> Unit,
) {
    val safePadding = rememberSafePadding()
    val paddingStart = safePadding.start + MaterialTheme.spacings.gutter
    val paddingEnd = safePadding.end + MaterialTheme.spacings.gutter
    val paddingBottom = safePadding.bottom + MaterialTheme.spacings.gutter
    val listState = rememberLazyListState()
    val album = state.album

    TopBarPageShell(
        topBar = {
            if (album != null) {
                ItemTopBar(
                    hasBackButton = true,
                    onBackClick = onBack,
                    trailingContent = {
                        DetailActionsMenu(
                            itemId = album.id,
                            actions = setOf(DetailAction.IDENTIFY, DetailAction.EDIT_IMAGES),
                            onIdentify = onIdentify,
                            onEditImages = onEditImages,
                            onIdentifyApplied = onIdentifyApplied,
                            onDeleted = onBack,
                        )
                    },
                )
            }
        }
    ) {
        when {
            state.isLoading && album == null ->
                AppLoadingIndicator(modifier = Modifier.align(Alignment.Center))
            state.error != null && album == null ->
                FullPageError(
                    onRefresh = onRetry,
                    isRefreshing = state.isLoading,
                    error = state.error,
                )
            album != null ->
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = paddingBottom),
                ) {
                    item {
                        MusicDetailHeader(
                            images = album.images,
                            lazyListState = listState,
                        ) {
                            ItemTitle(
                                title = album.name,
                                logoUri = album.images.logo,
                                modifier =
                                    Modifier.align(Alignment.BottomStart)
                                        .padding(start = paddingStart, end = paddingEnd),
                            )
                        }
                    }

                    item {
                        Column(
                            modifier = Modifier.padding(start = paddingStart, end = paddingEnd)
                        ) {
                            Spacer(Modifier.height(MaterialTheme.spacings.small))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement =
                                    Arrangement.spacedBy(MaterialTheme.spacings.small),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                album.artistName?.let { artistName ->
                                    Text(
                                        text = artistName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier =
                                            if (album.artistId != null) {
                                                Modifier.clickable {
                                                    onArtistClick(album.artistId.toString())
                                                }
                                            } else {
                                                Modifier
                                            },
                                    )
                                }
                                album.productionYear?.let {
                                    Text(
                                        text = it.toString(),
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                                Text(
                                    text =
                                        stringResource(
                                            FilmR.string.music_song_count,
                                            album.songCount ?: state.songs.size,
                                        ),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                            Spacer(Modifier.height(MaterialTheme.spacings.medium))
                            MusicActionsBar(
                                favorite = album.favorite,
                                onPlay = onPlay,
                                onShuffle = onShuffle,
                                onFavorite = onFavorite,
                                onPlayNext = onPlayNext,
                                onAddToQueue = onAddToQueue,
                            )
                            Spacer(Modifier.height(MaterialTheme.spacings.medium))
                            if (album.overview.isNotBlank()) {
                                OverviewText(text = album.overview, maxCollapsedLines = 3)
                                Spacer(Modifier.height(MaterialTheme.spacings.medium))
                            }
                        }
                    }

                    item {
                        Section(
                            title = stringResource(FilmR.string.music_songs),
                            titleModifier = Modifier.padding(horizontal = paddingStart),
                            trailingContent = {
                                ItemCountBadge(
                                    count = state.songs.size,
                                    modifier = Modifier.padding(start = 8.dp),
                                )
                            },
                        ) {
                            if (state.songs.isEmpty()) {
                                Text(
                                    text = stringResource(FilmR.string.music_no_songs),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = paddingStart),
                                )
                            } else {
                                Column {
                                    state.songs.forEachIndexed { index, song ->
                                        SongRow(
                                            song = song,
                                            horizontalPadding = paddingStart,
                                            onClick = { onSongClick(song.id) },
                                        )
                                        if (index < state.songs.lastIndex) {
                                            HorizontalDivider(
                                                color = DividerDefaults.color.copy(alpha = 0.2f),
                                                modifier =
                                                    Modifier.padding(horizontal = paddingStart),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item { Spacer(Modifier.height(MaterialTheme.spacings.medium)) }
                }
        }
    }
}

@Composable
private fun SongRow(
    song: FindroidMusicSong,
    horizontalPadding: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = horizontalPadding, vertical = 12.dp),
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
            song.artistName?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Text(
            text = formatDuration(song.runtimeTicks),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun formatDuration(ticks: Long): String {
    val seconds = (ticks / 10_000_000L).coerceAtLeast(0L)
    return "%d:%02d".format(seconds / 60, seconds % 60)
}
