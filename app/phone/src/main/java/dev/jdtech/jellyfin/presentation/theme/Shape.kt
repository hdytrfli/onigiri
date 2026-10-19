package dev.jdtech.jellyfin.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import dev.jdtech.jellyfin.core.presentation.theme.Radius

/**
 * Every non-button shape in the app: cards, tiles, badges, dialog containers and pop-up surfaces
 * all use [Radius.rounded]. Buttons do not read these - they are pills (see
 * [dev.jdtech.jellyfin.presentation.components.AppButton]).
 */
private val Rounded = RoundedCornerShape(Radius.rounded)

val shapes =
    Shapes(
        extraSmall = Rounded,
        small = Rounded,
        medium = Rounded,
        large = Rounded,
        extraLarge = Rounded,
    )
