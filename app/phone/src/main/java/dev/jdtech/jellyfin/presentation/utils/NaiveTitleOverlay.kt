package dev.jdtech.jellyfin.presentation.utils

import androidx.compose.runtime.compositionLocalOf

/**
 * When true, title logo images get a naive white overlay (tint) to improve visibility. Named
 * "naive" because it can look wrong for titles that are already light or multi-colored.
 */
val LocalNaiveTitleOverlay = compositionLocalOf { false }
