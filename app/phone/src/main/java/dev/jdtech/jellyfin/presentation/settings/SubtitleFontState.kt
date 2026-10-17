package dev.jdtech.jellyfin.presentation.settings

import dev.jdtech.jellyfin.settings.domain.SubtitleFonts

/**
 * State for the "Subtitle font" picker: the available fonts, the selection and the live preview.
 */
data class SubtitleFontState(
    val isLoading: Boolean = true,
    val fonts: List<SubtitleFonts.Font> = emptyList(),
    val selectedFamily: String = SubtitleFonts.DEFAULT_FAMILY,
    val style: SubtitleFontStyle,
)

/**
 * The subtitle styling preferences the preview renders with. [color] and [borderColor] are mpv
 * `#AARRGGBB` strings; [fontSize] and [borderSize] are in mpv's scaled pixels (720 = frame height).
 */
data class SubtitleFontStyle(
    val bold: Boolean,
    val italic: Boolean,
    val color: String,
    val fontSize: String,
    val borderEnabled: Boolean,
    val borderStyle: String,
    val borderColor: String,
    val borderSize: String,
)
