package dev.hdytrfli.onigiri.ui.components

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * The app's pull-to-refresh container. It uses the Material 3 expressive loading indicator, with
 * determinate progress while pulling and the indeterminate morphing state while refreshing.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppPullToRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    state: PullToRefreshState = rememberPullToRefreshState(),
    showIndicator: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier,
        state = state,
        indicator = {
            if (showIndicator) {
                PullToRefreshDefaults.IndicatorBox(
                    state = state,
                    isRefreshing = isRefreshing,
                    modifier = Modifier.align(Alignment.TopCenter),
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ) {
                    if (isRefreshing) {
                        LoadingIndicator(color = MaterialTheme.colorScheme.primary)
                    } else {
                        LoadingIndicator(
                            progress = { state.distanceFraction.coerceIn(0f, 1f) },
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        },
        content = content,
    )
}
