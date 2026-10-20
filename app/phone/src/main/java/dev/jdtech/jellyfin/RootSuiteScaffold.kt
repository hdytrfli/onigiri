package dev.jdtech.jellyfin

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.NavKey
import androidx.window.core.layout.WindowSizeClass
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.presentation.utils.LocalHideStatusBar

/**
 * The navigation suite (rail on tablets, bottom bar on phones) wrapped around a root screen, the
 * way Mihon structures it: the suite belongs to the root page, so a deeper screen is composed
 * outside it and is full width from its first frame — it never grows into the space the rail leaves
 * behind.
 */
@Composable
fun RootSuiteScaffold(
    navigator: Navigator,
    selectedRoute: NavKey,
    content: @Composable () -> Unit,
) {
    val navigationItems = listOf(homeTab, moviesTab, showsTab, collectionsTab, settingsTab)

    val windowAdaptiveInfo = currentWindowAdaptiveInfo()
    val navigationSuiteType =
        with(windowAdaptiveInfo) {
            if (
                windowSizeClass.isWidthAtLeastBreakpoint(
                    WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND
                )
            ) {
                NavigationSuiteType.NavigationRail
            } else {
                NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(this)
            }
        }
    val isRail = navigationSuiteType == NavigationSuiteType.NavigationRail

    // Consume the hidden status-bar/cutout top inset above the suite, so the rail does not reserve
    // space when the status bar is hidden.
    val navSuiteInsets =
        if (LocalHideStatusBar.current) {
            WindowInsets.safeDrawing.only(WindowInsetsSides.Top)
        } else {
            WindowInsets(0, 0, 0, 0)
        }
    Box(modifier = Modifier.fillMaxSize().consumeWindowInsets(navSuiteInsets)) {
        NavigationSuiteScaffold(
            navigationSuiteItems = {
                navigationItems.forEachIndexed { index, item ->
                    item(
                        selected = selectedRoute == item.route,
                        onClick = { navigator.navigate(item.route) },
                        icon = {
                            Icon(
                                painter = painterResource(item.icon),
                                contentDescription = stringResource(item.title),
                            )
                        },
                        modifier =
                            if (isRail) {
                                // Match the top bar's top padding on the rail (first item only, so
                                // the items keep their normal spacing).
                                if (index == 0) Modifier.padding(top = MaterialTheme.spacings.small)
                                else Modifier
                            } else {
                                Modifier.padding(vertical = MaterialTheme.spacings.small)
                            },
                        enabled = item.enabled,
                        label = { Text(text = stringResource(item.title)) },
                    )
                }
            },
            layoutType = navigationSuiteType,
        ) {
            content()
        }
    }
}
