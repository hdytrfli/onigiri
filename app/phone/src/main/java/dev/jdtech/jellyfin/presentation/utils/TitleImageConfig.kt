package dev.jdtech.jellyfin.presentation.utils

import androidx.compose.runtime.compositionLocalOf

/**
 * Title image sizing (fractions of screen width) and max width:height ratio, applied to detail-page
 * title logos.
 */
data class TitleImageConfig(
    val portraitWidth: Float = 0.7f,
    val landscapeWidth: Float = 0.3f,
    val ratio: Float = 2f,
)

val LocalTitleImageConfig = compositionLocalOf { TitleImageConfig() }
