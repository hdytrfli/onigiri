package dev.jdtech.jellyfin.presentation.film

import android.content.Intent
import android.content.res.Configuration
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.jdtech.jellyfin.PlayerActivity
import dev.jdtech.jellyfin.core.presentation.dummy.dummySeason
import dev.jdtech.jellyfin.film.presentation.season.SeasonAction
import dev.jdtech.jellyfin.film.presentation.season.SeasonState
import dev.jdtech.jellyfin.film.presentation.season.SeasonViewModel
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.presentation.film.components.EpisodeGridCard
import dev.jdtech.jellyfin.presentation.film.components.ItemButtonsBar
import dev.jdtech.jellyfin.presentation.film.components.ItemHeader
import dev.jdtech.jellyfin.presentation.film.components.ItemTitle
import dev.jdtech.jellyfin.presentation.film.components.ItemTopBar
import dev.jdtech.jellyfin.presentation.film.components.OverviewText
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.LocalContentSpacingScale
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.presentation.utils.rememberSafePadding
import java.util.UUID
import org.jellyfin.sdk.model.api.BaseItemKind

@Composable
fun SeasonScreen(
    seasonId: UUID,
    navigateBack: () -> Unit,
    navigateHome: () -> Unit,
    navigateToItem: (item: FindroidItem) -> Unit,
    navigateToSeries: (seriesId: UUID) -> Unit,
    viewModel: SeasonViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()

    // The player runs in its own activity, so returning from playback only resumes this screen.
    // Reload on resume so anything playback changed is reflected.
    LifecycleResumeEffect(Unit) {
        viewModel.loadSeason(seasonId = seasonId)
        onPauseOrDispose {}
    }

    SeasonScreenLayout(
        state = state,
        onAction = { action ->
            when (action) {
                is SeasonAction.Play -> {
                    val intent = Intent(context, PlayerActivity::class.java)
                    intent.putExtra("itemId", seasonId.toString())
                    intent.putExtra("itemKind", BaseItemKind.SEASON.serialName)
                    context.startActivity(intent)
                }
                is SeasonAction.OnBackClick -> navigateBack()
                is SeasonAction.OnHomeClick -> navigateHome()
                is SeasonAction.NavigateToItem -> navigateToItem(action.item)
                is SeasonAction.NavigateToSeries -> navigateToSeries(action.seriesId)
                else -> Unit
            }
            viewModel.onAction(action)
        },
    )
}

@Composable
private fun SeasonScreenLayout(state: SeasonState, onAction: (SeasonAction) -> Unit) {
    val safePadding = rememberSafePadding()

    val paddingStart = safePadding.start + MaterialTheme.spacings.default
    val paddingEnd = safePadding.end + MaterialTheme.spacings.default
    val paddingBottom = safePadding.bottom + MaterialTheme.spacings.default

    val lazyListState = rememberLazyListState()

    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val gridColumns = if (isLandscape) state.gridColumnsLandscape else state.gridColumnsPortrait

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
                items(state.episodes.chunked(gridColumns)) { rowEpisodes ->
                    Row(
                        modifier =
                            Modifier.fillMaxWidth().padding(start = paddingStart, end = paddingEnd),
                        horizontalArrangement =
                            Arrangement.spacedBy(MaterialTheme.spacings.small * spacingScale),
                    ) {
                        rowEpisodes.forEach { episode ->
                            EpisodeGridCard(
                                episode = episode,
                                onClick = { onAction(SeasonAction.NavigateToItem(episode)) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        repeat(gridColumns - rowEpisodes.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        } ?: run { CircularProgressIndicator(modifier = Modifier.align(Alignment.Center)) }

        ItemTopBar(
            hasBackButton = true,
            hasHomeButton = true,
            onBackClick = { onAction(SeasonAction.OnBackClick) },
            onHomeClick = { onAction(SeasonAction.OnHomeClick) },
        ) {
            Spacer(modifier = Modifier.width(4.dp))
            state.season?.let { season ->
                Button(
                    onClick = { onAction(SeasonAction.NavigateToSeries(season.seriesId)) },
                    modifier = Modifier.alpha(0.7f),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = Color.Black,
                            contentColor = Color.White,
                        ),
                ) {
                    Text(text = season.seriesName, overflow = TextOverflow.Ellipsis, maxLines = 1)
                }
            }
        }
    }
}

@PreviewScreenSizes
@Composable
private fun SeasonScreenLayoutPreview() {
    FindroidTheme { SeasonScreenLayout(state = SeasonState(season = dummySeason), onAction = {}) }
}
