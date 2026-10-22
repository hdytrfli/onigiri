package dev.jdtech.jellyfin.presentation.utils

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass

data class SafePadding(val start: Dp, val top: Dp, val end: Dp, val bottom: Dp)

/** When true, the status bar is hidden and its top inset must not be reserved. */
val LocalHideStatusBar = compositionLocalOf { false }

/**
 * Insets for a top app bar: the status bar and display cutout, collapsed to zero while the status
 * bar is hidden. Screens that call `recalculateWindowInsets` reset the consumed insets for their
 * subtree, so they must drop the top inset themselves instead of relying on the app-wide consumed
 * insets.
 */
@Composable
fun topBarWindowInsets(): WindowInsets =
    if (LocalHideStatusBar.current) {
        WindowInsets(0, 0, 0, 0)
    } else {
        WindowInsets.statusBars.union(WindowInsets.displayCutout)
    }

/**
 * The window insets a secondary page pads its Scaffold content with. Pages that call
 * `Modifier.recalculateWindowInsets()` reset the consumed insets of their subtree, undoing the
 * app-wide hidden-status-bar consumption (see MainActivity), so they must drop the top inset
 * themselves - exactly as their top bars do with [topBarWindowInsets]. [PageScaffold] applies this
 * automatically.
 */
@Composable
fun pageContentWindowInsets(): WindowInsets =
    if (LocalHideStatusBar.current) {
        WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
    } else {
        ScaffoldDefaults.contentWindowInsets
    }

@Composable
fun rememberSafePadding(handleStartInsets: Boolean = true): SafePadding {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current

    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass

    val safePaddingStart =
        if (handleStartInsets) {
            with(density) { WindowInsets.safeDrawing.getLeft(this, layoutDirection).toDp() }
        } else {
            // Navigation rail handles safe drawing inset in medium and expanded width
            when {
                windowSizeClass.isWidthAtLeastBreakpoint(
                    WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND
                ) -> 0.dp
                windowSizeClass.isWidthAtLeastBreakpoint(
                    WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND
                ) -> 0.dp
                else ->
                    with(density) { WindowInsets.safeDrawing.getLeft(this, layoutDirection).toDp() }
            }
        }

    val safePaddingTop =
        if (LocalHideStatusBar.current) {
            0.dp
        } else {
            with(density) { WindowInsets.safeDrawing.getTop(this).toDp() }
        }
    val safePaddingEnd =
        with(density) { WindowInsets.safeDrawing.getRight(this, layoutDirection).toDp() }
    val safePaddingBottom = with(density) { WindowInsets.safeDrawing.getBottom(this).toDp() }

    return SafePadding(
        start = safePaddingStart,
        top = safePaddingTop,
        end = safePaddingEnd,
        bottom = safePaddingBottom,
    )
}
