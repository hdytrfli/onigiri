package dev.jdtech.jellyfin.core.presentation.theme

import androidx.compose.ui.unit.dp

/** Repeated layout dimensions that have no Material role. */
object Dimens {
    /** Max width of the wide, centred content columns (settings, identify, subtitles). */
    val contentMaxWidth = 640.dp

    /** Max width of the setup/login forms and narrow dialogs. */
    val formMaxWidth = 480.dp

    /** Width of the app banner/logo. */
    val logoWidth = 250.dp

    /** Max height of a full-width dialog card. */
    val dialogMaxHeight = 540.dp
}
