package dev.jdtech.jellyfin.presentation.film

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.film.presentation.collection.CollectionsViewModel
import dev.jdtech.jellyfin.film.presentation.library.ListDisplayField
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.presentation.components.LocalRootPageContentInset
import dev.jdtech.jellyfin.presentation.components.PageToolbar
import dev.jdtech.jellyfin.presentation.components.RootPage
import dev.jdtech.jellyfin.presentation.film.components.Direction
import dev.jdtech.jellyfin.presentation.film.components.FullPageError
import dev.jdtech.jellyfin.presentation.film.components.ItemCard
import dev.jdtech.jellyfin.presentation.film.components.SectionHeader
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.presentation.utils.GridSize
import dev.jdtech.jellyfin.presentation.utils.LocalGridGap
import dev.jdtech.jellyfin.presentation.utils.gridColumns
import dev.jdtech.jellyfin.presentation.utils.rememberSafePadding

/** The Collections root page, split into the movie and show collections. */
@Composable
fun CollectionsScreen(
    onItemClick: (FindroidItem) -> Unit,
    viewModel: CollectionsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableStateOf(0) }
    val safePadding = rememberSafePadding(handleStartInsets = false)

    LaunchedEffect(true) { viewModel.load() }

    RootPage(
        header = {
            PageToolbar(
                tabs =
                    listOf(
                        stringResource(CoreR.string.movies_label),
                        stringResource(CoreR.string.shows_label),
                    ),
                selectedIndex = selectedTab,
                onSelect = { selectedTab = it },
            )
        }
    ) {
        val items = if (selectedTab == 0) state.movies else state.shows
        if (state.error != null && items.isEmpty()) {
            FullPageError(
                onRefresh = { viewModel.retry() },
                isRefreshing = state.isLoading,
                error = state.error,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(gridColumns(GridSize.POSTER)),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding =
                        PaddingValues(
                            start = safePadding.start + MaterialTheme.spacings.default,
                            top = LocalRootPageContentInset.current,
                            end = safePadding.end + MaterialTheme.spacings.default,
                            bottom = safePadding.bottom + MaterialTheme.spacings.default,
                        ),
                    horizontalArrangement = Arrangement.spacedBy(LocalGridGap.current),
                    verticalArrangement = Arrangement.spacedBy(LocalGridGap.current),
                ) {
                    // The title is content, not a header: it scrolls away with the grid.
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        SectionHeader(title = stringResource(CoreR.string.collections_label))
                    }
                    items(items = items, key = { it.id }) { item ->
                        ItemCard(
                            item = item,
                            direction = Direction.VERTICAL,
                            onClick = { onItemClick(item) },
                            modifier = Modifier.animateItem(),
                            fillWidth = true,
                            showYear = ListDisplayField.Year in state.displayFields,
                            showCommunityRating =
                                ListDisplayField.CommunityRating in state.displayFields,
                            showAgeRating = ListDisplayField.AgeRating in state.displayFields,
                        )
                    }
                }
            }
        }
    }
}
