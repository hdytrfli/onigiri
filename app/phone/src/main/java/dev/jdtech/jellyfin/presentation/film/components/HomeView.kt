package dev.jdtech.jellyfin.presentation.film.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.film.R as FilmR
import dev.jdtech.jellyfin.film.presentation.home.HomeAction
import dev.jdtech.jellyfin.models.FindroidCollection
import dev.jdtech.jellyfin.models.FindroidImages
import dev.jdtech.jellyfin.models.HomeItem
import dev.jdtech.jellyfin.presentation.components.AppButtonSize
import dev.jdtech.jellyfin.presentation.components.AppButtonVariant
import dev.jdtech.jellyfin.presentation.components.AppIconButton
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.presentation.utils.GridSize
import dev.jdtech.jellyfin.presentation.utils.gridColumns

@Composable
fun HomeView(
    view: HomeItem.ViewItem,
    itemsPadding: PaddingValues,
    onAction: (HomeAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(
            modifier =
                Modifier.fillMaxWidth().height(AppButtonSize.Default.value).padding(itemsPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(FilmR.string.latest_library, view.view.name),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
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
                variant = AppButtonVariant.Ghost,
                content = {
                    Icon(
                        painter = painterResource(CoreR.drawable.ic_arrow_up_right),
                        contentDescription = null,
                    )
                },
            )
        }
        Spacer(modifier = Modifier.height(MaterialTheme.spacings.extraSmall))
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
            )
        }
    }
}
