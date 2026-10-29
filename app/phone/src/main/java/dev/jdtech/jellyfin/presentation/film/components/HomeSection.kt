package dev.jdtech.jellyfin.presentation.film.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.jdtech.jellyfin.film.presentation.home.HomeAction
import dev.jdtech.jellyfin.film.presentation.library.ListDisplayField
import dev.jdtech.jellyfin.models.HomeSection
import dev.jdtech.jellyfin.presentation.utils.GridSize
import dev.jdtech.jellyfin.presentation.utils.gridColumns

@Composable
fun HomeSection(
    section: HomeSection,
    itemsPadding: PaddingValues,
    onAction: (HomeAction) -> Unit,
    modifier: Modifier = Modifier,
    showRemainingTime: Boolean = false,
    displayFields: Set<ListDisplayField> = emptySet(),
    gridSize: GridSize = GridSize.THUMBNAIL,
    vertical: Boolean = false,
) {
    Section(
        title = section.name.asString(),
        modifier = modifier,
        titleModifier = Modifier.padding(itemsPadding),
    ) {
        GridRows(
            items = section.items,
            columns = gridColumns(gridSize),
            modifier = Modifier.padding(itemsPadding),
        ) { item ->
            ItemCard(
                item = item,
                direction = if (vertical) Direction.VERTICAL else Direction.HORIZONTAL,
                onClick = { onAction(HomeAction.OnItemClick(item)) },
                fillWidth = true,
                showRemainingTime = showRemainingTime,
                showYear = ListDisplayField.Year in displayFields,
                showCommunityRating = ListDisplayField.CommunityRating in displayFields,
                showAgeRating = ListDisplayField.AgeRating in displayFields,
            )
        }
    }
}
