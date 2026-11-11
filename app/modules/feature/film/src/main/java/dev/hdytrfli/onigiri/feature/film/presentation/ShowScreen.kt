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
import dev.hdytrfli.onigiri.feature.film.R as FilmR
import dev.hdytrfli.onigiri.feature.film.presentation.components.ActorsRow
import dev.hdytrfli.onigiri.feature.film.presentation.components.DetailAction
import dev.hdytrfli.onigiri.feature.film.presentation.components.DetailActionsMenu
import dev.hdytrfli.onigiri.feature.film.presentation.components.DetailItemRow
import dev.hdytrfli.onigiri.feature.film.presentation.components.Direction
import dev.hdytrfli.onigiri.feature.film.presentation.components.FullPageError
import dev.hdytrfli.onigiri.feature.film.presentation.components.GridRows
import dev.hdytrfli.onigiri.feature.film.presentation.components.InfoText
import dev.hdytrfli.onigiri.feature.film.presentation.components.ItemButtonsBar
import dev.hdytrfli.onigiri.feature.film.presentation.components.ItemCard
import dev.hdytrfli.onigiri.feature.film.presentation.components.ItemHeader
import dev.hdytrfli.onigiri.feature.film.presentation.components.ItemTitle
import dev.hdytrfli.onigiri.feature.film.presentation.components.ItemTopBar
import dev.hdytrfli.onigiri.feature.film.presentation.components.OverviewText
import dev.hdytrfli.onigiri.feature.film.presentation.show.ShowAction
import dev.hdytrfli.onigiri.feature.film.presentation.show.ShowState
import dev.hdytrfli.onigiri.feature.film.presentation.show.ShowViewModel
import dev.hdytrfli.onigiri.models.FindroidItem
import dev.hdytrfli.onigiri.ui.components.AppLoadingIndicator
import dev.hdytrfli.onigiri.ui.components.OfflineModeReload
import dev.hdytrfli.onigiri.ui.components.TopBarPageShell
import dev.hdytrfli.onigiri.ui.preview.dummyShow
import dev.hdytrfli.onigiri.ui.theme.FindroidTheme
import dev.hdytrfli.onigiri.ui.theme.LocalContentSpacingScale
import dev.hdytrfli.onigiri.ui.theme.Yellow
import dev.hdytrfli.onigiri.ui.theme.spacings
import dev.hdytrfli.onigiri.ui.utils.GridSize
import dev.hdytrfli.onigiri.ui.utils.LocalPlaybackLauncher
import dev.hdytrfli.onigiri.ui.utils.gridColumns
import dev.hdytrfli.onigiri.ui.utils.rememberSafePadding
import dev.hdytrfli.onigiri.utils.getShowDateString
import java.util.UUID
import org.jellyfin.sdk.model.api.BaseItemKind

@Composable
fun ShowScreen(
    showId: UUID,
    navigateBack: () -> Unit,
    navigateHome: () -> Unit,
    navigateToItem: (item: FindroidItem) -> Unit,
    navigateToPerson: (personId: UUID) -> Unit,
    navigateToIdentify: (itemId: UUID) -> Unit,
    navigateToEditImages: (itemId: UUID) -> Unit,
    viewModel: ShowViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val playbackLauncher = LocalPlaybackLauncher.current
    val uriHandler = LocalUriHandler.current

    val state by viewModel.state.collectAsStateWithLifecycle()

    // The player runs in its own activity, so returning from playback only resumes this screen.
    // Reload on resume so anything playback changed is reflected.
    LifecycleResumeEffect(Unit) {
        viewModel.loadShow(showId = showId)
        onPauseOrDispose {}
    }
    OfflineModeReload { viewModel.loadShow(showId = showId) }

    ShowScreenLayout(
        state = state,
        onAction = { action ->
            when (action) {
                is ShowAction.Play -> {
                    playbackLauncher.launch(showId, BaseItemKind.SERIES, false)
                }
                is ShowAction.PlayTrailer -> {
                    try {
                        uriHandler.openUri(action.trailer)
                    } catch (e: IllegalArgumentException) {
                        Toast.makeText(context, e.localizedMessage, Toast.LENGTH_SHORT).show()
                    }
                }
                is ShowAction.OnBackClick -> navigateBack()
                is ShowAction.OnHomeClick -> navigateHome()
                is ShowAction.NavigateToItem -> navigateToItem(action.item)
                is ShowAction.NavigateToPerson -> navigateToPerson(action.personId)
                else -> Unit
            }
            viewModel.onAction(action)
        },
        onIdentify = navigateToIdentify,
        onIdentifyApplied = { viewModel.loadShow(showId = showId) },
        onEditImages = navigateToEditImages,
        onRetry = { viewModel.loadShow(showId = showId) },
    )
}

