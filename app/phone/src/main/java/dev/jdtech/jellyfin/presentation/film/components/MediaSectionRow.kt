package dev.jdtech.jellyfin.presentation.film.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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

/**
 * A horizontally scrollable home-style row: the title (with the optional list arrow) and a
 * [LazyRow] of poster cards.
 */
@Composable
fun MediaSectionRow(
    section: MediaSection,
    displayFields: Set<ListDisplayField>,
    onItemClick: (FindroidItem) -> Unit,
    modifier: Modifier = Modifier,
    onListClick: (() -> Unit)? = null,
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = MaterialTheme.spacings.default),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = section.title.asString(),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            if (onListClick != null) {
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
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = MaterialTheme.spacings.default),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small),
        ) {
            items(items = section.items, key = { it.id }) { item ->
                ItemCard(
                    item = item,
                    direction = Direction.VERTICAL,
                    onClick = { onItemClick(item) },
                    showYear = ListDisplayField.Year in displayFields,
                    showCommunityRating = ListDisplayField.CommunityRating in displayFields,
                    showAgeRating = ListDisplayField.AgeRating in displayFields,
                )
            }
        }
    }
}
