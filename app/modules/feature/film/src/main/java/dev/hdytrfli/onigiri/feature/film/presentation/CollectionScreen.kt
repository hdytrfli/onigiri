package dev.hdytrfli.onigiri.feature.film.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.hdytrfli.onigiri.core.R as CoreR
import dev.hdytrfli.onigiri.feature.film.presentation.collection.CollectionAction
import dev.hdytrfli.onigiri.feature.film.presentation.collection.CollectionState
import dev.hdytrfli.onigiri.feature.film.presentation.collection.CollectionViewModel
import dev.hdytrfli.onigiri.feature.film.presentation.components.DetailAction
import dev.hdytrfli.onigiri.feature.film.presentation.components.DetailActionsMenu
import dev.hdytrfli.onigiri.feature.film.presentation.components.Direction
import dev.hdytrfli.onigiri.feature.film.presentation.components.EmptyState
import dev.hdytrfli.onigiri.feature.film.presentation.components.GridRows
import dev.hdytrfli.onigiri.feature.film.presentation.components.ItemCard
import dev.hdytrfli.onigiri.feature.film.presentation.components.ItemHeader
import dev.hdytrfli.onigiri.feature.film.presentation.components.ItemTitle
import dev.hdytrfli.onigiri.feature.film.presentation.components.ItemTopBar
import dev.hdytrfli.onigiri.feature.film.presentation.components.OverviewText
import dev.hdytrfli.onigiri.models.FindroidBoxSet
import dev.hdytrfli.onigiri.models.FindroidImages
import dev.hdytrfli.onigiri.models.FindroidItem
import dev.hdytrfli.onigiri.models.FindroidMovie
import dev.hdytrfli.onigiri.models.FindroidShow
import dev.hdytrfli.onigiri.ui.components.AppButtonGroup
import dev.hdytrfli.onigiri.ui.components.AppPullToRefreshBox
import dev.hdytrfli.onigiri.ui.components.OfflineModeReload
import dev.hdytrfli.onigiri.ui.components.PullToRefreshButton
import dev.hdytrfli.onigiri.ui.components.TopBarPageShell
import dev.hdytrfli.onigiri.ui.preview.dummyMovies
import dev.hdytrfli.onigiri.ui.theme.FindroidTheme
import dev.hdytrfli.onigiri.ui.theme.spacings
import dev.hdytrfli.onigiri.ui.utils.GridSize
import dev.hdytrfli.onigiri.ui.utils.gridColumns
import dev.hdytrfli.onigiri.ui.utils.rememberSafePadding
import java.util.UUID

@Composable
fun CollectionScreen(
    collectionId: UUID,
    collectionName: String,
    onItemClick: (item: FindroidItem) -> Unit,
    navigateBack: () -> Unit,
    navigateToEditImages: (itemId: UUID) -> Unit,
    viewModel: CollectionViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(true) { viewModel.loadItems(collectionId) }
    OfflineModeReload { viewModel.loadItems(collectionId, force = true) }

    CollectionDetailLayout(
        state = state,
        onAction = { action ->
            when (action) {
                is CollectionAction.OnItemClick -> onItemClick(action.item)
                is CollectionAction.OnBackClick -> navigateBack()
            }
        },
        onEditImages = navigateToEditImages,
        onRefresh = { viewModel.retry(collectionId) },
    )
}

