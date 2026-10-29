package dev.jdtech.jellyfin.presentation.utils

import androidx.compose.runtime.compositionLocalOf

val LocalOfflineMode = compositionLocalOf<Boolean> { error("Offline mode not defined") }

/**
 * Turns offline mode on or off. The offline notices use it for their "Go online" action, so the
 * user can leave offline mode from the page that is blocked by it.
 */
val LocalSetOfflineMode = compositionLocalOf<(Boolean) -> Unit> { {} }
