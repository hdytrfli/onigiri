package dev.jdtech.jellyfin.presentation.film.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.film.presentation.home.HomeAction
import dev.jdtech.jellyfin.film.presentation.library.ListDisplayField
import dev.jdtech.jellyfin.models.HomeSection
import dev.jdtech.jellyfin.presentation.components.AppButtonSize
import dev.jdtech.jellyfin.presentation.components.AppButtonVariant
import dev.jdtech.jellyfin.presentation.components.AppIconButton
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
