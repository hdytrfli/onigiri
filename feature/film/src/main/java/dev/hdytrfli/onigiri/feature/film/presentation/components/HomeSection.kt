package dev.hdytrfli.onigiri.feature.film.presentation.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import dev.hdytrfli.onigiri.core.R as CoreR
import dev.hdytrfli.onigiri.feature.film.presentation.home.HomeAction
import dev.hdytrfli.onigiri.feature.film.presentation.library.ListDisplayField
import dev.hdytrfli.onigiri.models.HomeSection
import dev.hdytrfli.onigiri.ui.components.AppButtonSize
import dev.hdytrfli.onigiri.ui.components.AppButtonVariant
import dev.hdytrfli.onigiri.ui.components.AppIconButton
import dev.hdytrfli.onigiri.ui.utils.GridSize
import dev.hdytrfli.onigiri.ui.utils.gridColumns

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
    onMoreClick: (() -> Unit)? = null,
) {
    Section(
        title = section.name.asString(),
        modifier = modifier,
        titleModifier = Modifier.padding(itemsPadding),
        trailingContent =
            onMoreClick?.let {
                {
                    AppIconButton(
                        onClick = it,
                        size = AppButtonSize.Decorator,
                        variant = AppButtonVariant.Ghost,
                        content = {
                            Icon(
                                painter = painterResource(CoreR.drawable.ic_ellipsis),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                            )
                        },
                    )
                }
            },
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
