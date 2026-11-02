package dev.hdytrfli.onigiri.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith

// A gentle crossfade for navigation tab switches. This is separate from the pill-tab content,
// which intentionally switches without any transition.
private const val TAB_TRANSITION_FADE_OUT_MS = 180
private const val TAB_TRANSITION_FADE_IN_MS = 240
private const val TAB_TRANSITION_FADE_IN_DELAY_MS = 40

val TAB_TRANSITION_SPEC =
    fadeIn(
        tween(
            durationMillis = TAB_TRANSITION_FADE_IN_MS,
            delayMillis = TAB_TRANSITION_FADE_IN_DELAY_MS,
            easing = FastOutSlowInEasing,
        )
    ) togetherWith
        fadeOut(
            tween(
                durationMillis = TAB_TRANSITION_FADE_OUT_MS,
                easing = FastOutSlowInEasing,
            )
        )
