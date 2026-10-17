package dev.jdtech.jellyfin.presentation.film

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.jdtech.jellyfin.core.presentation.dummy.dummyMovies
import dev.jdtech.jellyfin.film.presentation.collection.CollectionState
import dev.jdtech.jellyfin.film.presentation.favorites.FavoritesViewModel
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.presentation.film.components.Direction
import dev.jdtech.jellyfin.presentation.film.components.HomeHeader
import dev.jdtech.jellyfin.presentation.film.components.ItemCard
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.presentation.utils.GridSize
import dev.jdtech.jellyfin.presentation.utils.LocalGridGap
import dev.jdtech.jellyfin.presentation.utils.gridColumns
import dev.jdtech.jellyfin.presentation.utils.rememberSafePadding

@Composable
fun FavoritesScreen(
    onItemClick: (item: FindroidItem) -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
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
    )
}

@Composable
private fun FavoritesScreenLayout(
    state: CollectionState,
    onItemClick: (item: FindroidItem) -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onRetryClick: () -> Unit,
) {
    val safePadding = rememberSafePadding(handleStartInsets = false)

    val paddingStart = safePadding.start + MaterialTheme.spacings.default
    val paddingTop = safePadding.top + MaterialTheme.spacings.small
    val paddingEnd = safePadding.end + MaterialTheme.spacings.default
    val paddingBottom = safePadding.bottom + MaterialTheme.spacings.default

    val contentPaddingTop = safePadding.top + 88.dp

    Box(modifier = Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(gridColumns(GridSize.POSTER)),
            modifier = Modifier.fillMaxSize(),
            contentPadding =
                PaddingValues(
                    start = paddingStart,
                    top = contentPaddingTop,
                    end = paddingEnd,
                    bottom = paddingBottom,
                ),
            horizontalArrangement = Arrangement.spacedBy(LocalGridGap.current),
            verticalArrangement = Arrangement.spacedBy(LocalGridGap.current),
        ) {
            items(state.items, key = { it.id }) { item ->
                ItemCard(
                    item = item,
                    direction = Direction.VERTICAL,
                    onClick = { onItemClick(item) },
                    modifier = Modifier.animateItem(),
                    fillWidth = true,
                )
            }
        }

        HomeHeader(
            serverName = state.serverName ?: "",
            isLoading = state.isLoading,
            isError = state.error != null,
            onErrorClick = {},
            onRetryClick = onRetryClick,
            onSearchClick = onSearchClick,
            onUserClick = onSettingsClick,
            modifier = Modifier.padding(start = paddingStart, top = paddingTop, end = paddingEnd),
        )
    }
}

@PreviewScreenSizes
@Composable
private fun FavoritesScreenLayoutPreview() {
    FindroidTheme {
        FavoritesScreenLayout(
            state = CollectionState(items = dummyMovies, serverName = "Jellyfin"),
            onItemClick = {},
            onSearchClick = {},
            onSettingsClick = {},
            onRetryClick = {},
        )
    }
}