@Composable
private fun CollectionDetailLayout(
    state: CollectionState,
    onAction: (CollectionAction) -> Unit,
    onEditImages: (UUID) -> Unit = {},
    onRefresh: () -> Unit = {},
    pullToRefreshState: PullToRefreshState = rememberPullToRefreshState(),
) {
    val safePadding = rememberSafePadding()

    val paddingStart = safePadding.start + MaterialTheme.spacings.gutter
    val paddingEnd = safePadding.end + MaterialTheme.spacings.gutter
    val paddingBottom = safePadding.bottom + MaterialTheme.spacings.gutter

    val itemsPadding = PaddingValues(start = paddingStart, end = paddingEnd)
    val scrollState = rememberScrollState()

    // First and last release year across the collection items.
    val yearRange =
        remember(state.items) {
            val years =
                state.items.mapNotNull { item ->
                    when (item) {
                        is FindroidMovie -> item.productionYear
                        is FindroidShow -> item.productionYear
                        else -> null
                    }
                }
            if (years.isEmpty()) {
                null
            } else {
                val min = years.min()
                val max = years.max()
                if (min == max) "$min" else "$min - $max"
            }
        }

    TopBarPageShell(
        topBar = {
            ItemTopBar(
                hasBackButton = true,
                onBackClick = { onAction(CollectionAction.OnBackClick) },
                trailingContent = {
                    AppButtonGroup {
                        PullToRefreshButton(
                            state = pullToRefreshState,
                            isRefreshing = state.isLoading,
                            onRefresh = onRefresh,
                        )
                        state.collection?.let { collection ->
                            DetailActionsMenu(
                                itemId = collection.id,
                                actions = setOf(DetailAction.EDIT_IMAGES),
                                onEditImages = { onEditImages(collection.id) },
                            )
                        }
                    }
                },
            )
        }
    ) {
        AppPullToRefreshBox(
            isRefreshing = state.isLoading,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize(),
            state = pullToRefreshState,
            showIndicator = false,
        ) {
            state.collection
                ?.takeIf { state.items.isNotEmpty() || state.isLoading }
                ?.let { collection ->
                    Column(modifier = Modifier.fillMaxWidth().verticalScroll(scrollState)) {
                        ItemHeader(
                            item = collection,
                            scrollState = scrollState,
                            content = {
                                Column(
                                    modifier =
                                        Modifier.align(Alignment.BottomStart)
                                            .padding(start = paddingStart, end = paddingEnd)
                                ) {
                                    ItemTitle(
                                        title = collection.name,
                                        logoUri = collection.images.logo,
                                    )
                                }
                            },
                        )
                        Column(
                            modifier = Modifier.padding(start = paddingStart, end = paddingEnd)
                        ) {
                            Spacer(Modifier.height(MaterialTheme.spacings.small))
                            Row(
                                horizontalArrangement =
                                    Arrangement.spacedBy(MaterialTheme.spacings.small),
                                verticalAlignment = Alignment.Bottom,
                            ) {
                                Text(
                                    text =
                                        stringResource(CoreR.string.items_count, state.items.size),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                yearRange?.let { range ->
                                    Text(text = range, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                            if (collection.overview.isNotBlank()) {
                                Spacer(Modifier.height(MaterialTheme.spacings.small))
                                OverviewText(text = collection.overview, maxCollapsedLines = 3)
                            }
                            Spacer(Modifier.height(MaterialTheme.spacings.medium))
                        }
                        if (state.items.isNotEmpty()) {
                            GridRows(
                                items = state.items,
                                columns = gridColumns(GridSize.POSTER),
                                modifier = Modifier.padding(itemsPadding),
                            ) { item ->
                                ItemCard(
                                    item = item,
                                    direction = Direction.VERTICAL,
                                    onClick = { onAction(CollectionAction.OnItemClick(item)) },
                                    fillWidth = true,
                                )
                            }
                        }
                        Spacer(Modifier.height(paddingBottom))
                    }
                }

            if (!state.isLoading && state.items.isEmpty()) {
                EmptyState(
                    context = stringResource(CoreR.string.empty_context_items),
                    onRefresh = onRefresh,
                )
            }
        }
    }
}

@PreviewScreenSizes
@Composable
private fun CollectionDetailLayoutPreview() {
    FindroidTheme {
        CollectionDetailLayout(
            state =
                CollectionState(
                    collection =
                        FindroidBoxSet(
                            id = UUID.randomUUID(),
                            name = "Marvel",
                            overview = "A collection of Marvel movies.",
                            images = FindroidImages(),
                        ),
                    items = dummyMovies,
                ),
            onAction = {},
        )
    }
}
