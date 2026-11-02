package dev.hdytrfli.onigiri.ui.utils

import androidx.compose.runtime.compositionLocalOf

/**
 * Backdrop aspect ratios (width / height): portraits, landscapes, and the player's sheet header.
 */
data class BackdropRatios(
    val portrait: Float = 1.5f,
    val landscape: Float = 2.3333333f,
    val playerSheet: Float = 1.3333334f,
)

val LocalBackdropRatios = compositionLocalOf { BackdropRatios() }
