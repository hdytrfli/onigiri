package dev.jdtech.jellyfin.presentation.reader

import dev.jdtech.jellyfin.settings.domain.AppPreferences
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.preferences.ColumnCount
import org.readium.r2.navigator.preferences.FontFamily
import org.readium.r2.navigator.preferences.TextAlign
import org.readium.r2.navigator.preferences.Theme
import org.readium.r2.shared.ExperimentalReadiumApi

/** Every reader preference, in the units the reader UI edits them in. */
data class ReaderSettings(
    val theme: String = "dark",
    val fontFamily: String = "",
    val fontSize: Float = 1f,
    val fontWeight: Float = 1f,
    val pageMargins: Float = 1f,
    val scroll: Boolean = false,
    val columnCount: Int = 1,
    val textAlign: String = "",
    val textNormalization: Boolean = true,
    val maxWidth: String = "default",
    val lineHeight: Float = 1.5f,
    val paragraphSpacing: Float = 1.3f,
    val wordSpacing: Float = 0f,
    val letterSpacing: Float = 0f,
    val brightness: Float = 1f,
) {
    /** The Readium CSS max line length, in rem; a huge value removes the cap. */
    val maxLineLengthRem: Double
        get() =
            when (maxWidth) {
                "narrow" -> 32.0
                "wide" -> 60.0
                "none" -> 999.0
                else -> 40.0
            }
}

fun AppPreferences.readerSettings(): ReaderSettings {
    fun float(
        preference: dev.jdtech.jellyfin.settings.domain.models.Preference<String>,
        fallback: Float,
    ) = getValue(preference).toFloatOrNull() ?: fallback

    return ReaderSettings(
        theme = getValue(readerTheme),
        fontFamily = getValue(readerFontFamily).takeIf { it != "default" }.orEmpty(),
        fontSize = float(readerFontSize, 1f),
        fontWeight = float(readerFontWeight, 1f),
        pageMargins = float(readerPageMargins, 1f),
        scroll = getValue(readerMode) == "scroll",
        columnCount = if (getValue(readerMode) == "dual") 2 else 1,
        textAlign = getValue(readerTextAlign).takeIf { it != "default" }.orEmpty(),
        textNormalization = getValue(readerTextNormalization),
        maxWidth = getValue(readerMaxWidth),
        lineHeight = float(readerLineHeight, 1.5f),
        paragraphSpacing = float(readerParagraphSpacing, 1.3f),
        wordSpacing = float(readerWordSpacing, 0f),
        letterSpacing = float(readerLetterSpacing, 0f),
        brightness = float(readerBrightness, 1f),
    )
}

fun AppPreferences.setReaderSettings(settings: ReaderSettings) {
    setValue(readerTheme, settings.theme)
    setValue(readerFontFamily, settings.fontFamily.ifBlank { "default" })
    setValue(readerFontSize, settings.fontSize.toString())
    setValue(readerFontWeight, settings.fontWeight.toString())
    setValue(readerPageMargins, settings.pageMargins.toString())
    setValue(
        readerMode,
        when {
            settings.scroll -> "scroll"
            settings.columnCount >= 2 -> "dual"
            else -> "single"
        },
    )
    setValue(readerTextAlign, settings.textAlign.ifBlank { "default" })
    setValue(readerTextNormalization, settings.textNormalization)
    setValue(readerMaxWidth, settings.maxWidth)
    setValue(readerLineHeight, settings.lineHeight.toString())
    setValue(readerParagraphSpacing, settings.paragraphSpacing.toString())
    setValue(readerWordSpacing, settings.wordSpacing.toString())
    setValue(readerLetterSpacing, settings.letterSpacing.toString())
    setValue(readerBrightness, settings.brightness.toString())
}

/** Maps our reader settings onto Readium's reflowable EPUB preferences. */
@OptIn(ExperimentalReadiumApi::class)
fun ReaderSettings.toEpubPreferences(): EpubPreferences =
    EpubPreferences(
        theme =
            when (theme) {
                "light" -> Theme.LIGHT
                "sepia" -> Theme.SEPIA
                else -> Theme.DARK
            },
        fontFamily = fontFamily.takeIf { it.isNotBlank() }?.let { FontFamily(it) },
        fontSize = fontSize.toDouble(),
        fontWeight = fontWeight.toDouble(),
        pageMargins = pageMargins.toDouble(),
        scroll = scroll,
        columnCount =
            when (columnCount) {
                0 -> ColumnCount.AUTO
                2 -> ColumnCount.TWO
                else -> ColumnCount.ONE
            },
        textAlign =
            when (textAlign) {
                "start" -> TextAlign.START
                "justify" -> TextAlign.JUSTIFY
                else -> null
            },
        textNormalization = textNormalization,
        lineHeight = lineHeight.toDouble(),
        paragraphSpacing = paragraphSpacing.toDouble(),
        wordSpacing = wordSpacing.toDouble(),
        letterSpacing = letterSpacing.toDouble(),
        // The reader's own typography controls only take effect over the publisher styles.
        publisherStyles = false,
    )
