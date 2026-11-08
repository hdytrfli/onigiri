package dev.hdytrfli.onigiri.feature.film.presentation

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.hdytrfli.onigiri.core.R as CoreR
import dev.hdytrfli.onigiri.feature.film.presentation.components.ActorsRow
import dev.hdytrfli.onigiri.feature.film.presentation.components.DetailAction
import dev.hdytrfli.onigiri.feature.film.presentation.components.DetailActionsMenu
import dev.hdytrfli.onigiri.feature.film.presentation.components.DownloadButton
import dev.hdytrfli.onigiri.feature.film.presentation.components.ExtraInfoText
import dev.hdytrfli.onigiri.feature.film.presentation.components.FullPageError
import dev.hdytrfli.onigiri.feature.film.presentation.components.InfoText
import dev.hdytrfli.onigiri.feature.film.presentation.components.ItemButtonsBar
import dev.hdytrfli.onigiri.feature.film.presentation.components.ItemHeader
import dev.hdytrfli.onigiri.feature.film.presentation.components.ItemTitle
import dev.hdytrfli.onigiri.feature.film.presentation.components.ItemTopBar
import dev.hdytrfli.onigiri.feature.film.presentation.components.OverviewText
import dev.hdytrfli.onigiri.feature.film.presentation.components.VideoMetadataBar
import dev.hdytrfli.onigiri.feature.film.presentation.downloader.DownloaderAction
import dev.hdytrfli.onigiri.feature.film.presentation.downloader.DownloaderEvent
import dev.hdytrfli.onigiri.feature.film.presentation.downloader.DownloaderState
import dev.hdytrfli.onigiri.feature.film.presentation.downloader.DownloaderViewModel
import dev.hdytrfli.onigiri.feature.film.presentation.movie.MovieAction
import dev.hdytrfli.onigiri.feature.film.presentation.movie.MovieState
import dev.hdytrfli.onigiri.feature.film.presentation.movie.MovieViewModel
import dev.hdytrfli.onigiri.ui.components.AppButtonGroup
import dev.hdytrfli.onigiri.ui.components.AppLoadingIndicator
import dev.hdytrfli.onigiri.ui.components.OfflineModeReload
import dev.hdytrfli.onigiri.ui.components.TopBarPageShell
import dev.hdytrfli.onigiri.ui.preview.dummyMovie
import dev.hdytrfli.onigiri.ui.preview.dummyVideoMetadata
import dev.hdytrfli.onigiri.ui.theme.FindroidTheme
import dev.hdytrfli.onigiri.ui.theme.LocalContentSpacingScale
import dev.hdytrfli.onigiri.ui.theme.Yellow
import dev.hdytrfli.onigiri.ui.theme.spacings
import dev.hdytrfli.onigiri.ui.utils.LocalOfflineMode
import dev.hdytrfli.onigiri.ui.utils.LocalPlaybackLauncher
import dev.hdytrfli.onigiri.ui.utils.rememberSafePadding
import dev.hdytrfli.onigiri.utils.ObserveAsEvents
import java.util.UUID
import org.jellyfin.sdk.model.api.BaseItemKind

