package dev.hdytrfli.onigiri.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.compositionLocalOf

val MaterialTheme.spacings
    get() = Spacings

val LocalSpacings = compositionLocalOf { MaterialTheme.spacings }

/** Multiplier applied to content spacing in detail screens. */
val LocalContentSpacingScale = compositionLocalOf { 1f }
