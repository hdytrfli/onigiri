package dev.jdtech.jellyfin

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.ui.Alignment
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

// A swipe reveal: the incoming page is unveiled by a clip edge travelling across it while the
// outgoing page just fades. Neither page's content moves or scales. Pushes reveal from the end,
// pops from the start, so the gesture keeps a direction.
private const val PAGE_TRANSITION_DURATION_MS = 300

internal val PAGE_FORWARD_SPEC =
    (expandHorizontally(
        animationSpec = tween(durationMillis = PAGE_TRANSITION_DURATION_MS),
        expandFrom = Alignment.End,
    ) + fadeIn(tween(durationMillis = PAGE_TRANSITION_DURATION_MS))) togetherWith
        fadeOut(tween(durationMillis = PAGE_TRANSITION_DURATION_MS - 60))

internal val PAGE_BACKWARD_SPEC =
    (expandHorizontally(
        animationSpec = tween(durationMillis = PAGE_TRANSITION_DURATION_MS),
        expandFrom = Alignment.Start,
    ) + fadeIn(tween(durationMillis = PAGE_TRANSITION_DURATION_MS))) togetherWith
        fadeOut(tween(durationMillis = PAGE_TRANSITION_DURATION_MS - 60))

/** True when a transition moves between two top-level tabs rather than onto a deeper screen. */
internal fun AnimatedContentTransitionScope<Scene<NavKey>>.isTabSwitch(
    tabRoutes: List<NavKey>
): Boolean {
    // navigation3 keys entries by class, so the scene key is the route's KClass rather than the
    // route instance: compare classes, not values.
    val from = initialState.key
    val to = targetState.key
    return tabRoutes.any { it::class == from } && tabRoutes.any { it::class == to }
}
