package dev.jdtech.jellyfin

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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

// Material's shared-axis X, the motion Mihon uses when pushing to a deeper screen (reversed on
// pop): both screens slide slightly along the horizontal axis while cross-fading, which gives the
// navigation a direction without a full-width slide. The offset is a fraction of the screen rather
// than Material's fixed 30dp so it scales with the display.
private const val SHARED_AXIS_DURATION_MS = 300
private const val SHARED_AXIS_OFFSET_DIVISOR = 20

internal val SHARED_AXIS_FORWARD_SPEC =
    (fadeIn(tween(durationMillis = SHARED_AXIS_DURATION_MS)) +
        slideInHorizontally(tween(durationMillis = SHARED_AXIS_DURATION_MS)) {
            it / SHARED_AXIS_OFFSET_DIVISOR
        }) togetherWith
        (fadeOut(tween(durationMillis = SHARED_AXIS_DURATION_MS)) +
            slideOutHorizontally(tween(durationMillis = SHARED_AXIS_DURATION_MS)) {
                -it / SHARED_AXIS_OFFSET_DIVISOR
            })

internal val SHARED_AXIS_BACKWARD_SPEC =
    (fadeIn(tween(durationMillis = SHARED_AXIS_DURATION_MS)) +
        slideInHorizontally(tween(durationMillis = SHARED_AXIS_DURATION_MS)) {
            -it / SHARED_AXIS_OFFSET_DIVISOR
        }) togetherWith
        (fadeOut(tween(durationMillis = SHARED_AXIS_DURATION_MS)) +
            slideOutHorizontally(tween(durationMillis = SHARED_AXIS_DURATION_MS)) {
                it / SHARED_AXIS_OFFSET_DIVISOR
            })

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
