package dev.jdtech.jellyfin.presentation.theme

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

/**
 * The corner radius every "rounded" surface in the app follows: cards, tiles, badges, thumbnails
 * and the `Rounded` radius of buttons. Named steps only, never a free dp value, so the app cannot
 * drift back into `CircleShape` + `RoundedCornerShape(50)` + `shapes.small` + 12dp.
 *
 * [Pill] flattens rounded rectangles into pills (a rounded-corner shape clamps, which is what makes
 * the pill work for both a 24dp badge and a 48dp button). It leaves buttons whose radius is [Full]
 * unchanged, since those are pills or circles regardless.
 */
enum class CornerRadius(val id: String) {
    Small("small"),
    Default("default"),
    Large("large"),
    Pill("pill");

    val shape: CornerBasedShape
        get() =
            when (this) {
                Small -> RoundedCornerShape(6.dp)
                Default -> RoundedCornerShape(10.dp)
                Large -> RoundedCornerShape(16.dp)
                Pill -> RoundedCornerShape(50)
            }

    companion object {
        /** Resolves a stored preference value, falling back to [Default] on anything unknown. */
        fun fromId(id: String?): CornerRadius = entries.firstOrNull { it.id == id } ?: Default
    }
}

/**
 * The corner radius in force. Provided once in [dev.jdtech.jellyfin.MainActivity] from the user's
 * preference and read by [FindroidTheme] (to build `MaterialTheme.shapes`) and by the button.
 */
val LocalCornerRadius = staticCompositionLocalOf { CornerRadius.Default }
