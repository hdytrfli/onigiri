package dev.hdytrfli.onigiri.feature.film.presentation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.hdytrfli.onigiri.core.R as CoreR
import dev.hdytrfli.onigiri.feature.film.presentation.collection.CollectionAction
import dev.hdytrfli.onigiri.feature.film.presentation.collection.CollectionState
import dev.hdytrfli.onigiri.feature.film.presentation.components.CollectionGrid
import dev.hdytrfli.onigiri.feature.film.presentation.components.EmptyStateOverlay
import dev.hdytrfli.onigiri.feature.film.presentation.downloads.DownloadsViewModel
import dev.hdytrfli.onigiri.models.FindroidItem
import dev.hdytrfli.onigiri.models.mediaSections
import dev.hdytrfli.onigiri.ui.components.AppPullToRefreshBox
import dev.hdytrfli.onigiri.ui.components.LocalRootPageContentInset
import dev.hdytrfli.onigiri.ui.components.OfflineModeReload
import dev.hdytrfli.onigiri.ui.components.PageTopBar
import dev.hdytrfli.onigiri.ui.components.PullToRefreshButton
import dev.hdytrfli.onigiri.ui.components.TopBarPageShell
import dev.hdytrfli.onigiri.ui.preview.dummyMovies
import dev.hdytrfli.onigiri.ui.theme.FindroidTheme
import dev.hdytrfli.onigiri.ui.theme.spacings
import dev.hdytrfli.onigiri.ui.utils.rememberSafePadding

@Composable
fun DownloadsScreen(
    onItemClick: (item: FindroidItem) -> Unit,
    viewModel: DownloadsViewModel = hiltViewModel(),
    pullToRefreshState: PullToRefreshState = rememberPullToRefreshState(),
    showPullIndicator: Boolean = false,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(true) { viewModel.loadItemsIfNeeded() }
    OfflineModeReload { viewModel.loadItems(force = true) }

    TopBarPageShell(
        topBar = {
            PageTopBar(
                title = stringResource(CoreR.string.title_download),
                trailingContent = {
                    PullToRefreshButton(
                        state = pullToRefreshState,
                        isRefreshing = state.isLoading,
                        onRefresh = { viewModel.loadItems(force = true) },
                    )
                },
            )
        }
    ) {
        DownloadsContentLayout(
            state = state,
            onAction = { action ->
                when (action) {
                    is CollectionAction.OnItemClick -> onItemClick(action.item)
                    is CollectionAction.OnBackClick -> Unit
                }
            },
            onRefresh = { viewModel.loadItems(force = true) },
            pullToRefreshState = pullToRefreshState,
            showPullIndicator = showPullIndicator,
        )
    }
}

/** The downloads grid without its page shell, for the Home downloads tab. */
@Composable
fun DownloadsContent(
    onItemClick: (item: FindroidItem) -> Unit,
    viewModel: DownloadsViewModel = hiltViewModel(),
    pullToRefreshState: PullToRefreshState = rememberPullToRefreshState(),
    showPullIndicator: Boolean = false,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(true) { viewModel.loadItemsIfNeeded() }
    OfflineModeReload { viewModel.loadItems(force = true) }

    DownloadsContentLayout(
        state = state,
        onAction = { action ->
            when (action) {
                is CollectionAction.OnItemClick -> onItemClick(action.item)
                is CollectionAction.OnBackClick -> Unit
            }
        },
        onRefresh = { viewModel.loadItems(force = true) },
        pullToRefreshState = pullToRefreshState,
        showPullIndicator = showPullIndicator,
    )
}

@Composable
private fun DownloadsContentLayout(
    state: CollectionState,
    onAction: (CollectionAction) -> Unit,
    onRefresh: () -> Unit = {},
    pullToRefreshState: PullToRefreshState = rememberPullToRefreshState(),
    showPullIndicator: Boolean = false,
) {
    val safePadding = rememberSafePadding()
    val contentPadding =
        PaddingValues(
            start = safePadding.start + MaterialTheme.spacings.gutter,
            top = LocalRootPageContentInset.current,
            end = safePadding.end + MaterialTheme.spacings.gutter,
            bottom = safePadding.bottom + MaterialTheme.spacings.gutter,
        )

    AppPullToRefreshBox(
        isRefreshing = state.isLoading,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
        state = pullToRefreshState,
        showIndicator = showPullIndicator,
    ) {
        EmptyStateOverlay(
            visible = state.sections.isEmpty(),
            loading = state.isLoading,
            context = stringResource(CoreR.string.empty_context_downloads),
            onRefresh = onRefresh,
            title = stringResource(CoreR.string.title_download),
            contentPadding = contentPadding,
        ) {
            CollectionGrid(
                sections = state.sections,
                contentPadding = contentPadding,
                onAction = onAction,
                displayFields = state.displayFields,
            )
        }
    }
}

@PreviewScreenSizes
@Composable
private fun DownloadsContentLayoutPreview() {
    FindroidTheme {
        DownloadsContentLayout(
            state = CollectionState(sections = mediaSections(dummyMovies)),
            onAction = {},
        )
    }
}
