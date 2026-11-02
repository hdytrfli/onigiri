package dev.hdytrfli.onigiri.ui.utils

import androidx.compose.runtime.compositionLocalOf

/**
 * Whether image placeholders may use the item's blurhash. Off falls back to the flat surface
 * colour, which is what a server without `ImageBlurHashes` gets anyway.
 */
val LocalBlurHash = compositionLocalOf { true }