@Composable
private fun ShowScreenLayout(
    state: ShowState,
    onAction: (ShowAction) -> Unit,
    onIdentify: (UUID) -> Unit = {},
    onIdentifyApplied: () -> Unit = {},
    onEditImages: (UUID) -> Unit = {},
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
                onBackClick = { onAction(ShowAction.OnBackClick) },
                trailingContent = {
                    state.show?.let { show ->
                        DetailActionsMenu(
                            itemId = show.id,
                            actions = setOf(DetailAction.IDENTIFY, DetailAction.EDIT_IMAGES),
                            onIdentify = { onIdentify(show.id) },
                            onIdentifyApplied = onIdentifyApplied,
                            onEditImages = { onEditImages(show.id) },
                            onDeleted = { onAction(ShowAction.OnBackClick) },
                        )
                    }
                },
            )
        }
    ) {
        state.show?.let { show ->
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(scrollState)) {
                ItemHeader(
                    item = show,
                    scrollState = scrollState,
                    content = {
                        Column(
                            modifier =
                                Modifier.align(Alignment.BottomStart)
                                    .padding(start = paddingStart, end = paddingEnd)
                        ) {
                            ItemTitle(
                                title = show.name,
                                logoUri = show.images.logo,
                            )
                            show.originalTitle?.let { originalTitle ->
                                if (originalTitle != show.name) {
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
                        Text(
                            text = getShowDateString(show),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text =
                                stringResource(
                                    CoreR.string.runtime_minutes,
                                    show.runtimeTicks.div(600000000),
                                ),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        show.officialRating?.let { officialRating ->
                            Text(text = officialRating, style = MaterialTheme.typography.bodyMedium)
                        }
                        show.communityRating?.let { communityRating ->
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
                    ItemButtonsBar(
                        item = show,
                        onPlayClick = { startFromBeginning ->
                            onAction(ShowAction.Play(startFromBeginning = startFromBeginning))
                        },
                        onMarkAsPlayedClick = {
                            when (show.played) {
                                true -> onAction(ShowAction.UnmarkAsPlayed)
                                false -> onAction(ShowAction.MarkAsPlayed)
                            }
                        },
                        onMarkAsFavoriteClick = {
                            when (show.favorite) {
                                true -> onAction(ShowAction.UnmarkAsFavorite)
                                false -> onAction(ShowAction.MarkAsFavorite)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        canPlay = state.seasons.isNotEmpty(),
                    )
                    Spacer(Modifier.height(MaterialTheme.spacings.medium * spacingScale))
                    OverviewText(text = show.overview, maxCollapsedLines = 3)
                    Spacer(Modifier.height(MaterialTheme.spacings.medium * spacingScale))
                    InfoText(
                        genres = show.genres,
                        director = state.director,
                        writers = state.writers,
                    )
                    Spacer(Modifier.height(MaterialTheme.spacings.medium * spacingScale))
                }

                if (state.seasons.isNotEmpty()) {
                    Column(modifier = Modifier.padding(start = paddingStart, end = paddingEnd)) {
                        Text(
                            text = stringResource(CoreR.string.seasons),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Spacer(Modifier.height(MaterialTheme.spacings.small * spacingScale))
                    }
                    GridRows(
                        items = state.seasons,
                        columns = gridColumns(GridSize.POSTER),
                        modifier = Modifier.padding(start = paddingStart, end = paddingEnd),
                    ) { season ->
                        ItemCard(
                            item = season,
                            direction = Direction.VERTICAL,
                            onClick = { onAction(ShowAction.NavigateToItem(season)) },
                            fillWidth = true,
                        )
                    }
                    Spacer(Modifier.height(MaterialTheme.spacings.medium * spacingScale))
                }

                if (state.actors.isNotEmpty()) {
                    ActorsRow(
                        actors = state.actors,
                        onActorClick = { personId ->
                            onAction(ShowAction.NavigateToPerson(personId))
                        },
                        contentPadding = PaddingValues(start = paddingStart, end = paddingEnd),
                    )
                }
                if (state.showRecommendations && state.collections.isNotEmpty()) {
                    Spacer(Modifier.height(MaterialTheme.spacings.medium * spacingScale))
                    DetailItemRow(
                        title = stringResource(FilmR.string.collections_section),
                        items = state.collections,
                        onItemClick = { onAction(ShowAction.NavigateToItem(it)) },
                        contentPadding = PaddingValues(start = paddingStart, end = paddingEnd),
                    )
                }
                if (state.showRecommendations && state.recommendations.isNotEmpty()) {
                    Spacer(Modifier.height(MaterialTheme.spacings.medium * spacingScale))
                    DetailItemRow(
                        title = stringResource(FilmR.string.more_like_this),
                        items = state.recommendations,
                        onItemClick = { onAction(ShowAction.NavigateToItem(it)) },
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
    FindroidTheme { ShowScreenLayout(state = ShowState(show = dummyShow), onAction = {}) }
}
