package dev.hdytrfli.onigiri

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.window.core.layout.WindowSizeClass
import dev.hdytrfli.onigiri.models.TabletUiMode
import dev.hdytrfli.onigiri.ui.theme.spacings
import dev.hdytrfli.onigiri.ui.utils.rememberSafePadding

/** Horizontal space a screen must leave at the start edge for the rail-shaped suite. */
val LocalSuiteStartInset = compositionLocalOf { 0.dp }

/** Bottom space a screen must leave for the bar-shaped suite. */
val LocalSuiteBottomInset = compositionLocalOf { 0.dp }

/**
 * Where the navigation suite is drawn. [TabletUiMode] drives it: AUTOMATIC follows the adaptive
 * width rule (rail on expanded widths), ALWAYS and NEVER force the rail and the bottom bar, and
 * LANDSCAPE rails only when the device is turned sideways.
 */
val LocalTabletUiMode = compositionLocalOf { TabletUiMode.AUTOMATIC }

/**
 * Pages that keep the navigation suite drawn over them: the five tab roots and every
 * settings-related page. Immersive content - item details, the player - hides it, because the bar
 * would only cover material the page already wants edge-to-edge.
 */
fun NavKey.drawsNavigationSuite(tabRoutes: Collection<NavKey>): Boolean =
    this is SettingsRoute ||
        this is SettingsFileEditRoute ||
        this is AboutRoute ||
        this is SubtitleFontRoute ||
        this is ReaderFontRoute ||
        this in tabRoutes

/** The bottom bar puts its items in one row (Play-style) instead of stacking them. */
@Composable
fun rememberWideBottomBar(): Boolean =
    currentWindowAdaptiveInfo()
        .windowSizeClass
        .isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)

/** The rail on tablets, the bottom bar on phones. */
@Composable
fun rememberNavigationSuiteType(): NavigationSuiteType {
    val tabletUiMode = LocalTabletUiMode.current
    val configuration = LocalConfiguration.current
    val windowAdaptiveInfo = currentWindowAdaptiveInfo()
    return with(windowAdaptiveInfo) {
        val preferRail =
            when (tabletUiMode) {
                TabletUiMode.AUTOMATIC ->
                    windowSizeClass.isWidthAtLeastBreakpoint(
                        WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND
                    )
                TabletUiMode.ALWAYS -> true
                TabletUiMode.LANDSCAPE ->
                    configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
                TabletUiMode.NEVER -> false
            }
        if (preferRail) {
            NavigationSuiteType.NavigationRail
        } else {
            // Never consult the adaptive default here: on an expanded-width window it picks a
            // rail anyway, which would silently cancel NEVER and the other non-automatic modes.
            NavigationSuiteType.NavigationBar
        }
    }
}

/**
 * The navigation suite drawn over the content instead of wrapping it. Because it lives outside the
 * NavDisplay it never takes part in a page transition: switching tabs animates only the content and
 * the rail stays perfectly still. Root entries reserve its space with [SuiteInsetBox].
 */