@Composable
fun MovieScreen(
    movieId: UUID,
    navigateBack: () -> Unit,
    navigateHome: () -> Unit,
    navigateToPerson: (personId: UUID) -> Unit,
    navigateToSubtitleSearch: (itemId: UUID) -> Unit,
    navigateToIdentify: (itemId: UUID) -> Unit,
    navigateToEditImages: (itemId: UUID) -> Unit,
    navigateToDownloads: () -> Unit,
    viewModel: MovieViewModel = hiltViewModel(),
    downloaderViewModel: DownloaderViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val playbackLauncher = LocalPlaybackLauncher.current
    val uriHandler = LocalUriHandler.current
    val isOfflineMode = LocalOfflineMode.current

    val state by viewModel.state.collectAsStateWithLifecycle()
    val downloaderState by downloaderViewModel.state.collectAsStateWithLifecycle()

    // The player runs in its own activity, so returning from playback only resumes this screen.
    // Reload on resume so the resume position shown in the play button is up to date.
    LifecycleResumeEffect(Unit) {
        viewModel.loadMovie(movieId = movieId)
        onPauseOrDispose {}
    }
    OfflineModeReload { viewModel.loadMovie(movieId = movieId) }

    LaunchedEffect(state.movie) { state.movie?.let { movie -> downloaderViewModel.update(movie) } }

    ObserveAsEvents(downloaderViewModel.events) { event ->
        when (event) {
            is DownloaderEvent.Successful -> {
                viewModel.loadMovie(movieId = movieId)
            }
            is DownloaderEvent.Deleted -> {
                if (isOfflineMode) {
                    navigateBack()
                } else {
                    viewModel.loadMovie(movieId = movieId)
                }
            }
        }
    }

    MovieScreenLayout(
        state = state,
        downloaderState = downloaderState,
        onAction = { action ->
            when (action) {
                is MovieAction.Play -> {
                    playbackLauncher.launch(
                        movieId,
                        BaseItemKind.MOVIE,
                        action.startFromBeginning,
                    )
                }
                is MovieAction.PlayTrailer -> {
                    try {
                        uriHandler.openUri(action.trailer)
                    } catch (e: IllegalArgumentException) {
                        Toast.makeText(context, e.localizedMessage, Toast.LENGTH_SHORT).show()
                    }
                }
                is MovieAction.OnBackClick -> navigateBack()
                is MovieAction.OnHomeClick -> navigateHome()
                is MovieAction.NavigateToPerson -> navigateToPerson(action.personId)
                else -> Unit
            }
            viewModel.onAction(action)
        },
        onDownloaderAction = { action -> downloaderViewModel.onAction(action) },
        onSubtitleSearch = navigateToSubtitleSearch,
        onIdentify = navigateToIdentify,
        onIdentifyApplied = { viewModel.loadMovie(movieId = movieId) },
        onEditImages = navigateToEditImages,
        onNavigateToDownloads = navigateToDownloads,
        onRetry = { viewModel.loadMovie(movieId = movieId) },
    )
}

