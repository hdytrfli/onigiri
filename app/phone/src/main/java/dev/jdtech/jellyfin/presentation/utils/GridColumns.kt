package dev.jdtech.jellyfin.presentation.utils

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration

/** The two tile shapes the app's grids use: a 2:3 poster or a 16:9 thumbnail. */
enum class GridSize {
    POSTER,
    THUMBNAIL,
}

/**
 * The configured column counts for each [GridSize] and orientation. Provided once near the root of
 * the UI so every grid reacts to a change; the default matches the app's historical column counts.
 */
data class GridColumns(
    val posterPortrait: Int = 2,
    val posterLandscape: Int = 4,
    val thumbnailPortrait: Int = 3,
    val thumbnailLandscape: Int = 4,
) {
    fun columns(size: GridSize, isLandscape: Boolean): Int =
        when (size) {
            GridSize.POSTER -> if (isLandscape) posterLandscape else posterPortrait
            GridSize.THUMBNAIL -> if (isLandscape) thumbnailLandscape else thumbnailPortrait
        }
}

val LocalGridColumns = compositionLocalOf { GridColumns() }

/** The configured column count for [size] in the current orientation. */
@Composable
fun gridColumns(size: GridSize): Int {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    return LocalGridColumns.current.columns(size, isLandscape)
}
