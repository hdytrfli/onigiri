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
 * One options trigger for the list's display and sorting actions. The entries keep the existing
 * dialogs, so the menu only replaces the two separate toolbar buttons.
 */
@Composable
fun ListOptionsMenu(
    onDisplayInfoClick: () -> Unit,
    onSortClick: () -> Unit,
    modifier: Modifier = Modifier,
    showDisplayInfo: Boolean = true,
    showSort: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        AppIconButton(
            onClick = { expanded = true },
            variant = AppButtonVariant.Secondary,
            content = {
                Icon(
                    painter = painterResource(CoreR.drawable.ic_ellipsis),
                    contentDescription = stringResource(CoreR.string.more_options),
                )
            },
        )

        AppDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            if (showDisplayInfo) {
                MenuActionItem(
                    label = stringResource(CoreR.string.display_info),
                    icon = CoreR.drawable.ic_tv_minimal,
                    onClick = {
                        expanded = false
                        onDisplayInfoClick()
                    },
                )
            }
            if (showSort) {
                MenuActionItem(
                    label = stringResource(CoreR.string.sort_by),
                    icon = CoreR.drawable.ic_arrow_down_up,
                    onClick = {
                        expanded = false
                        onSortClick()
                    },
                )
            }
        }
    }
}
