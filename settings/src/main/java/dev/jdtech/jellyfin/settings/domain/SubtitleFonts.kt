package dev.jdtech.jellyfin.settings.domain

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import dev.jdtech.jellyfin.settings.R
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Subtitle fonts for the mpv player.
 *
 * mpv's `sub-font` option takes a *family name*, which it resolves through fontconfig, so a font is
 * only usable once its file lives in a directory fontconfig scans. Next to the system fonts, libass
 * scans `<mpv config dir>/fonts/`, which is where both the fonts we bundle and the ones the user
 * imports are copied.
 *
 * Fontconfig caches the directories it has scanned, so [clearFontconfigCache] is called whenever a
 * font is added or removed. Without it a newly copied font stays invisible until the OS decides to
 * drop the cache.
 */
object SubtitleFonts {

    /** mpv's own default, resolved to Roboto by the `sans-serif` alias written in fonts.conf. */
    const val DEFAULT_FAMILY = "sans-serif"

    private const val FONTS_DIR_NAME = "fonts"
    private const val ASSET_DIR = "subtitle-fonts"

    /** Fonts the user can pick, and the file used to render their preview. */
    data class Font(
        val family: String,
        val displayName: String,
        val file: File?,
        /** Imported by the user, so it can be removed again. */
        val removable: Boolean = false,
    )

    private data class Bundled(val family: String, val assets: List<String>)

    /** Fonts shipped in the APK, under the SIL Open Font License in the same asset directory. */
    private val bundled =
        listOf(
            Bundled(
                family = "Geist",
                assets =
                    listOf(
                        "Geist-Regular.ttf",
                        "Geist-Italic.ttf",
                        "Geist-Bold.ttf",
                        "Geist-BoldItalic.ttf",
                    ),
            ),
            Bundled(
                family = "Geist Mono",
                assets = listOf("GeistMono-Regular.ttf", "GeistMono-Bold.ttf"),
            ),
        )

    /**
     * Android font files we offer, matched on file name; the family name is then read from the file
     * itself, because the file name is not what fontconfig matches on. Matched as `Prefix-…`,
     * `Prefix.…` or `PrefixVF…` so `NotoSansCJK-Regular.ttc` is not mistaken for `Noto Sans`.
     */
    private val systemPrefixes =
        listOf(
            "Roboto",
            "NotoSans",
            "NotoSerif",
            "DroidSansMono",
            "CutiveMono",
            "DancingScript",
            "MiSans",
        )

    private val systemDir = File("/system/fonts")

    /** The mpv configuration directory, as passed to mpv's `config-dir`. */
    fun configDir(context: Context): File = File(context.filesDir, "mpv")

    /** The directory libass searches for subtitle fonts on top of the system fonts. */
    fun fontsDir(context: Context): File = File(configDir(context), FONTS_DIR_NAME)

    /**
     * Every font the picker should offer: the default, the bundled ones, the ones the device ships
     * and whatever the user imported, without duplicates.
     */
    fun available(context: Context): List<Font> {
        ensureBundled(context)

        val fonts = LinkedHashMap<String, Font>()
        fun add(font: Font) {
            fonts.putIfAbsent(font.family.lowercase(), font)
        }

        add(Font(DEFAULT_FAMILY, context.getString(R.string.subtitle_font_default), file = null))

        val dir = fontsDir(context)
        for (font in bundled) {
            val file = File(dir, font.assets.first())
            if (file.exists()) add(Font(font.family, font.family, file))
        }

        for (font in systemFonts()) add(font)

        dir.listFiles()
            .orEmpty()
            .filter { it.isFontFile() }
            .sortedBy { it.name.lowercase() }
            .forEach { file ->
                val family = familyNameOf(file) ?: return@forEach
                add(Font(family, family, file, removable = true))
            }

        return fonts.values.toList()
    }

    /** Copies the bundled fonts into the mpv font directory if they are missing or incomplete. */
    fun ensureBundled(context: Context) {
        val dir = fontsDir(context)
        if (!dir.exists() && !dir.mkdirs()) return

        var copied = false
        for (font in bundled) {
            for (asset in font.assets) {
                val target = File(dir, asset)
                if (target.exists() && target.length() > 0) continue
                val ok = runCatching {
                    context.assets.open("$ASSET_DIR/$asset").use { input ->
                        target.outputStream().use { output -> input.copyTo(output) }
                    }
                }
                    .isSuccess
                if (ok) {
                    copied = true
                } else {
                    target.delete()
                }
            }
        }

        if (copied) clearFontconfigCache(context)
    }

