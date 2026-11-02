package dev.hdytrfli.onigiri.ui.utils

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp

/** The tile shapes the app's grids use, plus the home carousel's suggestions density. */
enum class GridSize {
    POSTER,
    THUMBNAIL,
    SUGGESTIONS,
    PROFILE,
}

/** Base gap between grid cells, scaled by the "Layout grid" gap preference. */
val GridGapBase = 12.dp

/**
 * The gap between grid cells. Provided once near the root of the UI so every grid (poster or
 * thumbnail) uses the same spacing and reacts to the gap preference.
 */
val LocalGridGap = compositionLocalOf { GridGapBase }

/**
 * The configured column counts for each [GridSize] and orientation. Provided once near the root of
 * the UI so every grid reacts to a change; the default matches the app's historical column counts.
 */
data class GridColumns(
    val posterPortrait: Int = 2,
    val posterLandscape: Int = 4,
    val thumbnailPortrait: Int = 3,
    val thumbnailLandscape: Int = 4,
    /** How many suggestions the home carousel shows at once, per orientation. */
    val suggestionPortrait: Int = 1,
    val suggestionLandscape: Int = 3,
    /** How many profile cards fit in one row of the profile picker. */
    val profilePortrait: Int = 4,
    val profileLandscape: Int = 6,
) {
    fun columns(size: GridSize, isLandscape: Boolean): Int =
        when (size) {
            GridSize.POSTER -> if (isLandscape) posterLandscape else posterPortrait
            GridSize.THUMBNAIL -> if (isLandscape) thumbnailLandscape else thumbnailPortrait
            GridSize.SUGGESTIONS -> if (isLandscape) suggestionLandscape else suggestionPortrait
            GridSize.PROFILE -> if (isLandscape) profileLandscape else profilePortrait
        }
}

val LocalGridColumns = compositionLocalOf { GridColumns() }

/** The configured column count for [size] in the current orientation. */
@Composable
fun gridColumns(size: GridSize): Int {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    return LocalGridColumns.current.columns(size, isLandscape)
}
