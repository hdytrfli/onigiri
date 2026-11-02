package dev.hdytrfli.onigiri.ui.utils

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

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
 * The window insets a secondary page pads its Scaffold content with: only the keyboard. Side safe
 * insets are deliberately not reserved - the page gutter is the margin, and on rail layouts the
 * rail (through `SuiteInsetBox`) already takes the start edge - while the bottom stays app-owned
 * (the navigation suite at roots, edge-to-edge content elsewhere).
 *
 * Pages that call `Modifier.recalculateWindowInsets()` reset the consumed insets of their subtree,
 * undoing the app-wide hidden-status-bar consumption (see MainActivity), so they must drop the top
 * inset themselves - exactly as their top bars do with [topBarWindowInsets]. [PageScaffold] applies
 * this automatically.
 */
@Composable
fun pageContentWindowInsets(): WindowInsets = WindowInsets.ime.only(WindowInsetsSides.Bottom)

/**
 * The app's safe spacing: the top and bottom insets only.
 *
 * The display cutout is never reserved on the sides - the app runs edge to edge horizontally, with
 * the gutter as the only margin, on every page and in every orientation. The status bar/cutout at
 * the top is still honoured (unless the status bar is hidden), because the top bars sit there.
 */
@Composable
fun rememberSafePadding(): SafePadding {
    val density = LocalDensity.current

    val safePaddingTop =
        if (LocalHideStatusBar.current) {
            0.dp
        } else {
            with(density) { WindowInsets.safeDrawing.getTop(this).toDp() }
        }
    val safePaddingBottom = with(density) { WindowInsets.safeDrawing.getBottom(this).toDp() }

    return SafePadding(start = 0.dp, top = safePaddingTop, end = 0.dp, bottom = safePaddingBottom)
}
