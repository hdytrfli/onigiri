package dev.jdtech.jellyfin.presentation.film.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.jdtech.jellyfin.film.presentation.collection.CollectionAction
import dev.jdtech.jellyfin.film.presentation.library.ListDisplayField
import dev.jdtech.jellyfin.models.CollectionSection
import dev.jdtech.jellyfin.models.FindroidEpisode
import dev.jdtech.jellyfin.presentation.components.LocalRootPageContentInset
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.presentation.utils.GridSize
import dev.jdtech.jellyfin.presentation.utils.LocalGridGap
import dev.jdtech.jellyfin.presentation.utils.gridColumns

@Composable
fun CollectionGrid(
    sections: List<CollectionSection>,
    innerPadding: PaddingValues,
    onAction: (CollectionAction) -> Unit,
    showSectionHeaders: Boolean = true,
    displayFields: Set<ListDisplayField> = emptySet(),
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(gridColumns(GridSize.POSTER)),
        modifier = Modifier.padding(innerPadding).fillMaxSize(),
        // Under a root page the floating header overlays this grid; standalone the value is 0.
        contentPadding =
            PaddingValues(
                top = LocalRootPageContentInset.current,
                bottom = MaterialTheme.spacings.default,
                start = MaterialTheme.spacings.default,
                end = MaterialTheme.spacings.default,
            ),
        horizontalArrangement = Arrangement.spacedBy(LocalGridGap.current),
        verticalArrangement = Arrangement.spacedBy(LocalGridGap.current),
    ) {
        sections.forEach { section ->
            if (showSectionHeaders) {
                stickyHeader {
                    SectionHeader(
                        title = section.name.asString(),
                        modifier =
                            Modifier.fillMaxWidth()
                                .background(MaterialTheme.colorScheme.background)
                                .padding(horizontal = MaterialTheme.spacings.default),
                    )
                }
            }
            items(items = section.items, key = { it.id }) { item ->
                ItemCard(
                    item = item,
                    direction =
                        if (item is FindroidEpisode) Direction.HORIZONTAL else Direction.VERTICAL,
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
