package dev.jdtech.jellyfin

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.Scene

// Material's fade-through, the motion Mihon uses when switching tabs: the leaving screen fades out
// over the first 30% of the duration while the entering one fades in over the rest, so the two are
// never both half-transparent — which is what made the old cross-fade feel muddy.
private const val TAB_TRANSITION_FADE_OUT_MS = 60
private const val TAB_TRANSITION_FADE_IN_MS = 140

internal val TAB_TRANSITION_SPEC =
    fadeIn(
        tween(
            durationMillis = TAB_TRANSITION_FADE_IN_MS,
            delayMillis = TAB_TRANSITION_FADE_OUT_MS,
        )
    ) togetherWith fadeOut(tween(durationMillis = TAB_TRANSITION_FADE_OUT_MS))

// Pages beyond the current one emerge over it: a quick fade with a slight scale, the same language
// as the activity transition, so the change reads clearly without any large movement. Symmetric,
// so going back looks the same.
private const val PAGE_TRANSITION_DURATION_MS = 220
private const val PAGE_TRANSITION_SCALE = 0.97f

internal val PAGE_TRANSITION_SPEC =
    (fadeIn(tween(durationMillis = PAGE_TRANSITION_DURATION_MS)) +
        scaleIn(
            animationSpec = tween(durationMillis = PAGE_TRANSITION_DURATION_MS),
            initialScale = PAGE_TRANSITION_SCALE,
        )) togetherWith fadeOut(tween(durationMillis = PAGE_TRANSITION_DURATION_MS - 40))

/** True when a transition moves between two top-level tabs rather than onto a detail screen. */
internal fun AnimatedContentTransitionScope<Scene<NavKey>>.isTabSwitch(
    tabRoutes: List<NavKey>
): Boolean {
    // navigation3 keys entries by class, so the scene key is the route's KClass rather than the
    // route instance: compare classes, not values.
    val from = initialState.key
    val to = targetState.key
    return tabRoutes.any { it::class == from } && tabRoutes.any { it::class == to }
}
