package dev.hdytrfli.onigiri.feature.film.presentation

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowSizeClass
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
import dev.hdytrfli.onigiri.feature.film.presentation.components.ItemTopBar
import dev.hdytrfli.onigiri.feature.film.presentation.components.OverviewText
import dev.hdytrfli.onigiri.feature.film.presentation.components.ParentNavMenu
import dev.hdytrfli.onigiri.feature.film.presentation.components.VideoMetadataBar
import dev.hdytrfli.onigiri.feature.film.presentation.downloader.DownloaderAction
import dev.hdytrfli.onigiri.feature.film.presentation.downloader.DownloaderEvent
import dev.hdytrfli.onigiri.feature.film.presentation.downloader.DownloaderState
import dev.hdytrfli.onigiri.feature.film.presentation.downloader.DownloaderViewModel
import dev.hdytrfli.onigiri.feature.film.presentation.episode.EpisodeAction
import dev.hdytrfli.onigiri.feature.film.presentation.episode.EpisodeState
import dev.hdytrfli.onigiri.feature.film.presentation.episode.EpisodeViewModel
import dev.hdytrfli.onigiri.ui.components.AppButtonGroup
import dev.hdytrfli.onigiri.ui.components.AppLoadingIndicator
import dev.hdytrfli.onigiri.ui.components.OfflineModeReload
import dev.hdytrfli.onigiri.ui.preview.dummyEpisode
import dev.hdytrfli.onigiri.ui.preview.dummyVideoMetadata
import dev.hdytrfli.onigiri.ui.theme.FindroidTheme
import dev.hdytrfli.onigiri.ui.theme.LocalContentSpacingScale
import dev.hdytrfli.onigiri.ui.theme.Yellow
import dev.hdytrfli.onigiri.ui.theme.spacings
import dev.hdytrfli.onigiri.ui.utils.LocalOfflineMode
import dev.hdytrfli.onigiri.ui.utils.LocalPlaybackLauncher
import dev.hdytrfli.onigiri.ui.utils.rememberSafePadding
import dev.hdytrfli.onigiri.utils.ObserveAsEvents
import dev.hdytrfli.onigiri.utils.format
import java.util.UUID
import org.jellyfin.sdk.model.api.BaseItemKind

@Composable
fun EpisodeScreen(
    episodeId: UUID,
    navigateBack: () -> Unit,
    navigateHome: () -> Unit,
    navigateToPerson: (personId: UUID) -> Unit,
    navigateToSeason: (seasonId: UUID) -> Unit,
    navigateToShow: (seriesId: UUID) -> Unit,
    navigateToSubtitleSearch: (itemId: UUID) -> Unit,
    navigateToEditImages: (itemId: UUID) -> Unit,
    navigateToDownloads: () -> Unit,
    viewModel: EpisodeViewModel = hiltViewModel(),
    downloaderViewModel: DownloaderViewModel = hiltViewModel(),
) {
    val playbackLauncher = LocalPlaybackLauncher.current
    val isOfflineMode = LocalOfflineMode.current

    val state by viewModel.state.collectAsStateWithLifecycle()
    val downloaderState by downloaderViewModel.state.collectAsStateWithLifecycle()

    // The player runs in its own activity, so returning from playback only resumes this screen.
    // Reload on resume so the resume position shown in the play button is up to date.
    LifecycleResumeEffect(Unit) {
        viewModel.loadEpisode(episodeId = episodeId)
        onPauseOrDispose {}
    }
    OfflineModeReload { viewModel.loadEpisode(episodeId = episodeId) }

    LaunchedEffect(state.episode) {
        state.episode?.let { episode -> downloaderViewModel.update(episode) }
    }

    ObserveAsEvents(downloaderViewModel.events) { event ->
        when (event) {
            is DownloaderEvent.Successful -> {
                viewModel.loadEpisode(episodeId = episodeId)
            }
            is DownloaderEvent.Deleted -> {
                if (isOfflineMode) {
                    navigateBack()
                } else {
                    viewModel.loadEpisode(episodeId = episodeId)
                }
            }
        }
    }

    EpisodeScreenLayout(
        state = state,
        downloaderState = downloaderState,
        onAction = { action ->
            when (action) {
                is EpisodeAction.Play -> {
                    playbackLauncher.launch(
                        episodeId,
                        BaseItemKind.EPISODE,
                        action.startFromBeginning,
                    )
                }
                is EpisodeAction.OnBackClick -> navigateBack()
                is EpisodeAction.OnHomeClick -> navigateHome()
                is EpisodeAction.NavigateToPerson -> navigateToPerson(action.personId)
                is EpisodeAction.NavigateToSeason -> navigateToSeason(action.seasonId)
                is EpisodeAction.NavigateToShow -> navigateToShow(action.seriesId)
                else -> Unit
            }
            viewModel.onAction(action)
        },
        onDownloaderAction = { action -> downloaderViewModel.onAction(action) },
        onSubtitleSearch = navigateToSubtitleSearch,
        onEditImages = navigateToEditImages,
        onNavigateToDownloads = navigateToDownloads,
        onRetry = { viewModel.loadEpisode(episodeId = episodeId) },
    )
}

