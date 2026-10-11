package dev.jdtech.jellyfin.presentation.utils

import androidx.compose.runtime.compositionLocalOf

/** Backdrop aspect ratios (width / height) for portrait and landscape. */
data class BackdropRatios(val portrait: Float = 1.5f, val landscape: Float = 2.3333333f)

val LocalBackdropRatios = compositionLocalOf { BackdropRatios() }
