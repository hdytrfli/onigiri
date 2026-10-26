package dev.jdtech.jellyfin.presentation.film.components

import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import dev.jdtech.jellyfin.presentation.utils.LocalBlurHash
import dev.jdtech.jellyfin.utils.BlurHash

// Decode at the aspect ratio of the image being loaded so the placeholder is never cropped.
internal const val BLUR_HASH_POSTER_WIDTH = 20
internal const val BLUR_HASH_POSTER_HEIGHT = 30
internal const val BLUR_HASH_LANDSCAPE_WIDTH = 32
internal const val BLUR_HASH_LANDSCAPE_HEIGHT = 18

// Blurhash bitmaps are tiny, but decoding one per card on every scroll is wasted work: keep the
// decoded results around (a few KB each).
private val decodedBlurHashes =
    object : LruCache<String, Bitmap>(64) {
        override fun sizeOf(key: String, value: Bitmap): Int = 1
    }

@Composable
fun rememberBlurHashPainter(hash: String?, width: Int, height: Int): Painter? {
    val enabled = LocalBlurHash.current
    return remember(hash, width, height, enabled) {
        if (!enabled || hash == null) return@remember null
        val key = "$hash:$width:$height"
        val bitmap =
            decodedBlurHashes.get(key)
                ?: BlurHash.decode(hash, width, height)?.also { decodedBlurHashes.put(key, it) }
        bitmap?.let { BitmapPainter(it.asImageBitmap()) }
    }
}
