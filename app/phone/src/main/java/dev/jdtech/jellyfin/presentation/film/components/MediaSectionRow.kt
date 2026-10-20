package dev.jdtech.jellyfin.presentation.film.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.film.presentation.library.ListDisplayField
import dev.jdtech.jellyfin.film.presentation.media.MediaSection
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.presentation.components.AppButtonSize
import dev.jdtech.jellyfin.presentation.components.AppButtonVariant
import dev.jdtech.jellyfin.presentation.components.AppIconButton
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.presentation.utils.GridSize
import dev.jdtech.jellyfin.presentation.utils.LocalGridGap
import dev.jdtech.jellyfin.presentation.utils.gridColumns

/**
 * A horizontally scrollable home-style row: the title (with the optional list arrow) and a
 * [LazyRow] of cards. The card width follows the user's poster/thumbnail column setting for the
 * current orientation.
 */
@Composable
fun MediaSectionRow(
    section: MediaSection,
    displayFields: Set<ListDisplayField>,
    onItemClick: (FindroidItem) -> Unit,
    modifier: Modifier = Modifier,
    onListClick: (() -> Unit)? = null,
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val gridSize = if (section.useThumbnails) GridSize.THUMBNAIL else GridSize.POSTER
        val columns = gridColumns(gridSize)
        val gap = LocalGridGap.current
        val horizontalPadding = MaterialTheme.spacings.default
        val cardWidth =
            (maxWidth - horizontalPadding * 2f - gap * (columns - 1).toFloat()) /
                columns.coerceAtLeast(1)

        val trailingAction: (@Composable () -> Unit)? =
            if (onListClick != null) {
                {
                    AppIconButton(
                        onClick = onListClick,
                        size = AppButtonSize.Decorator,
                        variant = AppButtonVariant.Ghost,
                        content = {
                            Icon(
                                painter = painterResource(CoreR.drawable.ic_arrow_up_right),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                            )
                        },
                    )
                }
            } else {
                null
            }
        Section(
            title = section.title.asString(),
            titleModifier = Modifier.padding(horizontal = horizontalPadding),
            trailingContent = trailingAction,
        ) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = horizontalPadding),
                horizontalArrangement = Arrangement.spacedBy(gap),
            ) {
                items(items = section.items, key = { it.id }) { item ->
                    ItemCard(
                        item = item,
                        direction =
                            if (section.useThumbnails) {
                                Direction.HORIZONTAL
                            } else {
                                Direction.VERTICAL
                            },
                        onClick = { onItemClick(item) },
                        modifier = Modifier.width(cardWidth),
                        showRemainingTime = section.showRemainingTime,
                        showYear = ListDisplayField.Year in displayFields,
                        showCommunityRating = ListDisplayField.CommunityRating in displayFields,
                        // Continue Watching already has the time-left badge in the same corner.
                        showAgeRating =
                            ListDisplayField.AgeRating in displayFields &&
                                !section.showRemainingTime,
                    )
                }
            }
        }
    }
}
