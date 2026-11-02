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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.hdytrfli.onigiri.feature.film.presentation.components.DetailAction
import dev.hdytrfli.onigiri.feature.film.presentation.components.DetailActionsMenu
import dev.hdytrfli.onigiri.feature.film.presentation.components.EpisodeGridCard
import dev.hdytrfli.onigiri.feature.film.presentation.components.FullPageError
import dev.hdytrfli.onigiri.feature.film.presentation.components.ItemButtonsBar
import dev.hdytrfli.onigiri.feature.film.presentation.components.ItemHeader
import dev.hdytrfli.onigiri.feature.film.presentation.components.ItemTitle
import dev.hdytrfli.onigiri.feature.film.presentation.components.ItemTopBar
import dev.hdytrfli.onigiri.feature.film.presentation.components.OverviewText
import dev.hdytrfli.onigiri.feature.film.presentation.components.ParentNavMenu
import dev.hdytrfli.onigiri.feature.film.presentation.season.SeasonAction
import dev.hdytrfli.onigiri.feature.film.presentation.season.SeasonState
import dev.hdytrfli.onigiri.feature.film.presentation.season.SeasonViewModel
import dev.hdytrfli.onigiri.models.FindroidItem
import dev.hdytrfli.onigiri.ui.components.AppLoadingIndicator
import dev.hdytrfli.onigiri.ui.components.OfflineModeReload
import dev.hdytrfli.onigiri.ui.preview.dummySeason
import dev.hdytrfli.onigiri.ui.theme.FindroidTheme
import dev.hdytrfli.onigiri.ui.theme.LocalContentSpacingScale
import dev.hdytrfli.onigiri.ui.theme.spacings
import dev.hdytrfli.onigiri.ui.utils.GridSize
import dev.hdytrfli.onigiri.ui.utils.LocalGridGap
import dev.hdytrfli.onigiri.ui.utils.LocalPlaybackLauncher
import dev.hdytrfli.onigiri.ui.utils.gridColumns
import dev.hdytrfli.onigiri.ui.utils.rememberSafePadding
import java.util.UUID
import org.jellyfin.sdk.model.api.BaseItemKind

@Composable
fun SeasonScreen(
    seasonId: UUID,
    navigateBack: () -> Unit,
    navigateHome: () -> Unit,
    navigateToItem: (item: FindroidItem) -> Unit,
    navigateToSeries: (seriesId: UUID) -> Unit,
    navigateToEditImages: (itemId: UUID) -> Unit,
    viewModel: SeasonViewModel = hiltViewModel(),
) {
    val playbackLauncher = LocalPlaybackLauncher.current
    val state by viewModel.state.collectAsStateWithLifecycle()

    // The player runs in its own activity, so returning from playback only resumes this screen.
    // Reload on resume so anything playback changed is reflected.
    LifecycleResumeEffect(Unit) {
        viewModel.loadSeason(seasonId = seasonId)
        onPauseOrDispose {}
    }
    OfflineModeReload { viewModel.loadSeason(seasonId = seasonId) }

    SeasonScreenLayout(
        state = state,
        onAction = { action ->
            when (action) {
                is SeasonAction.Play -> {
                    playbackLauncher.launch(seasonId, BaseItemKind.SEASON, false)
                }
                is SeasonAction.OnBackClick -> navigateBack()
                is SeasonAction.OnHomeClick -> navigateHome()
                is SeasonAction.NavigateToItem -> navigateToItem(action.item)
                is SeasonAction.NavigateToSeries -> navigateToSeries(action.seriesId)
                else -> Unit
            }
            viewModel.onAction(action)
        },
        onEditImages = navigateToEditImages,
        onRetry = { viewModel.loadSeason(seasonId = seasonId) },
    )
}

