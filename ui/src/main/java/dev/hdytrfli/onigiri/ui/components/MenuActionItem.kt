package dev.hdytrfli.onigiri.ui.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

/** Width shared by the app's dropdown menus, including the editor's per-tile menu. */
val MenuWidth = 220.dp

/** The gap between a dropdown's trigger and its menu, so the menu doesn't touch the button. */
private val MenuOffset = DpOffset(x = 0.dp, y = 8.dp)

/**
 * The app's dropdown menu: the shared [MenuWidth] popup, so every menu in the app has the same
 * footprint. Items are [MenuActionItem]s.
 */
@Composable
fun AppDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    alignEnd: Boolean = false,
    anchorWidth: Dp = 48.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier.width(MenuWidth),
        offset =
            if (alignEnd) {
                DpOffset(x = anchorWidth - MenuWidth, y = MenuOffset.y)
            } else {
                MenuOffset
            },
        content = content,
    )
}

/**
 * A single action row in a dropdown menu: the label on the left, its icon trailing (like every
 * other menu in the app). The icon is optional for entries without one (e.g. tab pickers), where
 * [selected] tints the label instead of drawing any check mark.
 */
@Composable
fun MenuActionItem(
    label: String,
    onClick: () -> Unit,
    icon: Int? = null,
    enabled: Boolean = true,
    selected: Boolean = false,
) {
    DropdownMenuItem(
        text = {
            Text(
                text = label,
                color =
                    if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        Color.Unspecified
                    },
            )
        },
        trailingIcon =
            icon?.let { drawableId ->
                { Icon(painter = painterResource(drawableId), contentDescription = null) }
            },
        enabled = enabled,
        onClick = onClick,
    )
}
