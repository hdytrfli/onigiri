package dev.jdtech.jellyfin.presentation.settings

import dev.jdtech.jellyfin.settings.domain.SubtitleFonts

/** State for the "Subtitle font" picker: the available fonts and the current selection. */
data class SubtitleFontState(
    val isLoading: Boolean = true,
    val fonts: List<SubtitleFonts.Font> = emptyList(),
    val selectedFamily: String = SubtitleFonts.DEFAULT_FAMILY,
)
