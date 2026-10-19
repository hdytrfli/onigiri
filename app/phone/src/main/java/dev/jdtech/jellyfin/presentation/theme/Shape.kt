package dev.jdtech.jellyfin.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Every non-button shape in the app: cards, tiles, badges and pop-up surfaces all use the one 10dp
 * radius. Buttons do not read these - they are pills (see
 * [dev.jdtech.jellyfin.presentation.components.AppButton]). `extraLarge` is left at the Material
 * default for the dialog container.
 */
private val Rounded = RoundedCornerShape(10.dp)

val shapes =
    Shapes(
        extraSmall = Rounded,
        small = Rounded,
        medium = Rounded,
        large = Rounded,
    )
