package dev.jdtech.jellyfin.presentation.film.components

import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import dev.jdtech.jellyfin.core.presentation.dummy.dummyMovies
import dev.jdtech.jellyfin.film.presentation.home.HomeAction
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings
import kotlinx.coroutines.delay

private val dynamicPageSize =
    object : PageSize {
        override fun Density.calculateMainAxisPageSize(availableSpace: Int, pageSpacing: Int): Int {
            val nPages =
                when {
                    availableSpace.toDp() >= 840.dp -> 3
                    availableSpace.toDp() >= 600.dp -> 2
                    else -> 1
                }

            return (availableSpace - (nPages - 1) * pageSpacing) / nPages
        }
    }

@Composable
fun HomeCarousel(
    items: List<FindroidItem>,
    itemsPadding: PaddingValues,
    onAction: (HomeAction) -> Unit,
) {
    val itemCount = items.size.coerceAtLeast(1)

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
        pageSize = dynamicPageSize,
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