@Composable
private fun EpisodeScreenLayout(
    state: EpisodeState,
    downloaderState: DownloaderState,
    onAction: (EpisodeAction) -> Unit,
    onDownloaderAction: (DownloaderAction) -> Unit,
    onSubtitleSearch: (UUID) -> Unit = {},
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

    // Tablet keeps the season line in the header and the extra top-bar actions; compact widths show
    // only the episode name and the back button.
    val showNamesInHeader =
        currentWindowAdaptiveInfo()
            .windowSizeClass
            .isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)

    Box(modifier = Modifier.fillMaxSize()) {
        state.episode?.let { episode ->
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(scrollState)) {
                ItemHeader(
                    item = episode,
                    scrollState = scrollState,
                    content = {
                        Column(
                            modifier =
                                Modifier.align(Alignment.BottomStart)
                                    .padding(start = paddingStart, end = paddingEnd)
                        ) {
                            val seasonName =
                                episode.seasonName
                                    ?: run {
                                        stringResource(
                                            CoreR.string.season_number,
                                            episode.parentIndexNumber,
                                        )
                                    }
                            if (showNamesInHeader) {
                                Text(
                                    text =
                                        "$seasonName - " +
                                            stringResource(
                                                id = CoreR.string.episode_number,
                                                episode.indexNumber,
                                            ),
                                    maxLines = 1,
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            }
                            Text(
                                text = episode.name,
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 3,
                                style = MaterialTheme.typography.headlineMedium,
                            )
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
                        episode.premiereDate?.let { premiereDate ->
                            Text(
                                text = premiereDate.format(),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        Text(
                            text =
                                stringResource(
                                    CoreR.string.runtime_minutes,
                                    episode.runtimeTicks.div(600000000),
                                ),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        episode.communityRating?.let { communityRating ->
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
                        item = episode,
                        onPlayClick = { startFromBeginning ->
                            onAction(EpisodeAction.Play(startFromBeginning = startFromBeginning))
                        },
                        onMarkAsPlayedClick = {
                            when (episode.played) {
                                true -> onAction(EpisodeAction.UnmarkAsPlayed)
                                false -> onAction(EpisodeAction.MarkAsPlayed)
                            }
                        },
                        onMarkAsFavoriteClick = {
                            when (episode.favorite) {
                                true -> onAction(EpisodeAction.UnmarkAsFavorite)
                                false -> onAction(EpisodeAction.MarkAsFavorite)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(MaterialTheme.spacings.medium * spacingScale))
                    OverviewText(text = episode.overview, maxCollapsedLines = 3)
                    Spacer(Modifier.height(MaterialTheme.spacings.medium * spacingScale))
                    if (state.director != null || state.writers.isNotEmpty()) {
                        InfoText(
                            genres = emptyList(),
                            director = state.director,
                            writers = state.writers,
                        )
                        Spacer(Modifier.height(MaterialTheme.spacings.medium * spacingScale))
                    }
                    if (state.displayExtraInfo && state.videoMetadata != null) {
                        ExtraInfoText(videoMetadata = state.videoMetadata)
                        Spacer(Modifier.height(MaterialTheme.spacings.medium * spacingScale))
                    }
                }
                if (state.actors.isNotEmpty()) {
                    ActorsRow(
                        actors = state.actors,
                        onActorClick = { personId ->
                            onAction(EpisodeAction.NavigateToPerson(personId))
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

        ItemTopBar(
            hasBackButton = true,
            onBackClick = { onAction(EpisodeAction.OnBackClick) },
            content = {
                state.episode?.let { episode ->
                    Spacer(modifier = Modifier.width(MaterialTheme.spacings.small))
                    ParentNavMenu(
                        onShowClick = { onAction(EpisodeAction.NavigateToShow(episode.seriesId)) },
                        onSeasonClick = {
                            onAction(EpisodeAction.NavigateToSeason(episode.seasonId))
                        },
                    )
                }
            },
            trailingContent = {
                state.episode?.let { episode ->
                    AppButtonGroup {
                        DownloadButton(
                            item = episode,
                            downloaderState = downloaderState,
                            onDownloadClick = { storageIndex ->
                                onDownloaderAction(DownloaderAction.Download(episode, storageIndex))
                            },
                            onCancelDownloadClick = {
                                onDownloaderAction(DownloaderAction.CancelDownload(episode))
                            },
                            onDeleteDownloadClick = {
                                onDownloaderAction(DownloaderAction.DeleteDownload(episode))
                            },
                            onNavigateToDownloads = onNavigateToDownloads,
                        )
                        DetailActionsMenu(
                            itemId = episode.id,
                            actions = setOf(DetailAction.SUBTITLE_SEARCH, DetailAction.EDIT_IMAGES),
                            onSubtitleSearch = { onSubtitleSearch(episode.id) },
                            onEditImages = { onEditImages(episode.id) },
                        )
                    }
                }
            },
        )
    }
}

@PreviewScreenSizes
@Composable
private fun EpisodeScreenLayoutPreview() {
    FindroidTheme {
        EpisodeScreenLayout(
            state = EpisodeState(episode = dummyEpisode, videoMetadata = dummyVideoMetadata),
            downloaderState = DownloaderState(),
            onAction = {},
            onDownloaderAction = {},
        )
    }
}
