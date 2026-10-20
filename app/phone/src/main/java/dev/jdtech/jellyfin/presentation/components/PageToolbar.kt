package dev.jdtech.jellyfin.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.jdtech.jellyfin.presentation.theme.spacings

/**
 * A page's toolbar: a segmented set of pill tabs (shadcn's default tabs style) with optional
 * trailing actions, e.g. search / display info / sort on the Movies and Shows pages.
 */
@Composable
fun PageToolbar(
    tabs: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = MaterialTheme.spacings.default),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        PillTabs(
            tabs = tabs,
            selectedIndex = selectedIndex,
            onSelect = onSelect,
            modifier = Modifier.weight(1f, fill = false),
        )
        if (actions != null) {
            Spacer(modifier = Modifier.width(MaterialTheme.spacings.small))
            Row(verticalAlignment = Alignment.CenterVertically, content = actions)
        }
    }
}

@Composable
private fun PillTabs(
    tabs: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(4.dp)
                .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tabs.forEachIndexed { index, title ->
            val selected = index == selectedIndex
            val containerColor by
                animateColorAsState(
                    targetValue =
                        if (selected) {
                            MaterialTheme.colorScheme.surfaceBright
                        } else {
                            Color.Transparent
                        },
                    label = "tab-container",
                )
            val contentColor by
                animateColorAsState(
                    targetValue =
                        if (selected) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    label = "tab-content",
                )

            Box(
                modifier =
                    Modifier.then(
                            if (selected) {
                                Modifier.shadow(elevation = 1.dp, shape = CircleShape)
                            } else {
                                Modifier
                            }
                        )
                        .clip(CircleShape)
                        .background(containerColor)
                        .clickable(role = Role.Tab, onClick = { onSelect(index) })
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
