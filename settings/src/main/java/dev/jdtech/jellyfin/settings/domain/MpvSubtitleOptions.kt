package dev.jdtech.jellyfin.settings.domain

/**
 * Builds mpv `sub-*` options from [AppPreferences].
 *
 * Applied at playback start in `MPVPlayer` (like `hwdec`/`vo`/`ao`). Manual `mpv.conf` entries
 * still take precedence for advanced overrides.
 *
 * The subtitle box is controlled by the border settings: with `sub-border-style=opaque-box` the
 * border colour/size draw a box tightly around each line of text (the "background").
 */
object MpvSubtitleOptions {
    fun yesNo(value: Boolean): String = if (value) "yes" else "no"

    /** Raw option values for `MPVLib.setOptionString`. */
    fun buildOptions(appPreferences: AppPreferences): Map<String, String> {
        val useMargins = yesNo(appPreferences.getValue(appPreferences.playerMpvSubUseMargins))
        val borderEnabled = appPreferences.getValue(appPreferences.playerMpvSubBorderEnabled)
        val borderSize =
            if (borderEnabled) {
                appPreferences.getValue(appPreferences.playerMpvSubBorderSize)
            } else {
                "0"
            }
        return mapOf(
            "sub-font-size" to appPreferences.getValue(appPreferences.playerMpvSubFontSize),
            "sub-color" to appPreferences.getValue(appPreferences.playerMpvSubColor),
            "sub-pos" to appPreferences.getValue(appPreferences.playerMpvSubPosition),
            "sub-use-margins" to useMargins,
            "sub-ass-force-margins" to useMargins,
            "sub-bold" to yesNo(appPreferences.getValue(appPreferences.playerMpvSubBold)),
            "sub-italic" to yesNo(appPreferences.getValue(appPreferences.playerMpvSubItalic)),
            "sub-outline-size" to borderSize,
            "sub-outline-color" to appPreferences.getValue(appPreferences.playerMpvSubBorderColor),
            "sub-back-color" to "#00000000",
            "sub-shadow-offset" to "0",
            "sub-border-style" to appPreferences.getValue(appPreferences.playerMpvSubBorderStyle),
            "sub-ass-override" to appPreferences.getValue(appPreferences.playerMpvSubAssOverride),
        )
    }
}