@Composable
private fun MovieScreenLayout(
    state: MovieState,
    downloaderState: DownloaderState,
    onAction: (MovieAction) -> Unit,
    onDownloaderAction: (DownloaderAction) -> Unit,
    onSubtitleSearch: (UUID) -> Unit = {},
    onIdentify: (UUID) -> Unit = {},
    onIdentifyApplied: () -> Unit = {},
    onEditImages: (UUID) -> Unit = {},
    onNavigateToDownloads: () -> Unit = {},
    onRetry: () -> Unit = {},
) {
    val safePadding = rememberSafePadding()

    val paddingStart = safePadding.start + MaterialTheme.spacings.gutter
    val paddingEnd = safePadding.end + MaterialTheme.spacings.gutter
    val paddingBottom = safePadding.bottom + MaterialTheme.spacings.gutter

    val scrollState = rememberScrollState()

    val spacingScale = LocalContentSpacingScale.current

    TopBarPageShell(
        topBar = {
            ItemTopBar(
                hasBackButton = true,
                onBackClick = { onAction(MovieAction.OnBackClick) },
                trailingContent = {
                    state.movie?.let { movie ->
                        AppButtonGroup {
                            DownloadButton(
                                item = movie,
                                downloaderState = downloaderState,
                                onDownloadClick = { storageIndex ->
                                    onDownloaderAction(
                                        DownloaderAction.Download(movie, storageIndex)
                                    )
                                },
                                onCancelDownloadClick = {
                                    onDownloaderAction(DownloaderAction.CancelDownload(movie))
                                },
                                onDeleteDownloadClick = {
                                    onDownloaderAction(DownloaderAction.DeleteDownload(movie))
                                },
                                onNavigateToDownloads = onNavigateToDownloads,
                            )
                            DetailActionsMenu(
                                itemId = movie.id,
                                actions =
                                    setOf(
                                        DetailAction.SUBTITLE_SEARCH,
                                        DetailAction.IDENTIFY,
                                        DetailAction.EDIT_IMAGES,
                                    ),
                                onSubtitleSearch = { onSubtitleSearch(movie.id) },
                                onIdentify = { onIdentify(movie.id) },
                                onIdentifyApplied = onIdentifyApplied,
                                onEditImages = { onEditImages(movie.id) },
                                onDeleted = { onAction(MovieAction.OnBackClick) },
                            )
                        }
                    }
                },
            )
        }
    ) {
        state.movie?.let { movie ->
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(scrollState)) {
                ItemHeader(
                    item = movie,
                    scrollState = scrollState,
                    content = {
                        Column(
                            modifier =
                                Modifier.align(Alignment.BottomStart)
                                    .padding(start = paddingStart, end = paddingEnd)
                        ) {
                            ItemTitle(
                                title = movie.name,
                                logoUri = movie.images.logo,
                            )
                            movie.originalTitle?.let { originalTitle ->
                                if (originalTitle != movie.name) {
                                    Text(
                                        text = originalTitle,
                                        overflow = TextOverflow.Ellipsis,
                                        maxLines = 1,
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                            }
                        }
                    },
                )
                Column(modifier = Modifier.padding(start = paddingStart, end = paddingEnd)) {
                    Spacer(Modifier.height(MaterialTheme.spacings.small * spacingScale))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        movie.premiereDate?.let { premiereDate ->
                            Text(
                                text = premiereDate.year.toString(),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        Text(
                            text =
                                stringResource(
                                    CoreR.string.runtime_minutes,
                                    movie.runtimeTicks.div(600000000),
                                ),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        movie.officialRating?.let { officialRating ->
                            Text(text = officialRating, style = MaterialTheme.typography.bodyMedium)
                        }
                        movie.communityRating?.let { communityRating ->
                            Row(verticalAlignment = Alignment.Bottom) {
                                Icon(
                                    painter = painterResource(CoreR.drawable.ic_star),
                                    contentDescription = null,
                                    tint = Yellow,
                                )
                                Spacer(Modifier.width(MaterialTheme.spacings.extraSmall))
                                Text(
                                    text = "%.1f".format(communityRating),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(MaterialTheme.spacings.small * spacingScale))
                    state.videoMetadata?.let { videoMetadata ->
                        VideoMetadataBar(videoMetadata)
                        Spacer(Modifier.height(MaterialTheme.spacings.small * spacingScale))
                    }
                    ItemButtonsBar(
                        item = movie,
                        onPlayClick = { startFromBeginning ->
                            onAction(MovieAction.Play(startFromBeginning = startFromBeginning))
                        },
                        onMarkAsPlayedClick = {
                            when (movie.played) {
                                true -> onAction(MovieAction.UnmarkAsPlayed)
                                false -> onAction(MovieAction.MarkAsPlayed)
                            }
                        },
                        onMarkAsFavoriteClick = {
                            when (movie.favorite) {
                                true -> onAction(MovieAction.UnmarkAsFavorite)
                                false -> onAction(MovieAction.MarkAsFavorite)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(MaterialTheme.spacings.medium * spacingScale))
                    OverviewText(text = movie.overview, maxCollapsedLines = 3)
                    Spacer(Modifier.height(MaterialTheme.spacings.medium * spacingScale))
                    InfoText(
                        genres = movie.genres,
                        director = state.director,
                        writers = state.writers,
                    )
                    Spacer(Modifier.height(MaterialTheme.spacings.medium * spacingScale))
                    if (state.displayExtraInfo && state.videoMetadata != null) {
                        ExtraInfoText(videoMetadata = state.videoMetadata)
                        Spacer(Modifier.height(MaterialTheme.spacings.medium * spacingScale))
                    }
                }
                if (state.actors.isNotEmpty()) {
                    ActorsRow(
                        actors = state.actors,
                        onActorClick = { personId ->
                            onAction(MovieAction.NavigateToPerson(personId))
                        },
                        contentPadding = PaddingValues(start = paddingStart, end = paddingEnd),
                    )
                }
                Spacer(Modifier.height(paddingBottom))
            }
        }
            ?: run {
                val error = state.error
                if (error != null) {
                    FullPageError(onRefresh = onRetry, isRefreshing = false, error = error)
                } else {
                    AppLoadingIndicator(modifier = Modifier.align(Alignment.Center))
                }
            }
    }
}

@PreviewScreenSizes
@Composable
private fun EpisodeScreenLayoutPreview() {
    FindroidTheme {
        MovieScreenLayout(
            state = MovieState(movie = dummyMovie, videoMetadata = dummyVideoMetadata),
            downloaderState = DownloaderState(),
            onAction = {},
            onDownloaderAction = {},
        )
    }
}
