package dev.jdtech.jellyfin.presentation.theme

import androidx.compose.material3.Shapes

/**
 * The theme's shapes all read the one corner radius so every card, tile, badge and control follows
 * the Interface setting. `extraLarge` is left alone: it is the dialog container, which is excluded.
 */
fun appShapes(cornerRadius: CornerRadius): Shapes {
    val shape = cornerRadius.shape
    return Shapes(extraSmall = shape, small = shape, medium = shape, large = shape)
}
