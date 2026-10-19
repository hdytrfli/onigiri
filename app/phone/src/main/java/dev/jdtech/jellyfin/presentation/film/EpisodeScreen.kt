package dev.jdtech.jellyfin.presentation.film

import android.content.Intent
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.jdtech.jellyfin.PlayerActivity
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.core.presentation.downloader.DownloaderAction
import dev.jdtech.jellyfin.core.presentation.downloader.DownloaderEvent
import dev.jdtech.jellyfin.core.presentation.downloader.DownloaderState
import dev.jdtech.jellyfin.core.presentation.downloader.DownloaderViewModel
import dev.jdtech.jellyfin.core.presentation.dummy.dummyEpisode
import dev.jdtech.jellyfin.core.presentation.dummy.dummyVideoMetadata
import dev.jdtech.jellyfin.core.presentation.theme.Yellow
import dev.jdtech.jellyfin.film.presentation.episode.EpisodeAction
import dev.jdtech.jellyfin.film.presentation.episode.EpisodeState
import dev.jdtech.jellyfin.film.presentation.episode.EpisodeViewModel
import dev.jdtech.jellyfin.presentation.components.AppButton
import dev.jdtech.jellyfin.presentation.components.AppButtonVariant
import dev.jdtech.jellyfin.presentation.film.components.ActorsRow
import dev.jdtech.jellyfin.presentation.film.components.DetailAction
import dev.jdtech.jellyfin.presentation.film.components.DetailActionsMenu
import dev.jdtech.jellyfin.presentation.film.components.DownloadButton
import dev.jdtech.jellyfin.presentation.film.components.ExtraInfoText
import dev.jdtech.jellyfin.presentation.film.components.InfoText
import dev.jdtech.jellyfin.presentation.film.components.ItemButtonsBar
import dev.jdtech.jellyfin.presentation.film.components.ItemHeader
import dev.jdtech.jellyfin.presentation.film.components.ItemTopBar
import dev.jdtech.jellyfin.presentation.film.components.OverviewText
import dev.jdtech.jellyfin.presentation.film.components.VideoMetadataBar
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.LocalContentSpacingScale
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.presentation.utils.LocalOfflineMode
import dev.jdtech.jellyfin.presentation.utils.rememberSafePadding
import dev.jdtech.jellyfin.utils.ObserveAsEvents
import dev.jdtech.jellyfin.utils.format
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
    val context = LocalContext.current
    val isOfflineMode = LocalOfflineMode.current

    val state by viewModel.state.collectAsStateWithLifecycle()
    val downloaderState by downloaderViewModel.state.collectAsStateWithLifecycle()

    // The player runs in its own activity, so returning from playback only resumes this screen.
    // Reload on resume so the resume position shown in the play button is up to date.
    LifecycleResumeEffect(Unit) {
        viewModel.loadEpisode(episodeId = episodeId)
        onPauseOrDispose {}
    }

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
                    val intent = Intent(context, PlayerActivity::class.java)
                    intent.putExtra("itemId", episodeId.toString())
                    intent.putExtra("itemKind", BaseItemKind.EPISODE.serialName)
                    intent.putExtra("startFromBeginning", action.startFromBeginning)
                    context.startActivity(intent)
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
) {
    val safePadding = rememberSafePadding()

    val paddingStart = safePadding.start + MaterialTheme.spacings.default
    val paddingEnd = safePadding.end + MaterialTheme.spacings.default
    val paddingBottom = safePadding.bottom + MaterialTheme.spacings.default

    val scrollState = rememberScrollState()

    val spacingScale = LocalContentSpacingScale.current

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
                    Spacer(Modifier.height(MaterialTheme.spacings.small * spacingScale))
                    if (state.displayExtraInfo && state.videoMetadata != null) {
                        ExtraInfoText(videoMetadata = state.videoMetadata!!)
                    }
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
        } ?: run { CircularProgressIndicator(modifier = Modifier.align(Alignment.Center)) }

        ItemTopBar(
            hasBackButton = true,
            hasHomeButton = true,
            onBackClick = { onAction(EpisodeAction.OnBackClick) },
            onHomeClick = { onAction(EpisodeAction.OnHomeClick) },
            content = {
                Spacer(modifier = Modifier.width(MaterialTheme.spacings.extraSmall))
                state.episode?.let { episode ->
                    AppButton(
                        onClick = { onAction(EpisodeAction.NavigateToShow(episode.seriesId)) },
                        variant = AppButtonVariant.Overlay,
                    ) {
                        Text(
                            text = episode.seriesName,
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1,
                        )
                    }
                    Spacer(modifier = Modifier.width(MaterialTheme.spacings.small))
                    AppButton(
                        onClick = { onAction(EpisodeAction.NavigateToSeason(episode.seasonId)) },
                        variant = AppButtonVariant.Overlay,
                    ) {
                        episode.seasonName?.let { seasonName -> Text(seasonName) }
                            ?: run {
                                Text(
                                    stringResource(
                                        CoreR.string.season_number,
                                        episode.parentIndexNumber,
                                    )
                                )
                            }
                    }
                }
            },
            trailingContent = {
                state.episode?.let { episode ->
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
                    Spacer(modifier = Modifier.width(MaterialTheme.spacings.small))
                    DetailActionsMenu(
                        itemId = episode.id,
                        actions = setOf(DetailAction.SUBTITLE_SEARCH, DetailAction.EDIT_IMAGES),
                        onSubtitleSearch = { onSubtitleSearch(episode.id) },
                        onEditImages = { onEditImages(episode.id) },
                    )
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
