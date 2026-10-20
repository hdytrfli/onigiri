package dev.jdtech.jellyfin.presentation.film.components

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

@Composable
fun rememberBlurHashPainter(hash: String?, width: Int, height: Int): Painter? {
    val enabled = LocalBlurHash.current
    return remember(hash, width, height, enabled) {
        if (!enabled) return@remember null
        val bitmap = hash?.let { BlurHash.decode(it, width, height) } ?: return@remember null
        BitmapPainter(bitmap.asImageBitmap())
    }
}
