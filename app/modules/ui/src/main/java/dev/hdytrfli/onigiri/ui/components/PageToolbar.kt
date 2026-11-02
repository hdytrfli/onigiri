package dev.hdytrfli.onigiri.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import dev.hdytrfli.onigiri.core.R as CoreR
import dev.hdytrfli.onigiri.ui.theme.spacings

/**
 * A page's toolbar: a segmented set of pill tabs (shadcn's default tabs style) with optional
 * trailing actions, e.g. search / display info / sort on the Movies and Shows pages.
 *
 * [dropdownOnCompact] swaps the pills for a single dropdown on compact widths (phones), where the
 * full pill row does not fit; root pages opt in, secondary pages keep the pills everywhere.
 */
@Composable
fun PageToolbar(
    tabs: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    tabIcons: List<Int>? = null,
    dropdownOnCompact: Boolean = false,
    leadingContent: (@Composable RowScope.() -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = MaterialTheme.spacings.gutter),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingContent != null) {
            AppButtonGroup(content = leadingContent)
            Spacer(modifier = Modifier.width(MaterialTheme.spacings.small))
        }
        if (dropdownOnCompact && isCompactWindow()) {
            TabDropdown(
                tabs = tabs,
                selectedIndex = selectedIndex,
                onSelect = onSelect,
                tabIcons = tabIcons,
            )
        } else {
            // Keep the tabs anchored to the leading edge whether or not a page has trailing
            // actions, so the group does not jump between tabs or between pages.
            PillTabs(
                tabs = tabs,
                selectedIndex = selectedIndex,
                onSelect = onSelect,
            )
        }
        if (actions != null) {
            Spacer(modifier = Modifier.weight(1f))
            AppButtonGroup(content = actions)
        }
    }
}

/** True below the medium width breakpoint - the phone bucket the dropdown replaces the pills in. */
@Composable
private fun isCompactWindow(): Boolean =
    !currentWindowAdaptiveInfo()
        .windowSizeClass
        .isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)

/**
 * The compact-width stand-in for [PillTabs]: one pill-shaped button with the selected tab and a
 * chevron, opening a menu with every tab.
 */
@Composable
private fun TabDropdown(
    tabs: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    tabIcons: List<Int>?,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Row(
            modifier =
                Modifier.clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { expanded = true },
                    )
                    .height(48.dp)
                    .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = tabs.getOrElse(selectedIndex) { "" },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Icon(
                painter = painterResource(CoreR.drawable.ic_chevron_down),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onPrimary,
            )
        }

        AppDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            tabs.forEachIndexed { index, title ->
                MenuActionItem(
                    label = title,
                    icon = tabIcons?.getOrNull(index),
                    selected = index == selectedIndex,
                    onClick = {
                        expanded = false
                        onSelect(index)
                    },
                )
            }
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
    // Bounds of every tab, in the coordinate space of the scrollable content. The single pill below
    // is drawn in that same space, so it follows the tabs while the row is scrolled.
    val tabBounds = remember { mutableStateMapOf<Int, Rect>() }
    val selectedBounds = tabBounds[selectedIndex]
    val density = LocalDensity.current

    Box(
        modifier =
            modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .height(48.dp)
                .padding(4.dp)
                .horizontalScroll(rememberScrollState())
    ) {
        // Drawn first so it sits behind the labels. The selection updates instantly with the tab.
        Box(
            modifier =
                Modifier.offset(x = with(density) { (selectedBounds?.left ?: 0f).toDp() })
                    .width(with(density) { (selectedBounds?.width ?: 0f).toDp() })
                    .fillMaxHeight()
                    .shadow(elevation = 1.dp, shape = CircleShape)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape,
                    )
        )

        Row(
            modifier = Modifier.fillMaxHeight(),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEachIndexed { index, title ->
                val selected = index == selectedIndex
                val contentColor =
                    if (selected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }

                Box(
                    modifier =
                        Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                role = Role.Tab,
                                onClick = { onSelect(index) },
                            )
                            .fillMaxHeight()
                            // Measure before the label padding so the pill spans the whole tab,
                            // not just its text.
                            .onGloballyPositioned { coordinates ->
                                val bounds = coordinates.boundsInParent()
                                if (tabBounds[index] != bounds) {
                                    tabBounds[index] = bounds
                                }
                            }
                            .padding(horizontal = 14.dp),
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
}
