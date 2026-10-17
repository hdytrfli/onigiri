package dev.jdtech.jellyfin.presentation.utils

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import dev.jdtech.jellyfin.settings.domain.GridId

/** Minimum number of columns a grid can be configured with. */
const val MIN_GRID_COLUMNS = 1

/** Maximum number of columns a grid can be configured with. */
const val MAX_GRID_COLUMNS = 8

/**
 * Reads and writes the per-[GridId] column counts. It is provided once near the root of the UI, so
 * every grid and its settings dialog share the same values. The default resolves each group's
 * configured defaults, which keeps previews (and any tree without the provider) sensible.
 */
data class GridSettings(
    val columnCount: (gridId: GridId, isLandscape: Boolean) -> Int = { gridId, isLandscape ->
        if (isLandscape) gridId.defaultLandscape else gridId.defaultPortrait
    },
    val onColumnsChange: (gridId: GridId, portrait: Int, landscape: Int) -> Unit = { _, _, _ -> },
)

val LocalGridSettings = compositionLocalOf { GridSettings() }

/** The configured column count for [gridId] in the current orientation. */
@Composable
fun gridColumnsFor(gridId: GridId): Int {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    return LocalGridSettings.current.columnCount(gridId, isLandscape)
}
