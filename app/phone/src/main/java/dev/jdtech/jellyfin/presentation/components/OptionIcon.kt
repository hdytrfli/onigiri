package dev.jdtech.jellyfin.presentation.components

import androidx.annotation.DrawableRes
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.settings.R as SettingsR
import dev.jdtech.jellyfin.settings.presentation.models.OptionIcon

/** Resolves the settings model's option icon to the drawable the UI owns. */
@DrawableRes
fun OptionIcon.drawableRes(): Int =
    when (this) {
        OptionIcon.Audio -> CoreR.drawable.ic_music
        OptionIcon.AudioOutput -> CoreR.drawable.ic_speaker
        OptionIcon.Subtitle -> CoreR.drawable.ic_case_sensitive
        OptionIcon.Grid -> CoreR.drawable.ic_layout_grid
        OptionIcon.Portrait -> CoreR.drawable.ic_rectangle_vertical
        OptionIcon.Landscape -> CoreR.drawable.ic_rectangle_horizontal
        OptionIcon.Image -> CoreR.drawable.ic_image
        OptionIcon.Width -> CoreR.drawable.ic_scaling
        OptionIcon.Decode -> CoreR.drawable.ic_cpu
        OptionIcon.Monitor -> CoreR.drawable.ic_monitor
        OptionIcon.Clock -> CoreR.drawable.ic_clock
        OptionIcon.Segment -> CoreR.drawable.ic_fast_forward
        OptionIcon.Sort -> CoreR.drawable.ic_arrow_down_up
        OptionIcon.Storage -> SettingsR.drawable.ic_hard_drive
    }
