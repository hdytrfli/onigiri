package dev.jdtech.jellyfin.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.jdtech.jellyfin.presentation.theme.spacings
import kotlinx.coroutines.launch

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
    ) {
        // Keep the tabs anchored to the leading edge whether or not a page has trailing actions,
        // so the group does not jump between tabs or between pages.
        PillTabs(
            tabs = tabs,
            selectedIndex = selectedIndex,
            onSelect = onSelect,
        )
        if (actions != null) {
            Spacer(modifier = Modifier.weight(1f))
            AppButtonGroup(content = actions)
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

    val pillLeft = remember { Animatable(0f) }
    val pillWidth = remember { Animatable(0f) }
    var hasPositionedPill by remember { mutableStateOf(false) }

    LaunchedEffect(selectedBounds) {
        val bounds = selectedBounds ?: return@LaunchedEffect
        val animationSpec = tween<Float>(durationMillis = 220, easing = FastOutSlowInEasing)
        if (hasPositionedPill) {
            launch { pillLeft.animateTo(bounds.left, animationSpec) }
            launch { pillWidth.animateTo(bounds.width, animationSpec) }
        } else {
            // Tab bounds are only known after the first layout pass: snap to the selection instead
            // of sliding in from the left edge on first composition.
            pillLeft.snapTo(bounds.left)
            pillWidth.snapTo(bounds.width)
            hasPositionedPill = true
        }
    }

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
        // Drawn first so it sits behind the labels. Its offset and width are animated between the
        // previously selected tab and the new one, measured from the tab bounds above.
        Box(
            modifier =
                Modifier.offset(x = with(density) { pillLeft.value.toDp() })
                    .width(with(density) { pillWidth.value.toDp() })
                    .fillMaxHeight()
                    .shadow(elevation = 1.dp, shape = CircleShape)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceBright,
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
                        Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                role = Role.Tab,
                                onClick = { onSelect(index) },
                            )
                            .fillMaxHeight()
                            .padding(horizontal = 14.dp)
                            .onGloballyPositioned { coordinates ->
                                val bounds = coordinates.boundsInParent()
                                if (tabBounds[index] != bounds) {
                                    tabBounds[index] = bounds
                                }
                            },
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
