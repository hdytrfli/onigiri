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
        OptionIcon.Subtitle -> CoreR.drawable.ic_case_sensitive
        OptionIcon.Layout -> SettingsR.drawable.ic_layout_dashboard
        OptionIcon.Image -> CoreR.drawable.ic_banner
        OptionIcon.Text -> CoreR.drawable.ic_text_search
        OptionIcon.Playback -> SettingsR.drawable.ic_play
        OptionIcon.Video -> CoreR.drawable.ic_video
        OptionIcon.Clock -> CoreR.drawable.ic_timer
        OptionIcon.Segment -> CoreR.drawable.ic_fast_forward
        OptionIcon.Sort -> CoreR.drawable.ic_arrow_down_up
        OptionIcon.Storage -> SettingsR.drawable.ic_hard_drive
    }
