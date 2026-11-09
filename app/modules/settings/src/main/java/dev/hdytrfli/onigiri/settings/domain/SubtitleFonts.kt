package dev.hdytrfli.onigiri.settings.domain

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import dev.hdytrfli.onigiri.settings.R
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs

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
 *
 * Every bundled font is a *variable* font, which is what makes a weight setting possible: mpv has
 * no weight option, but fontconfig's lookup pattern does carry one, so [weightAliases] hands the
 * player a name per weight that fonts.conf rewrites into "family + weight". The offered weights sit
 * between regular and bold, where every bundled font has a real face to land on.
 */
object SubtitleFonts {

    /** mpv's own default, resolved to Roboto by the `sans-serif` alias written in fonts.conf. */
    const val DEFAULT_FAMILY = "sans-serif"

    /** The weight every font has, and the one mpv uses when nothing else is asked for. */
    const val DEFAULT_WEIGHT = 400

    /**
     * The weights offered, all of which every bundled font carries. The names are the ones
     * fontconfig knows, and also the ones variable fonts use for their named instances, so the
     * aliases written into fonts.conf and fontconfig's own instance lookup land on the same face.
     */
    private val weightSteps =
        listOf(
            400 to "Regular",
            500 to "Medium",
            600 to "SemiBold",
            700 to "Bold",
        )

    /** fontconfig's own name for a weight, as used by the `fonts.conf` aliases. */
    private val fontconfigWeights =
        mapOf(
            500 to "medium",
            600 to "demibold",
            700 to "bold",
        )

    private const val FONTS_DIR_NAME = "fonts"
    private const val ASSET_DIR = "subtitle-fonts"

    /** The name [family] is asked for at [weight], and what fontconfig should make of it. */
    data class WeightAlias(val alias: String, val family: String, val fontconfigWeight: String)

    /** Fonts the user can pick, and the file the family is loaded from. */
    data class Font(
        val family: String,
        val displayName: String,
        val file: File?,
        /** Imported by the user, so it can be removed again. */
        val removable: Boolean = false,
    )

    /** A font we ship: the label we show, and the asset files it is unpacked from. */
    private data class Bundled(val name: String, val assets: List<String>)

    /**
     * Fonts shipped in the APK: one variable upright file per family, so every one of them covers
     * the full weight range. All under the SIL Open Font License, shipped in the `licenses/`
     * directory next to them.
     */
    private val bundled =
        listOf(
            Bundled("Geist", listOf("Geist-Variable.ttf")),
            Bundled("Roboto", listOf("Roboto-Variable.ttf")),
            Bundled("Figtree", listOf("Figtree-Variable.ttf")),
            Bundled("Archivo", listOf("Archivo-Variable.ttf")),
            Bundled("Lexend", listOf("Lexend-Variable.ttf")),
            Bundled("Atkinson Hyperlegible Next", listOf("AtkinsonHyperlegibleNext-Variable.ttf")),
            Bundled("Libre Baskerville", listOf("LibreBaskerville-Variable.ttf")),
            Bundled("Vollkorn", listOf("Vollkorn-Variable.ttf")),
        )

    /**
     * Files earlier versions copied into the mpv font directory. They are deleted on sight, because
     * a static face would win over the variable file that replaced it.
     */
    private val replacedAssets =
        listOf(
            "Geist-Regular.ttf",
            "Geist-Italic.ttf",
            "Geist-Bold.ttf",
            "Geist-BoldItalic.ttf",
            "GeistMono-Regular.ttf",
            "GeistMono-Bold.ttf",
        )

    /** Bundled families dropped from the app; remove their old copies from existing installs. */
    private val removedAssets =
        listOf(
            "GeistMono-Variable.ttf",
            "Inter-Variable.ttf",
            "SourceSans3-Variable.ttf",
            "Montserrat-Variable.ttf",
            "NotoSans-Variable.ttf",
            "WorkSans-Variable.ttf",
            "IBMPlexSans-Variable.ttf",
            "OpenSans-Variable.ttf",
            "DMSans-Variable.ttf",
            "Rubik-Variable.ttf",
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
            if (!file.exists()) continue
            // fontconfig matches on the family the file declares, not on our own label.
            add(Font(familyNameOf(file) ?: font.name, font.name, file))
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

    /**
     * The bundled families with the asset file each is unpacked from, for renderers that read
     * straight from the APK assets (the in-app reader) instead of through fontconfig.
     */
    fun bundledFamilyAssets(context: Context): List<Pair<String, String>> {
        ensureBundled(context)
        val dir = fontsDir(context)
        return bundled.mapNotNull { font ->
            val file = File(dir, font.assets.first())
            if (!file.exists()) return@mapNotNull null
            (familyNameOf(file) ?: font.name) to font.assets.first()
        }
    }

    /** The label shown for [weight]. */
    fun weightLabel(weight: Int): String =
        weightSteps.firstOrNull { it.first == weight }?.second ?: weight.toString()

    /**
     * The `sub-font` value that renders [family] at [weight], see [weightAliases]. The weight is
     * snapped to the nearest one we write an alias for, so a value left over from an earlier
     * version can never ask fontconfig for a name it cannot resolve.
     */
    fun fontOption(family: String, weight: Int): String {
        if (family == DEFAULT_FAMILY) return family
        val nearest = weightSteps.minByOrNull { abs(it.first - weight) } ?: return family
        return if (nearest.first == DEFAULT_WEIGHT) family else alias(family, nearest.first)
    }

    /**
     * The names [fontOption] can ask for, and what fontconfig should make of each. mpv has no
     * weight option, but fontconfig's lookup pattern does carry one, so the player writes these
     * into fonts.conf as "family + weight" rewrites.
     */
    fun weightAliases(context: Context): List<WeightAlias> =
        available(context).flatMap { font ->
            if (font.family == DEFAULT_FAMILY) {
                emptyList()
            } else {
                weightSteps.mapNotNull { (weight, _) ->
                    // Regular is the bare family name, so it needs no alias.
                    val fontconfigWeight = fontconfigWeights[weight] ?: return@mapNotNull null
                    WeightAlias(alias(font.family, weight), font.family, fontconfigWeight)
                }
            }
        }

    private fun alias(family: String, weight: Int): String = "$family ${weightLabel(weight)}"

    /**
     * Copies the bundled fonts into the mpv font directory if they are missing or incomplete, and
     * drops the static files the variable ones replaced.
     */
    fun ensureBundled(context: Context) {
        val dir = fontsDir(context)
        if (!dir.exists() && !dir.mkdirs()) return

        var changed = replacedAssets.count { File(dir, it).delete() } > 0
        changed = removedAssets.count { File(dir, it).delete() } > 0 || changed

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
                    changed = true
                } else {
                    target.delete()
                }
            }
        }

        if (changed) clearFontconfigCache(context)
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

    /** Where a table starts, or null when the font has no table with that tag. */
    private fun tableOffset(raf: RandomAccessFile, tag: Long): Long? {
        val tableDir =
            if (raf.readUInt32(0) == TTC_TAG) {
                // First font of a collection, after the tag, version and font count.
                raf.readUInt32(16)
            } else {
                0L
            }

        return (0 until raf.readUInt16(tableDir + 4)).firstNotNullOfOrNull { index ->
            val record = tableDir + 12 + index * 16L
            if (raf.readUInt32(record) == tag) raf.readUInt32(record + 8) else null
        }
    }

    private fun readFamilyName(raf: RandomAccessFile): String? {
        val nameTable = tableOffset(raf, NAME_TAG) ?: return null

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