    /**
     * Copies a picked font into the mpv font directory. Returns null when the file can't be read or
     * doesn't carry a family name, in which case nothing is kept.
     */
    fun importFrom(context: Context, uri: Uri): Font? {
        val dir = fontsDir(context)
        if (!dir.exists() && !dir.mkdirs()) return null

        val name = queryDisplayName(context, uri) ?: DEFAULT_FAMILY
        val target = uniqueFile(dir, sanitise(name))

        val copied = runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            } ?: return null
        }
            .isSuccess
        if (!copied || target.length() == 0L) {
            target.delete()
            return null
        }

        val family = familyNameOf(target)
        if (family == null) {
            target.delete()
            return null
        }

        clearFontconfigCache(context)
        return Font(family, family, target, removable = true)
    }

    fun delete(context: Context, font: Font) {
        if (!font.removable) return
        font.file?.delete()
        clearFontconfigCache(context)
    }

    fun clearFontconfigCache(context: Context) {
        File(context.cacheDir, "fontconfig").deleteRecursively()
    }

    /**
     * Reads the family name out of a font file. Handles TrueType, OpenType and the first font of a
     * TrueType collection; returns null if the file isn't a font after all.
     */
    fun familyNameOf(file: File): String? =
        try {
            RandomAccessFile(file, "r").use(::readFamilyName)
        } catch (e: Exception) {
            null
        }

    private fun readFamilyName(raf: RandomAccessFile): String? {
        val tableDir =
            if (raf.readUInt32(0) == TTC_TAG) {
                // First font of a collection, after the tag, version and font count.
                raf.readUInt32(16)
            } else {
                0L
            }

        val numTables = raf.readUInt16(tableDir + 4)
        val nameTable =
            (0 until numTables).firstNotNullOfOrNull { index ->
                val record = tableDir + 12 + index * 16L
                if (raf.readUInt32(record) == NAME_TAG) raf.readUInt32(record + 8) else null
            } ?: return null

        val count = raf.readUInt16(nameTable + 2)
        val stringOffset = nameTable + raf.readUInt16(nameTable + 4)

        data class Record(
            val platform: Int,
            val encoding: Int,
            val language: Int,
            val nameId: Int,
            val length: Int,
            val offset: Int,
        )

        val records =
            (0 until count).mapNotNull { index ->
                val base = nameTable + 6 + index * 12L
                val buffer = raf.read(base, 12)
                Record(
                    platform = buffer.getShort(0).toInt() and 0xFFFF,
                    encoding = buffer.getShort(2).toInt() and 0xFFFF,
                    language = buffer.getShort(4).toInt() and 0xFFFF,
                    nameId = buffer.getShort(6).toInt() and 0xFFFF,
                    length = buffer.getShort(8).toInt() and 0xFFFF,
                    offset = buffer.getShort(10).toInt() and 0xFFFF,
                )
            }

        fun decode(record: Record): String? {
            if (record.length == 0) return null
            val bytes = raf.read(stringOffset + record.offset, record.length).array()
            val charset =
                if (record.platform == PLATFORM_WINDOWS) Charsets.UTF_16BE else Charsets.ISO_8859_1
            return String(bytes, charset).trim('\u0000', ' ', '\n').takeIf { it.isNotEmpty() }
        }

        // Prefer the typographic family, then any family name, US English first.
        val candidates = records.sortedBy { record ->
            when {
                record.language == LANGUAGE_EN_US && record.platform == PLATFORM_WINDOWS -> 0
                record.platform == PLATFORM_WINDOWS -> 1
                record.platform == PLATFORM_MAC -> 2
                else -> 3
            }
        }

        val typographic = candidates.firstOrNull { it.nameId == NAME_ID_TYPOGRAPHIC_FAMILY }
        val family = candidates.firstOrNull { it.nameId == NAME_ID_FAMILY }
        return (typographic ?: family)?.let(::decode)
    }

    private fun systemFonts(): List<Font> {
        val files = systemDir.listFiles() ?: return emptyList()
        return systemPrefixes.mapNotNull { prefix ->
            val file = files.firstOrNull { it.name.matchesPrefix(prefix) } ?: return@mapNotNull null
            val family = familyNameOf(file) ?: return@mapNotNull null
            Font(family, family, file)
        }
    }

    private fun String.matchesPrefix(prefix: String): Boolean {
        if (!startsWith(prefix) || length == prefix.length) return false
        return this[prefix.length] in charArrayOf('-', '.', '[') || startsWith("${prefix}VF")
    }

    private fun File.isFontFile(): Boolean =
        name.lowercase().let { it.endsWith(".ttf") || it.endsWith(".otf") || it.endsWith(".ttc") }

    private fun queryDisplayName(context: Context, uri: Uri): String? = runCatching {
        context.contentResolver
            .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
    }
        .getOrNull()

    private fun sanitise(name: String): String {
        val cleaned = name.substringBeforeLast('.').replace(Regex("[^A-Za-z0-9 _-]"), "").trim()
        return (cleaned.ifEmpty { "font" }).take(48)
    }

    private fun uniqueFile(dir: File, baseName: String): File {
        var candidate = File(dir, "$baseName.ttf")
        var index = 1
        while (candidate.exists()) {
            candidate = File(dir, "$baseName $index.ttf")
            index++
        }
        return candidate
    }

    private fun RandomAccessFile.read(offset: Long, size: Int): ByteBuffer {
        seek(offset)
        val bytes = ByteArray(size)
        readFully(bytes)
        return ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)
    }

    private fun RandomAccessFile.readUInt16(offset: Long): Int =
        read(offset, 2).short.toInt() and 0xFFFF

    private fun RandomAccessFile.readUInt32(offset: Long): Long =
        read(offset, 4).int.toLong() and 0xFFFFFFFFL

    // Font table tags, kept close to the parser that uses them.
    private const val TTC_TAG = 0x74746366L
    private const val NAME_TAG = 0x6E616D65L
    private const val NAME_ID_FAMILY = 1
    private const val NAME_ID_TYPOGRAPHIC_FAMILY = 16
    private const val PLATFORM_MAC = 1
    private const val PLATFORM_WINDOWS = 3
    private const val LANGUAGE_EN_US = 0x409
}
