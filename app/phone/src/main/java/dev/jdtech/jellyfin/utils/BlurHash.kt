package dev.jdtech.jellyfin.utils

import android.graphics.Bitmap
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Decoder for the BlurHash format used by Jellyfin's `ImageBlurHashes`, based on the reference
 * implementation at https://github.com/woltapp/blurhash.
 */
object BlurHash {
    private const val BASE83 =
        "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz#$%*+,-.:;=?@[]^_{|}~"

    /**
     * Decodes [hash] into a [width] x [height] bitmap. Returns `null` when the hash is malformed or
     * the requested size is invalid.
     */
    fun decode(hash: String, width: Int, height: Int): Bitmap? {
        if (width <= 0 || height <= 0 || hash.length < 6) return null

        val sizeFlag = decode83(hash.substring(0, 1)) ?: return null
        val numY = sizeFlag / 9 + 1
        val numX = sizeFlag % 9 + 1

        if (hash.length != 4 + 2 * numX * numY) return null

        val quantisedMaximumValue = decode83(hash.substring(1, 2)) ?: return null
        val maximumValue = (quantisedMaximumValue + 1) / 166f

        val colors = Array(numX * numY) { FloatArray(3) }

        val dc = decode83(hash.substring(2, 6)) ?: return null
        colors[0] =
            floatArrayOf(
                sRGBToLinear(dc shr 16),
                sRGBToLinear((dc shr 8) and 255),
                sRGBToLinear(dc and 255),
            )

        for (i in 1 until colors.size) {
            val value = decode83(hash.substring(4 + i * 2, 6 + i * 2)) ?: return null
            val quantR = value / (19 * 19)
            val quantG = value / 19 % 19
            val quantB = value % 19
            colors[i] =
                floatArrayOf(
                    signPow((quantR - 9) / 9f, 2f) * maximumValue,
                    signPow((quantG - 9) / 9f, 2f) * maximumValue,
                    signPow((quantB - 9) / 9f, 2f) * maximumValue,
                )
        }

        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                var r = 0f
                var g = 0f
                var b = 0f
                for (j in 0 until numY) {
                    for (i in 0 until numX) {
                        val basis =
                            cos(PI * x * i / width).toFloat() * cos(PI * y * j / height).toFloat()
                        val color = colors[i + j * numX]
                        r += color[0] * basis
                        g += color[1] * basis
                        b += color[2] * basis
                    }
                }
                pixels[y * width + x] =
                    (255 shl 24) or
                        (linearToSRGB(r) shl 16) or
                        (linearToSRGB(g) shl 8) or
                        linearToSRGB(b)
            }
        }

        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
            setPixels(pixels, 0, width, 0, 0, width, height)
        }
    }

    private fun decode83(value: String): Int? {
        var result = 0
        for (char in value) {
            val digit = BASE83.indexOf(char)
            if (digit == -1) return null
            result = result * 83 + digit
        }
        return result
    }

    private fun sRGBToLinear(value: Int): Float {
        val v = value / 255f
        return if (v <= 0.04045f) v / 12.92f else ((v + 0.055f) / 1.055f).pow(2.4f)
    }

    private fun linearToSRGB(value: Float): Int {
        val v = value.coerceIn(0f, 1f)
        val s = if (v <= 0.0031308f) v * 12.92f else 1.055f * v.pow(1f / 2.4f) - 0.055f
        return (s * 255f).roundToInt().coerceIn(0, 255)
    }

    private fun signPow(value: Float, exponent: Float): Float {
        val sign = if (value < 0f) -1f else 1f
        return sign * abs(value).pow(exponent)
    }
}
