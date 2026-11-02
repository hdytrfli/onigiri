package dev.hdytrfli.onigiri.ui.theme

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

    /** Max width of a compact settings dialog (grid layout, sort by). */
    val dialogMaxWidth = 400.dp

    /** Max height of a compact settings dialog (grid layout, sort by). */
    val dialogCompactMaxHeight = 460.dp
}
