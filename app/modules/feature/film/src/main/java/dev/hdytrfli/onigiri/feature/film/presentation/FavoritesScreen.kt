package dev.hdytrfli.onigiri.feature.film.presentation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.hdytrfli.onigiri.core.R as CoreR
import dev.hdytrfli.onigiri.feature.film.presentation.collection.CollectionAction
import dev.hdytrfli.onigiri.feature.film.presentation.collection.CollectionState
import dev.hdytrfli.onigiri.feature.film.presentation.components.CollectionGrid
import dev.hdytrfli.onigiri.feature.film.presentation.components.EmptyStateOverlay
import dev.hdytrfli.onigiri.feature.film.presentation.components.FullPageError
import dev.hdytrfli.onigiri.feature.film.presentation.favorites.FavoritesViewModel
import dev.hdytrfli.onigiri.models.FindroidItem
import dev.hdytrfli.onigiri.models.mediaSections
import dev.hdytrfli.onigiri.ui.components.AppPullToRefreshBox
import dev.hdytrfli.onigiri.ui.components.LocalRootPageContentInset
import dev.hdytrfli.onigiri.ui.components.OfflineModeReload
import dev.hdytrfli.onigiri.ui.preview.dummyMovies
import dev.hdytrfli.onigiri.ui.theme.FindroidTheme
import dev.hdytrfli.onigiri.ui.theme.spacings
import dev.hdytrfli.onigiri.ui.utils.rememberSafePadding

@Composable
fun FavoritesScreen(
    onItemClick: (item: FindroidItem) -> Unit,
    viewModel: FavoritesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(true) { viewModel.loadItemsIfNeeded() }
    OfflineModeReload { viewModel.loadItems(force = true) }

    FavoritesScreenLayout(
        state = state,
        onItemClick = onItemClick,
        onRetryClick = { viewModel.loadItems(force = true) },
    )
}

@Composable
private fun FavoritesScreenLayout(
    state: CollectionState,
    onItemClick: (item: FindroidItem) -> Unit,
    onRetryClick: () -> Unit,
) {
    val safePadding = rememberSafePadding()

    val paddingStart = safePadding.start + MaterialTheme.spacings.gutter
    val paddingEnd = safePadding.end + MaterialTheme.spacings.gutter
    val paddingBottom = safePadding.bottom + MaterialTheme.spacings.gutter
    val contentPadding =
        PaddingValues(
            start = paddingStart,
            top = LocalRootPageContentInset.current,
            end = paddingEnd,
            bottom = paddingBottom,
        )

    if (state.error != null && state.items.isEmpty()) {
        FullPageError(
            onRefresh = onRetryClick,
            isRefreshing = state.isLoading,
            error = state.error,
        )
        return
    }

    AppPullToRefreshBox(
        isRefreshing = state.isLoading,
        onRefresh = onRetryClick,
        modifier = Modifier.fillMaxSize(),
    ) {
        EmptyStateOverlay(
            visible = state.sections.isEmpty(),
            loading = state.isLoading,
            context = stringResource(CoreR.string.empty_context_favorites),
            onRefresh = onRetryClick,
            title = stringResource(CoreR.string.title_favorite),
            contentPadding = contentPadding,
        ) {
            CollectionGrid(
                sections = state.sections,
                innerPadding = PaddingValues(0.dp),
                contentPadding = contentPadding,
                onAction = { action ->
                    if (action is CollectionAction.OnItemClick) {
                        onItemClick(action.item)
                    }
                },
                displayFields = state.displayFields,
            )
        }
    }
}

@PreviewScreenSizes
@Composable
private fun FavoritesScreenLayoutPreview() {
    FindroidTheme {
        FavoritesScreenLayout(
            state = CollectionState(sections = mediaSections(dummyMovies)),
            onItemClick = {},
            onRetryClick = {},
        )
    }
}
