package dev.hdytrfli.onigiri.ui.utils

import androidx.compose.runtime.compositionLocalOf

/** Title image sizing (fractions of screen width), applied to detail-page title logos. */
data class TitleImageConfig(
    val portraitWidth: Float = 0.7f,
    val landscapeWidth: Float = 0.3f,
)

val LocalTitleImageConfig = compositionLocalOf { TitleImageConfig() }
