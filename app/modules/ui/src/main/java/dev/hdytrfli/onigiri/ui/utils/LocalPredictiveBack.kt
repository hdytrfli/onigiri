package dev.hdytrfli.onigiri.ui.utils

import androidx.compose.runtime.compositionLocalOf

/** Whether the previous screen may be previewed while swiping back (Android predictive back). */
val LocalPredictiveBack = compositionLocalOf { false }
