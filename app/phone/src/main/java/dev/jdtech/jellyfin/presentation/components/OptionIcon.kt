package dev.jdtech.jellyfin.presentation.components

import androidx.annotation.DrawableRes
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.settings.R as SettingsR
import dev.jdtech.jellyfin.settings.presentation.models.OptionIcon

/**
 * Resolves the settings model's option icon to the drawable the UI owns. Icons that describe a
 * family of options take the row's option value: the tablet-mode dialog gives each mode its own
 * glyph, while single-semantic icons ignore the value.
 */
@DrawableRes
fun OptionIcon.drawableRes(optionValue: String? = null): Int =
    when (this) {
        OptionIcon.TabletMode ->
            when (optionValue) {
                "auto" -> CoreR.drawable.ic_sparkles
                "always" -> CoreR.drawable.ic_monitor
                "landscape" -> CoreR.drawable.ic_rectangle_horizontal
                "never" -> CoreR.drawable.ic_smartphone
                else -> CoreR.drawable.ic_monitor
            }
        OptionIcon.Audio -> CoreR.drawable.ic_music
        OptionIcon.AudioOutput -> CoreR.drawable.ic_speaker
        OptionIcon.Subtitle -> CoreR.drawable.ic_case_sensitive
        OptionIcon.Grid -> CoreR.drawable.ic_layout_grid
        OptionIcon.Portrait -> CoreR.drawable.ic_rectangle_vertical
        OptionIcon.Landscape -> CoreR.drawable.ic_rectangle_horizontal
        OptionIcon.Ratio -> CoreR.drawable.ic_square
        OptionIcon.Width -> CoreR.drawable.ic_scaling
        OptionIcon.Decode -> CoreR.drawable.ic_cpu
        OptionIcon.Monitor -> CoreR.drawable.ic_monitor
        OptionIcon.Clock -> CoreR.drawable.ic_clock
        OptionIcon.Segment -> CoreR.drawable.ic_fast_forward
        OptionIcon.Sort -> CoreR.drawable.ic_list
        OptionIcon.Storage -> SettingsR.drawable.ic_hard_drive
    }
