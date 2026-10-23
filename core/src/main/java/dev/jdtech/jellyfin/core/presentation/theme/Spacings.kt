package dev.jdtech.jellyfin.core.presentation.theme

import androidx.compose.ui.unit.dp

object Spacings {
    /**
     * The uniform gutter: every screen edge - top, sides, bottom - is inset by this amount, so the
     * header, the content, and the navigation suite share one outer margin.
     */
    val gutter = 16.dp

    /**
     * The tight spacing a shown status bar gets below itself instead of [gutter], which felt too
     * generous right under the bar.
     */
    val statusGutter = 8.dp

    /** The rail's own tighter top spacing on the content's top spine. */
    val railTopGutter = 12.dp
    val default = 24.dp
    val extraSmall = 4.dp
    val small = 8.dp
    val medium = 16.dp
    val large = 32.dp
    val extraLarge = 64.dp
}
