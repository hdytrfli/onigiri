package dev.hdytrfli.onigiri.presentation.settings

import dev.hdytrfli.onigiri.settings.domain.SubtitleFonts

/** State for the "Subtitle font" picker: the available fonts and the current selection. */
data class SubtitleFontState(
    val isLoading: Boolean = true,
    val fonts: List<SubtitleFonts.Font> = emptyList(),
    val selectedFamily: String = SubtitleFonts.DEFAULT_FAMILY,
)
