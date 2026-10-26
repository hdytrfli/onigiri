package dev.jdtech.jellyfin.presentation.film.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.jdtech.jellyfin.film.presentation.collection.CollectionAction
import dev.jdtech.jellyfin.film.presentation.library.ListDisplayField
import dev.jdtech.jellyfin.models.CollectionSection
import dev.jdtech.jellyfin.models.FindroidEpisode
import dev.jdtech.jellyfin.presentation.utils.GridSize
import dev.jdtech.jellyfin.presentation.utils.LocalGridGap
import dev.jdtech.jellyfin.presentation.utils.gridColumns

/**
 * A poster grid of titled [sections]. The titles are [section] items, so they scroll with the
 * content like every other section title in the app. [innerPadding] is layout padding around the
 * grid; [contentPadding] is the scroll inset, owned by the caller, so each page decides where the
 * grid starts below the floating header and how far it sits inside the safe area.
 */
@Composable
fun CollectionGrid(
    sections: List<CollectionSection>,
    innerPadding: PaddingValues,
    contentPadding: PaddingValues,
    onAction: (CollectionAction) -> Unit,
    displayFields: Set<ListDisplayField> = emptySet(),
) {
    // asString() is composable but the grid's content lambda is not, so every title
    // is resolved before the grid starts.
    val titles = sections.map { it.name.asString() }

    // One grid serves both tile shapes: a row fits posterColumns posters or thumbnailColumns
    // episode thumbnails, so the grid takes the least common multiple of the two as its column
    // count and every item spans the cells its shape needs.
    val posterColumns = gridColumns(GridSize.POSTER)
    val thumbnailColumns = gridColumns(GridSize.THUMBNAIL)
    val columns = leastCommonMultiple(posterColumns, thumbnailColumns)
    val posterSpan = columns / posterColumns
    val episodeSpan = columns / thumbnailColumns

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = Modifier.padding(innerPadding).fillMaxSize(),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(LocalGridGap.current),
        verticalArrangement = Arrangement.spacedBy(LocalGridGap.current),
    ) {
        sections.forEachIndexed { index, section ->
            section(title = titles[index]) {
                items(
                    items = section.items,
                    key = { it.id },
                    span = {
                        if (it is FindroidEpisode) {
                            GridItemSpan(episodeSpan)
                        } else {
                            GridItemSpan(posterSpan)
                        }
                    },
                ) { item ->
                    ItemCard(
                        item = item,
                        direction =
                            if (item is FindroidEpisode) {
                                Direction.HORIZONTAL
                            } else {
                                Direction.VERTICAL
                            },
                        onClick = { onAction(CollectionAction.OnItemClick(item)) },
                        modifier = Modifier.animateItem(),
                        fillWidth = true,
                        showYear = ListDisplayField.Year in displayFields,
                        showCommunityRating = ListDisplayField.CommunityRating in displayFields,
                        showAgeRating = ListDisplayField.AgeRating in displayFields,
                    )
                }
            }
        }
    }
}

/** The least common multiple of two column counts, merging a poster and a thumbnail grid. */
private fun leastCommonMultiple(a: Int, b: Int): Int {
    fun greatestCommonDivisor(x: Int, y: Int): Int =
        if (y == 0) x else greatestCommonDivisor(y, x % y)

    return a / greatestCommonDivisor(a, b) * b
}