@Composable
private fun SeasonScreenLayout(
    state: SeasonState,
    onAction: (SeasonAction) -> Unit,
    onEditImages: (UUID) -> Unit = {},
    onRetry: () -> Unit = {},
) {
    val safePadding = rememberSafePadding()

    val paddingStart = safePadding.start + MaterialTheme.spacings.gutter
    val paddingEnd = safePadding.end + MaterialTheme.spacings.gutter
    val paddingBottom = safePadding.bottom + MaterialTheme.spacings.gutter

    val lazyListState = rememberLazyListState()

    val episodeColumns = gridColumns(GridSize.THUMBNAIL)

    val spacingScale = LocalContentSpacingScale.current

    Box(modifier = Modifier.fillMaxSize()) {
        state.season?.let { season ->
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                state = lazyListState,
                contentPadding = PaddingValues(bottom = paddingBottom),
                verticalArrangement =
                    Arrangement.spacedBy(MaterialTheme.spacings.default * spacingScale),
            ) {
                item {
                    ItemHeader(
                        item = season,
                        lazyListState = lazyListState,
                        content = {
                            Column(
                                modifier =
                                    Modifier.align(Alignment.BottomStart)
                                        .padding(start = paddingStart, end = paddingEnd)
                            ) {
                                ItemTitle(
                                    title = season.seriesName,
                                    logoUri = season.images.showLogo,
                                    maxLines = 1,
                                )
                                Text(
                                    text = season.name,
                                    overflow = TextOverflow.Ellipsis,
                                    maxLines = 1,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        },
                    )
                    Spacer(Modifier.height(MaterialTheme.spacings.small * spacingScale))
                    season.productionYear?.let { year ->
                        Text(
                            text = year.toString(),
                            modifier = Modifier.padding(start = paddingStart, end = paddingEnd),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Spacer(Modifier.height(MaterialTheme.spacings.small * spacingScale))
                    }
                    ItemButtonsBar(
                        item = season,
                        onPlayClick = { startFromBeginning ->
                            onAction(SeasonAction.Play(startFromBeginning = startFromBeginning))
                        },
                        onMarkAsPlayedClick = {
                            when (season.played) {
                                true -> onAction(SeasonAction.UnmarkAsPlayed)
                                false -> onAction(SeasonAction.MarkAsPlayed)
                            }
                        },
                        onMarkAsFavoriteClick = {
                            when (season.favorite) {
                                true -> onAction(SeasonAction.UnmarkAsFavorite)
                                false -> onAction(SeasonAction.MarkAsFavorite)
                            }
                        },
                        modifier =
                            Modifier.padding(start = paddingStart, end = paddingEnd).fillMaxWidth(),
                        canPlay = state.episodes.isNotEmpty(),
                    )
                    Spacer(Modifier.height(MaterialTheme.spacings.medium * spacingScale))
                    if (season.overview.isNotBlank()) {
                        Column(
                            modifier = Modifier.padding(start = paddingStart, end = paddingEnd)
                        ) {
                            OverviewText(text = season.overview, maxCollapsedLines = 3)
                        }
                        Spacer(Modifier.height(MaterialTheme.spacings.medium * spacingScale))
                    }
                }
                items(state.episodes.chunked(episodeColumns)) { rowEpisodes ->
                    Row(
                        modifier =
                            Modifier.fillMaxWidth().padding(start = paddingStart, end = paddingEnd),
                        horizontalArrangement = Arrangement.spacedBy(LocalGridGap.current),
                    ) {
                        rowEpisodes.forEach { episode ->
                            EpisodeGridCard(
                                episode = episode,
                                onClick = { onAction(SeasonAction.NavigateToItem(episode)) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        repeat(episodeColumns - rowEpisodes.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
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
            onBackClick = { onAction(SeasonAction.OnBackClick) },
            trailingContent = {
                state.season?.let { season ->
                    DetailActionsMenu(
                        itemId = season.id,
                        actions = setOf(DetailAction.EDIT_IMAGES),
                        onEditImages = { onEditImages(season.id) },
                    )
                }
            },
            content = {
                state.season?.let { season ->
                    Spacer(modifier = Modifier.width(MaterialTheme.spacings.small))
                    ParentNavMenu(
                        onShowClick = { onAction(SeasonAction.NavigateToSeries(season.seriesId)) }
                    )
                }
            },
        )
    }
}

@PreviewScreenSizes
@Composable
private fun SeasonScreenLayoutPreview() {
    FindroidTheme { SeasonScreenLayout(state = SeasonState(season = dummySeason), onAction = {}) }
}
