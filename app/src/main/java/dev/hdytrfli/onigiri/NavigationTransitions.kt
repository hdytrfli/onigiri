package dev.hdytrfli.onigiri

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.Scene

// Pages slide in from 5% of their width to their place with a fade: a single-axis move, no
// scaling, so the content keeps its size and only the horizontal offset changes. Symmetric, so
// going back looks the same.
private const val PAGE_TRANSITION_DURATION_MS = 300
private const val PAGE_TRANSITION_OFFSET_PERCENT = 5

internal val PAGE_TRANSITION_SPEC =
    (fadeIn(tween(durationMillis = PAGE_TRANSITION_DURATION_MS)) +
        slideInHorizontally(tween(durationMillis = PAGE_TRANSITION_DURATION_MS)) {
            it * PAGE_TRANSITION_OFFSET_PERCENT / 100
        }) togetherWith
        (fadeOut(tween(durationMillis = PAGE_TRANSITION_DURATION_MS - 40)) +
            slideOutHorizontally(tween(durationMillis = PAGE_TRANSITION_DURATION_MS)) {
                -it * PAGE_TRANSITION_OFFSET_PERCENT / 100
            })

// The suite never moves between pages, so its appearance is animated instead: it fades out when
// an immersive page takes over (a detail screen, the player) and fades back in on the way home.
private const val SUITE_FADE_DURATION_MS = 200

internal val SUITE_ENTER_TRANSITION = fadeIn(tween(durationMillis = SUITE_FADE_DURATION_MS))
internal val SUITE_EXIT_TRANSITION = fadeOut(tween(durationMillis = SUITE_FADE_DURATION_MS))

// Used when the predictive-back preview is off: the gesture shows nothing until it commits.
internal val NO_PREVIEW_TRANSITION_SPEC = EnterTransition.None togetherWith ExitTransition.None

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