@Composable
fun RootNavigationSuite(
    navigator: Navigator,
    selectedRoute: NavKey,
    suiteType: NavigationSuiteType,
) {
    val items = listOf(homeTab, moviesTab, showsTab, booksTab, settingsTab)
    val tabReselect = LocalTabReselect.current

    // A plain tap navigates; tapping the item of the page already on screen asks that page to do
    // its secondary action (cycle its tabs, open its search).
    fun onClick(item: TabBarItem) {
        if (selectedRoute == item.route) {
            tabReselect.reselect(item.route)
        } else {
            navigator.navigate(item.route)
        }
    }

    if (suiteType == NavigationSuiteType.NavigationRail) {
        // The rail takes no window insets of its own: the first item starts on the content's top
        // spine (status bar + gutter, like the floating header beside it), and every item below is
        // separated by a small gap. Only the top is inset - the rail surface stays full-bleed.
        val safeTop = rememberSafePadding().top
        NavigationRail(
            modifier = Modifier.fillMaxHeight(),
            windowInsets = WindowInsets(0, 0, 0, 0),
        ) {
            items.forEachIndexed { index, item ->
                Spacer(
                    modifier =
                        Modifier.height(
                            if (index == 0) {
                                // The rail's own spine sits tighter than the pages' gutter.
                                safeTop + MaterialTheme.spacings.railTopGutter
                            } else {
                                MaterialTheme.spacings.small
                            }
                        )
                )
                NavigationRailItem(
                    selected = selectedRoute == item.route,
                    onClick = { onClick(item) },
                    icon = { Icon(painterResource(item.icon), contentDescription = null) },
                    label = { Text(stringResource(item.title)) },
                    enabled = item.enabled,
                    // The rail's selection pill follows the secondary button colour, unlike the
                    // toolbar pills which are primary.
                    colors =
                        NavigationRailItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        ),
                )
            }
        }
    } else {
        // Like Google Play on tablets: on wide windows the bottom bar puts the icon and the label
        // in one row inside the selection pill; compact windows keep the stacked phone item.
        val wideBottomBar = rememberWideBottomBar()
        NavigationBar(modifier = Modifier.fillMaxWidth().height(80.dp)) {
            items.forEach { item ->
                if (wideBottomBar) {
                    WideNavigationBarItem(
                        selected = selectedRoute == item.route,
                        onClick = { onClick(item) },
                        iconResource = item.icon,
                        label = stringResource(item.title),
                        enabled = item.enabled,
                    )
                } else {
                    NavigationBarItem(
                        selected = selectedRoute == item.route,
                        onClick = { onClick(item) },
                        icon = { Icon(painterResource(item.icon), contentDescription = null) },
                        label = { Text(stringResource(item.title)) },
                        enabled = item.enabled,
                    )
                }
            }
        }
    }
}

/** Reserves the suite's space so overlaid content never sits under the suite's shape. */
@Composable
fun SuiteInsetBox(content: @Composable () -> Unit) {
    Box(
        modifier =
            Modifier.fillMaxSize()
                .padding(
                    start = LocalSuiteStartInset.current,
                    bottom = LocalSuiteBottomInset.current,
                )
    ) {
        content()
    }
}

/** The width the rail takes from the content on wide layouts. */
fun NavigationSuiteType.suiteStartInset(): Dp =
    if (this == NavigationSuiteType.NavigationRail) 80.dp else 0.dp

/** The height the bottom bar takes from the content: the suite's 80dp default bar. */
fun NavigationSuiteType.suiteBottomInset(): Dp =
    if (this == NavigationSuiteType.NavigationRail) 0.dp else 80.dp

/**
 * A [NavigationBar] item for wide layouts: the icon and the label share one row inside the
 * selection pill on a thinner 64dp bar, like the tablet bottom nav in Google Play. The interaction
 * feedback stays on the pill itself - the plain slot behind it gives no press effect.
 */
@Composable
private fun RowScope.WideNavigationBarItem(
    selected: Boolean,
    onClick: () -> Unit,
    iconResource: Int,
    label: String,
    enabled: Boolean,
) {
    Box(
        modifier =
            Modifier.weight(1f)
                .height(64.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                    enabled = enabled,
                ),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier =
                Modifier.clip(RoundedCornerShape(percent = 50))
                    .background(
                        if (selected) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            Color.Transparent
                        },
                        RoundedCornerShape(percent = 50),
                    )
                    .clickable(onClick = onClick, enabled = enabled)
                    .padding(horizontal = MaterialTheme.spacings.medium, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(iconResource),
                contentDescription = null,
                tint =
                    if (selected) {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
            )
            Spacer(Modifier.width(MaterialTheme.spacings.small))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color =
                    if (selected) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
            )
        }
    }
}
