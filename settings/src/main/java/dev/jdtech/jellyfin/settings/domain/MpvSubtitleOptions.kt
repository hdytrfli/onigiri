package dev.jdtech.jellyfin.settings.domain

/**
 * Builds mpv `sub-*` options from [AppPreferences].
 *
 * Applied at playback start in `MPVPlayer` (like `hwdec`/`vo`/`ao`). Manual
 * `mpv.conf` entries still take precedence for advanced overrides.
 */
object MpvSubtitleOptions {
    fun yesNo(value: Boolean): String = if (value) "yes" else "no"

    /**
     * Combine an `#RRGGBB` or `#AARRGGBB` color with a 0-255 opacity into mpv
     * `#AARRGGBB` format.
     */
    fun backColorWithAlpha(rgbHex: String, opacity: String): String {
        val alphaInt = opacity.toIntOrNull()?.coerceIn(0, 255) ?: return rgbHex
        val clean = rgbHex.removePrefix("#")
        val rrggbb =
            when (clean.length) {
                6 -> clean
                8 -> clean.substring(2)
                else -> return rgbHex
            }
        val alphaHex = alphaInt.toString(16).padStart(2, '0').uppercase()
        return "#$alphaHex$rrggbb"
    }

    /** Raw option values for `MPVLib.setOptionString`. */
    fun buildOptions(appPreferences: AppPreferences): Map<String, String> {
        val useMargins = yesNo(appPreferences.getValue(appPreferences.playerMpvSubUseMargins))
        val backColor =
            backColorWithAlpha(
                appPreferences.getValue(appPreferences.playerMpvSubBackColor),
                appPreferences.getValue(appPreferences.playerMpvSubBackOpacity),
            )
        return mapOf(
            "sub-font-size" to appPreferences.getValue(appPreferences.playerMpvSubFontSize),
            "sub-color" to appPreferences.getValue(appPreferences.playerMpvSubColor),
            "sub-pos" to appPreferences.getValue(appPreferences.playerMpvSubPosition),
            "sub-use-margins" to useMargins,
            "sub-ass-force-margins" to useMargins,
            "sub-bold" to yesNo(appPreferences.getValue(appPreferences.playerMpvSubBold)),
            "sub-italic" to yesNo(appPreferences.getValue(appPreferences.playerMpvSubItalic)),
            "sub-outline-size" to appPreferences.getValue(appPreferences.playerMpvSubBorderSize),
            "sub-outline-color" to appPreferences.getValue(appPreferences.playerMpvSubBorderColor),
            "sub-back-color" to backColor,
        )
    }
}
