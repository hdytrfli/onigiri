package dev.jdtech.jellyfin.presentation.film.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.film.R as FilmR
import dev.jdtech.jellyfin.film.presentation.home.HomeAction
import dev.jdtech.jellyfin.film.presentation.library.ListDisplayField
import dev.jdtech.jellyfin.models.FindroidCollection
import dev.jdtech.jellyfin.models.FindroidImages
import dev.jdtech.jellyfin.models.HomeItem
import dev.jdtech.jellyfin.presentation.components.AppButtonSize
import dev.jdtech.jellyfin.presentation.components.AppButtonVariant
import dev.jdtech.jellyfin.presentation.components.AppIconButton
import dev.jdtech.jellyfin.presentation.utils.GridSize
import dev.jdtech.jellyfin.presentation.utils.gridColumns

@Composable
fun HomeView(
    view: HomeItem.ViewItem,
    itemsPadding: PaddingValues,
    onAction: (HomeAction) -> Unit,
    modifier: Modifier = Modifier,
    displayFields: Set<ListDisplayField> = emptySet(),
) {
    Section(
        title = stringResource(FilmR.string.latest_library, view.view.name),
        modifier = modifier,
        titleModifier = Modifier.padding(itemsPadding),
        trailingContent = {
            AppIconButton(
                onClick = {
                    onAction(
                        HomeAction.OnLibraryClick(
                            FindroidCollection(
                                id = view.view.id,
                                name = view.view.name,
                                images = FindroidImages(),
                                type = view.view.type,
                            )
                        )
                    )
                },
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
        },
    ) {
        GridRows(
            items = view.view.items,
            columns = gridColumns(GridSize.POSTER),
            modifier = Modifier.padding(itemsPadding),
        ) { item ->
            ItemCard(
                item = item,
                direction = Direction.VERTICAL,
                onClick = { onAction(HomeAction.OnItemClick(item)) },
                fillWidth = true,
                showYear = ListDisplayField.Year in displayFields,
                showCommunityRating = ListDisplayField.CommunityRating in displayFields,
                showAgeRating = ListDisplayField.AgeRating in displayFields,
            )
        }
    }
}
