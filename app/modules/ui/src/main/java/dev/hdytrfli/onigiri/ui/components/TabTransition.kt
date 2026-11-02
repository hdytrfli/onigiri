package dev.hdytrfli.onigiri.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith

// A gentle fade-through, timed to feel like the page transition: the leaving screen clears first,
// then the entering one fades in with the same easing.
private const val TAB_TRANSITION_FADE_OUT_MS = 120
private const val TAB_TRANSITION_FADE_IN_MS = 300

val TAB_TRANSITION_SPEC =
    fadeIn(
        tween(
            durationMillis = TAB_TRANSITION_FADE_IN_MS,
            delayMillis = TAB_TRANSITION_FADE_OUT_MS,
            easing = FastOutSlowInEasing,
        )
    ) togetherWith
        fadeOut(
            tween(
                durationMillis = TAB_TRANSITION_FADE_OUT_MS,
                easing = FastOutSlowInEasing,
            )
        )
