package dev.hdytrfli.onigiri.feature.film.presentation.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.hdytrfli.onigiri.core.R as CoreR
import dev.hdytrfli.onigiri.feature.film.R as FilmR
import dev.hdytrfli.onigiri.feature.film.presentation.home.HomeAction
import dev.hdytrfli.onigiri.feature.film.presentation.library.ListDisplayField
import dev.hdytrfli.onigiri.models.FindroidCollection
import dev.hdytrfli.onigiri.models.FindroidImages
import dev.hdytrfli.onigiri.models.HomeItem
import dev.hdytrfli.onigiri.ui.components.AppButtonSize
import dev.hdytrfli.onigiri.ui.components.AppButtonVariant
import dev.hdytrfli.onigiri.ui.components.AppIconButton
import dev.hdytrfli.onigiri.ui.utils.GridSize
import dev.hdytrfli.onigiri.ui.utils.gridColumns

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
