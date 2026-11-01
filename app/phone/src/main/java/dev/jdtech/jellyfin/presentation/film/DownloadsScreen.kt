package dev.jdtech.jellyfin.presentation.film

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.core.presentation.dummy.dummyMovies
import dev.jdtech.jellyfin.film.presentation.collection.CollectionAction
import dev.jdtech.jellyfin.film.presentation.collection.CollectionState
import dev.jdtech.jellyfin.film.presentation.downloads.DownloadsViewModel
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.models.mediaSections
import dev.jdtech.jellyfin.presentation.components.AppPullToRefreshBox
import dev.jdtech.jellyfin.presentation.components.LocalRootPageContentInset
import dev.jdtech.jellyfin.presentation.components.OfflineModeReload
import dev.jdtech.jellyfin.presentation.components.PageScaffold
import dev.jdtech.jellyfin.presentation.film.components.CollectionGrid
import dev.jdtech.jellyfin.presentation.film.components.EmptyState
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.presentation.utils.pageContentWindowInsets
import dev.jdtech.jellyfin.presentation.utils.rememberSafePadding
import dev.jdtech.jellyfin.presentation.utils.topBarWindowInsets

@Composable
fun DownloadsScreen(
    onItemClick: (item: FindroidItem) -> Unit,
    embedded: Boolean = false,
    viewModel: DownloadsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(true) { viewModel.loadItemsIfNeeded() }
    OfflineModeReload { viewModel.loadItems() }

    DownloadsScreenLayout(
        state = state,
        onAction = { action ->
            when (action) {
                is CollectionAction.OnItemClick -> onItemClick(action.item)
                is CollectionAction.OnBackClick -> Unit
            }
        },
        onRefresh = { viewModel.loadItems() },
        embedded = embedded,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DownloadsScreenLayout(
    state: CollectionState,
    onAction: (CollectionAction) -> Unit,
    onRefresh: () -> Unit = {},
    embedded: Boolean = false,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val safePadding = rememberSafePadding()

    PageScaffold(
        scrollBehavior = scrollBehavior,
        topBar = {
            if (!embedded) {
                TopAppBar(
                    title = { Text(text = stringResource(CoreR.string.title_download)) },
                    windowInsets = topBarWindowInsets(),
                    scrollBehavior = scrollBehavior,
                )
            }
        },
        contentWindowInsets =
            if (embedded) {
                WindowInsets(0, 0, 0, 0)
            } else {
                pageContentWindowInsets()
            },
    ) { innerPadding ->
        val contentPadding =
            if (embedded) {
                // Under a root page the floating header overlays the grid and the Scaffold adds
                // no padding, so the scroll inset holds both the header and the safe area.
                PaddingValues(
                    start = safePadding.start + MaterialTheme.spacings.gutter,
                    top = LocalRootPageContentInset.current,
                    end = safePadding.end + MaterialTheme.spacings.gutter,
                    bottom = safePadding.bottom + MaterialTheme.spacings.gutter,
                )
            } else {
                // Standalone, the Scaffold's inner padding already holds the top bar and safe area.
                PaddingValues(all = MaterialTheme.spacings.default)
            }

        AppPullToRefreshBox(
            isRefreshing = state.isLoading,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                CollectionGrid(
                    sections = state.sections,
                    innerPadding = innerPadding,
                    contentPadding = contentPadding,
                    onAction = onAction,
                    displayFields = state.displayFields,
                )

                if (state.sections.isEmpty()) {
                    EmptyState(
                        context = stringResource(CoreR.string.empty_context_downloads),
                        onRefresh = onRefresh,
                    )
                }
            }
        }
    }
}

@PreviewScreenSizes
@Composable
private fun DownloadsScreenLayoutPreview() {
    FindroidTheme {
        DownloadsScreenLayout(
            state = CollectionState(sections = mediaSections(dummyMovies)),
            onAction = {},
        )
    }
}
