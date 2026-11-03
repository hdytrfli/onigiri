package dev.hdytrfli.onigiri.feature.film.presentation.music

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
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
import dev.hdytrfli.onigiri.feature.film.presentation.components.GridRows
import dev.hdytrfli.onigiri.feature.film.presentation.components.ItemTopBar
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

    LaunchedEffect(id) { viewModel.load(id) }
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
    val gutter = MaterialTheme.spacings.gutter
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
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = safePadding.bottom + gutter),
                ) {
                    item { ArtistHero(artist = artist) }

                    artist.overview
                        .takeIf { it.isNotBlank() }
                        ?.let { overview ->
                            item {
                                Text(
                                    text = overview,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 4,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier =
                                        Modifier.padding(horizontal = gutter, vertical = gutter),
                                )
                            }
                        }

                    item {
                        Section(
                            title = stringResource(FilmR.string.music_albums),
                            titleModifier = Modifier.padding(horizontal = gutter),
                        ) {
                            if (state.albums.isEmpty()) {
                                Text(
                                    text = stringResource(FilmR.string.music_no_albums),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = gutter),
                                )
                            } else {
                                GridRows(
                                    items = state.albums,
                                    columns = gridColumns(GridSize.POSTER),
                                    modifier = Modifier.padding(horizontal = gutter),
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

                    item { Spacer(Modifier.height(gutter)) }
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

@Composable
private fun ArtistHero(artist: dev.hdytrfli.onigiri.models.FindroidMusicArtist) {
    val gutter = MaterialTheme.spacings.gutter
    val backdrop = artist.images.backdrop ?: artist.images.primary

    Box(modifier = Modifier.fillMaxWidth()) {
        if (backdrop != null) {
            AsyncImage(
                model = backdrop,
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
            if (artist.images.logo != null) {
                AsyncImage(
                    model = artist.images.logo,
                    contentDescription = artist.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.height(72.dp).widthIn(max = 240.dp),
                )
            } else {
                Text(
                    text = artist.name,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }

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
