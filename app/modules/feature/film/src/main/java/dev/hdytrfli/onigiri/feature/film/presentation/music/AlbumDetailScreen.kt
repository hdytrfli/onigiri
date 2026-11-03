package dev.hdytrfli.onigiri.feature.film.presentation.music

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import dev.hdytrfli.onigiri.feature.film.R as FilmR
import dev.hdytrfli.onigiri.feature.film.presentation.components.DetailAction
import dev.hdytrfli.onigiri.feature.film.presentation.components.DetailActionsMenu
import dev.hdytrfli.onigiri.feature.film.presentation.components.FullPageError
import dev.hdytrfli.onigiri.feature.film.presentation.components.ItemTopBar
import dev.hdytrfli.onigiri.feature.film.presentation.components.Section
import dev.hdytrfli.onigiri.models.FindroidMusicAlbum
import dev.hdytrfli.onigiri.models.FindroidMusicSong
import dev.hdytrfli.onigiri.ui.components.AppLoadingIndicator
import dev.hdytrfli.onigiri.ui.components.OfflineModeReload
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

    LaunchedEffect(id) { viewModel.load(id) }
    OfflineModeReload { viewModel.load(id, force = true) }

    AlbumDetailLayout(
        state = state,
        onBack = navigateBack,
        onArtistClick = onArtistClick,
        onEditImages = { state.album?.let { navigateToEditImages(it.id.toString()) } },
        onIdentify = { state.album?.let { navigateToIdentify(it.id.toString()) } },
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
    onRetry: () -> Unit,
    onIdentifyApplied: () -> Unit,
) {
    val safePadding = rememberSafePadding()
    val gutter = MaterialTheme.spacings.gutter
    val album = state.album

    Box(modifier = Modifier.fillMaxSize()) {
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
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = safePadding.bottom + gutter),
                ) {
                    item {
                        AlbumHero(
                            album = album,
                            onArtistClick = onArtistClick,
                        )
                    }

                    item {
                        Section(
                            title = stringResource(FilmR.string.music_songs),
                            titleModifier = Modifier.padding(horizontal = gutter),
                        ) {
                            if (state.songs.isEmpty()) {
                                Text(
                                    text = stringResource(FilmR.string.music_no_songs),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = gutter),
                                )
                            } else {
                                Column {
                                    state.songs.forEachIndexed { index, song ->
                                        SongRow(index = index, song = song)
                                        if (index < state.songs.lastIndex) {
                                            HorizontalDivider(
                                                color = DividerDefaults.color.copy(alpha = 0.2f),
                                                modifier = Modifier.padding(horizontal = gutter),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item { Spacer(Modifier.height(gutter)) }
                }
        }

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
                    )
                },
            )
        }
    }
}

@Composable
private fun AlbumHero(
    album: FindroidMusicAlbum,
    onArtistClick: (artistId: String) -> Unit,
) {
    val gutter = MaterialTheme.spacings.gutter
    val image = album.images.backdrop ?: album.images.primary

    Box(modifier = Modifier.fillMaxWidth()) {
        if (image != null) {
            AsyncImage(
                model = image,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
            )
        } else {
            Box(
                modifier =
                    Modifier.fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .background(MaterialTheme.colorScheme.surfaceContainer)
            )
        }

        Box(
            modifier =
                Modifier.matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.background.copy(alpha = 0.85f),
                            )
                        )
                    )
        )

        Column(
            modifier = Modifier.align(Alignment.BottomStart).padding(gutter),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small),
        ) {
            Text(
                text = album.name,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small),
            ) {
                album.artistName?.let { artistName ->
                    Text(
                        text = artistName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier =
                            if (album.artistId != null) {
                                Modifier.clickable { onArtistClick(album.artistId.toString()) }
                            } else {
                                Modifier
                            },
                    )
                }
                album.productionYear?.let {
                    Text(
                        text = it.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            album.songCount?.let {
                Text(
                    text = stringResource(FilmR.string.music_song_count, it),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SongRow(index: Int, song: FindroidMusicSong) {
    val gutter = MaterialTheme.spacings.gutter
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = gutter, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.medium),
    ) {
        Text(
            text = (song.indexNumber ?: index + 1).toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(24.dp),
        )
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
