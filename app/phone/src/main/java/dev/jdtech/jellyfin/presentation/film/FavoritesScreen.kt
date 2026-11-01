package dev.jdtech.jellyfin.presentation.film

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.core.presentation.dummy.dummyMovies
import dev.jdtech.jellyfin.film.presentation.collection.CollectionAction
import dev.jdtech.jellyfin.film.presentation.collection.CollectionState
import dev.jdtech.jellyfin.film.presentation.favorites.FavoritesViewModel
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.models.mediaSections
import dev.jdtech.jellyfin.presentation.components.AppPullToRefreshBox
import dev.jdtech.jellyfin.presentation.components.LocalRootPageContentInset
import dev.jdtech.jellyfin.presentation.film.components.CollectionGrid
import dev.jdtech.jellyfin.presentation.film.components.EmptyState
import dev.jdtech.jellyfin.presentation.film.components.FullPageError
import dev.jdtech.jellyfin.presentation.film.components.HomeHeader
import dev.jdtech.jellyfin.presentation.film.components.OfflineNotice
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.presentation.utils.LocalOfflineMode
import dev.jdtech.jellyfin.presentation.utils.rememberSafePadding

@Composable
fun FavoritesScreen(
    onItemClick: (item: FindroidItem) -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    embedded: Boolean = false,
    viewModel: FavoritesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(true) { viewModel.loadItems() }

    FavoritesScreenLayout(
        state = state,
        onItemClick = onItemClick,
        onSearchClick = onSearchClick,
        onSettingsClick = onSettingsClick,
        onRetryClick = { viewModel.loadItems() },
        embedded = embedded,
    )
}

@Composable
private fun FavoritesScreenLayout(
    state: CollectionState,
    onItemClick: (item: FindroidItem) -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onRetryClick: () -> Unit,
    embedded: Boolean = false,
) {
    val safePadding = rememberSafePadding()

    val paddingStart = safePadding.start + MaterialTheme.spacings.gutter
    val paddingEnd = safePadding.end + MaterialTheme.spacings.gutter
    val paddingBottom = safePadding.bottom + MaterialTheme.spacings.gutter

    val contentPaddingTop =
        if (embedded) {
            LocalRootPageContentInset.current
        } else {
            safePadding.top + 88.dp
        }

    if (LocalOfflineMode.current) {
        OfflineNotice()
        return
    }

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
        Box(modifier = Modifier.fillMaxSize()) {
            if (state.sections.isEmpty()) {
                EmptyState(text = stringResource(CoreR.string.no_favorites))
            }

            CollectionGrid(
                sections = state.sections,
                innerPadding = PaddingValues(0.dp),
                contentPadding =
                    PaddingValues(
                        start = paddingStart,
                        top = contentPaddingTop,
                        end = paddingEnd,
                        bottom = paddingBottom,
                    ),
                onAction = { action ->
                    if (action is CollectionAction.OnItemClick) {
                        onItemClick(action.item)
                    }
                },
                displayFields = state.displayFields,
            )

            if (!embedded) {
                HomeHeader(
                    serverName = state.serverName ?: "",
                    isLoading = state.isLoading,
                    isError = state.error != null,
                    onErrorClick = {},
                    onRetryClick = onRetryClick,
                    onSearchClick = onSearchClick,
                    onUserClick = onSettingsClick,
                    modifier =
                        Modifier.padding(
                            start = paddingStart,
                            top = safePadding.top + MaterialTheme.spacings.small,
                            end = paddingEnd,
                        ),
                )
            }
        }
    }
}

@PreviewScreenSizes
@Composable
private fun FavoritesScreenLayoutPreview() {
    FindroidTheme {
        FavoritesScreenLayout(
            state = CollectionState(sections = mediaSections(dummyMovies), serverName = "Jellyfin"),
            onItemClick = {},
            onSearchClick = {},
            onSettingsClick = {},
            onRetryClick = {},
        )
    }
}
