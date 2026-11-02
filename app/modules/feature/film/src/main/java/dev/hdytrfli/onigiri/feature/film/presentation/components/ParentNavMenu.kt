package dev.hdytrfli.onigiri.feature.film.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import dev.hdytrfli.onigiri.core.R as CoreR
import dev.hdytrfli.onigiri.ui.components.AppButtonVariant
import dev.hdytrfli.onigiri.ui.components.AppDropdownMenu
import dev.hdytrfli.onigiri.ui.components.AppIconButton
import dev.hdytrfli.onigiri.ui.components.MenuActionItem

/**
 * The item pages' shortcut to their parent show and season. A single icon button (using the shows
 * tab's icon) opens a menu of the parents instead of putting their names in the top bar, where a
 * long title would either truncate or push the actions away. The season entry is omitted on the
 * season page itself.
 */
@Composable
fun ParentNavMenu(
    onShowClick: () -> Unit,
    modifier: Modifier = Modifier,
    onSeasonClick: (() -> Unit)? = null,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        AppIconButton(
            onClick = { expanded = true },
            variant = AppButtonVariant.Secondary,
            content = {
                Icon(
                    painter = painterResource(CoreR.drawable.ic_tv),
                    contentDescription = stringResource(CoreR.string.shows_label),
                )
            },
        )
        AppDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            MenuActionItem(
                label = stringResource(CoreR.string.show_detail),
                icon = CoreR.drawable.ic_tv,
                onClick = {
                    expanded = false
                    onShowClick()
                },
            )
            if (onSeasonClick != null) {
                MenuActionItem(
                    label = stringResource(CoreR.string.season_detail),
                    icon = CoreR.drawable.ic_library,
                    onClick = {
                        expanded = false
                        onSeasonClick()
                    },
                )
            }
        }
    }
}
