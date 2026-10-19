package dev.jdtech.jellyfin.presentation.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.jdtech.jellyfin.core.R as CoreR

/**
 * One row in a dialog that offers options, styled like the player sheet's track list: an optional
 * leading icon, the label, and a trailing check mark for the current selection.
 *
 * Every option dialog uses this - single select, multi select, sort by and storage - so they all
 * share one row instead of each drawing its own control.
 */
@Composable
fun AppOptionRow(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    @DrawableRes iconDrawableId: Int? = null,
    showCheck: Boolean = true,
    role: Role? = null,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .selectable(selected = selected, role = role, onClick = onClick)
                .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (iconDrawableId != null) {
            Icon(
                painter = painterResource(iconDrawableId),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            )
            Spacer(modifier = Modifier.width(16.dp))
        }

        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )

        if (showCheck) {
            Spacer(modifier = Modifier.width(16.dp))
            Icon(
                painter = painterResource(CoreR.drawable.ic_check),
                contentDescription = null,
                modifier = Modifier.size(24.dp).alpha(if (selected) 1f else 0f),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
