package dev.hdytrfli.onigiri.feature.film.presentation.components

import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import dev.hdytrfli.onigiri.feature.film.presentation.home.HomeAction
import dev.hdytrfli.onigiri.models.FindroidItem
import dev.hdytrfli.onigiri.ui.preview.dummyMovies
import dev.hdytrfli.onigiri.ui.theme.FindroidTheme
import dev.hdytrfli.onigiri.ui.theme.spacings
import dev.hdytrfli.onigiri.ui.utils.GridSize
import dev.hdytrfli.onigiri.ui.utils.gridColumns
import kotlinx.coroutines.delay

/** Splits the available width into [count] slides, accounting for the page spacing. */
private fun suggestionsPageSize(count: Int) =
    object : PageSize {
        override fun Density.calculateMainAxisPageSize(availableSpace: Int, pageSpacing: Int): Int {
            return (availableSpace - (count - 1) * pageSpacing) / count
        }
    }

@Composable
fun HomeCarousel(
    items: List<FindroidItem>,
    itemsPadding: PaddingValues,
    onAction: (HomeAction) -> Unit,
) {
    val itemCount = items.size.coerceAtLeast(1)
    val suggestionCount = gridColumns(GridSize.SUGGESTIONS).coerceAtLeast(1)

    // An effectively unbounded pager indexed modulo the item count, so swiping and the
    // auto-scroll keep moving forward and wrap last -> first (and back) with no jump.
    val pagerState =
        rememberPagerState(
            initialPage = (Int.MAX_VALUE / 2).let { it - it % itemCount },
            pageCount = { Int.MAX_VALUE },
        )
    val pagerIsDragged by pagerState.interactionSource.collectIsDraggedAsState()

    val autoScrollDelay = 5000L

    if (items.size > 1 && !pagerIsDragged) {
        LaunchedEffect(pagerState) {
            while (true) {
                delay(autoScrollDelay)
                pagerState.animateScrollToPage(pagerState.currentPage + 1)
            }
        }
    }

    HorizontalPager(
        state = pagerState,
        contentPadding = itemsPadding,
        pageSize = remember(suggestionCount) { suggestionsPageSize(suggestionCount) },
        pageSpacing = MaterialTheme.spacings.medium,
    ) { page ->
        items.getOrNull(page % itemCount)?.let { item ->
            HomeCarouselItem(item = item, onAction = onAction)
        }
    }
}

@Composable
@Preview(showBackground = true)
private fun HomeCarouselPreview() {
    FindroidTheme {
        HomeCarousel(
            items = dummyMovies,
            itemsPadding = PaddingValues(horizontal = 0.dp),
            onAction = {},
        )
    }
}
