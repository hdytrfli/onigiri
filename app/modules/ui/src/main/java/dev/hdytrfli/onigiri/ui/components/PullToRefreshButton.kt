package dev.hdytrfli.onigiri.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.hdytrfli.onigiri.core.R as CoreR
import kotlinx.coroutines.delay

/**
 * The refresh action used beside a page's list options. It is hidden at rest, grows out of the
 * toolbar while the content is pulled down, and becomes the loading state while refreshing.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PullToRefreshButton(
    state: PullToRefreshState,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    holdAfterRefreshMs: Long = 800L,
) {
    val pullFraction = state.distanceFraction.coerceIn(0f, 1f)
    // Keep the completed refresh visible briefly, like mpvex's pull-refresh behavior.
    var showRefreshState by remember { mutableStateOf(isRefreshing) }
    LaunchedEffect(isRefreshing, holdAfterRefreshMs) {
        if (isRefreshing) {
            showRefreshState = true
        } else if (showRefreshState) {
            delay(holdAfterRefreshMs)
            showRefreshState = false
        }
    }
    val visible = showRefreshState || pullFraction > 0.01f
    val visibilityState = remember { MutableTransitionState(false) }
    visibilityState.targetState = visible
    val isExiting = visibilityState.currentState && !visibilityState.targetState
    val contentScale by
        animateFloatAsState(
            targetValue = if (showRefreshState) 1f else pullFraction,
            animationSpec =
                tween(
                    durationMillis = 220,
                    easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f),
                ),
            label = "refresh-button-scale",
        )
    val showIndeterminate = showRefreshState || isExiting
    val refreshLabel = stringResource(CoreR.string.refresh)

    AnimatedVisibility(
        visibleState = visibilityState,
        modifier = modifier,
        enter = fadeIn(tween(120)) + expandHorizontally(tween(180)),
        exit = shrinkHorizontally(tween(durationMillis = 160, delayMillis = 220)),
    ) {
        AppIconButton(
            onClick = onRefresh,
            variant = AppButtonVariant.Ghost,
            modifier =
                Modifier.graphicsLayer {
                        scaleX = contentScale
                        scaleY = contentScale
                    }
                    .semantics { contentDescription = refreshLabel },
            content = {
                if (showIndeterminate) {
                    LoadingIndicator(
                        modifier = Modifier.size(40.dp),
                        color = LocalContentColor.current,
                    )
                } else {
                    LoadingIndicator(
                        progress = { pullFraction },
                        modifier = Modifier.size(40.dp),
                        color = LocalContentColor.current,
                    )
                }
            },
        )
    }
}
