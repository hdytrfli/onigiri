package dev.jdtech.jellyfin.presentation.film.components

import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

/** Width shared by the app's dropdown menus, including the editor's per-tile menu. */
val MenuWidth = 220.dp

/** A single action row in a dropdown menu: the label on the left, its icon trailing. */
@Composable
fun MenuActionItem(icon: Int, label: String, onClick: () -> Unit, enabled: Boolean = true) {
    DropdownMenuItem(
        text = { Text(label) },
        trailingIcon = { Icon(painter = painterResource(icon), contentDescription = null) },
        enabled = enabled,
        onClick = onClick,
    )
}
