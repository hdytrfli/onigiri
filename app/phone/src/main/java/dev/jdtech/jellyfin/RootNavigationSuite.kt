package dev.jdtech.jellyfin

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.window.core.layout.WindowSizeClass
import dev.jdtech.jellyfin.models.TabletUiMode

/** Space a root screen must leave for the [RootNavigationSuite] overlay. */
val LocalSuiteStartInset = compositionLocalOf { 0.dp }
val LocalSuiteBottomInset = compositionLocalOf { 0.dp }

/**
 * Where the navigation suite is drawn. [TabletUiMode] drives it: AUTOMATIC follows the adaptive
 * width rule (rail on expanded widths), ALWAYS and NEVER force the rail and the bottom bar, and
 * LANDSCAPE rails only when the device is turned sideways.
 */
val LocalTabletUiMode = compositionLocalOf { TabletUiMode.AUTOMATIC }

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
fun BoxScope.RootNavigationSuite(
    navigator: Navigator,
    selectedRoute: NavKey,
    suiteType: NavigationSuiteType,
) {
    val items = listOf(homeTab, moviesTab, showsTab, collectionsTab, settingsTab)

    if (suiteType == NavigationSuiteType.NavigationRail) {
        NavigationRail(modifier = Modifier.align(Alignment.CenterStart).fillMaxHeight()) {
            items.forEach { item ->
                NavigationRailItem(
                    selected = selectedRoute == item.route,
                    onClick = { navigator.navigate(item.route) },
                    icon = { Icon(painterResource(item.icon), contentDescription = null) },
                    label = { Text(stringResource(item.title)) },
                    enabled = item.enabled,
                )
            }
        }
    } else {
        NavigationBar(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
            items.forEach { item ->
                NavigationBarItem(
                    selected = selectedRoute == item.route,
                    onClick = { navigator.navigate(item.route) },
                    icon = { Icon(painterResource(item.icon), contentDescription = null) },
                    label = { Text(stringResource(item.title)) },
                    enabled = item.enabled,
                )
            }
        }
    }
}

/** Reserves the suite's space on a root screen, with a constant inset so hiding it never shifts. */
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

/** The height the bottom bar takes from the content on compact layouts. */
fun NavigationSuiteType.suiteBottomInset(): Dp =
    if (this == NavigationSuiteType.NavigationRail) 0.dp else 80.dp
