package dev.jdtech.jellyfin.presentation.film.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.jdtech.jellyfin.R
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.presentation.utils.LocalOfflineMode

@Composable
fun HomeHeader(
    serverName: String,
    isLoading: Boolean,
    isError: Boolean,
    onErrorClick: () -> Unit,
    onRetryClick: () -> Unit,
    onSearchClick: () -> Unit,
    onUserClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isOfflineMode = LocalOfflineMode.current

    Row(
        modifier = modifier.fillMaxWidth().height(56.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Surface(
            modifier = Modifier.fillMaxHeight().weight(1f, fill = false),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = MaterialTheme.spacings.medium),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = serverName,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.animateContentSize(),
                )
            }
        }

        Spacer(Modifier.width(MaterialTheme.spacings.medium))

        Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.medium)) {
            AnimatedVisibility(visible = isError, enter = fadeIn(), exit = fadeOut()) {
                Surface(
                    onClick = onErrorClick,
                    modifier = Modifier.fillMaxHeight().aspectRatio(1f),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.errorContainer,
                ) {
                    Box {
                        Icon(
                            painter = painterResource(CoreR.drawable.ic_alert_circle),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }
                }
            }

            AnimatedVisibility(visible = isLoading || isError, enter = fadeIn(), exit = fadeOut()) {
                Surface(
                    onClick = onRetryClick,
                    modifier = Modifier.fillMaxHeight().aspectRatio(1f),
                    enabled = !isLoading,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    Box {
                        if (isLoading) {
                            val rotation =
                                rememberInfiniteTransition(label = "loading")
                                    .animateFloat(
                                        initialValue = 0f,
                                        targetValue = 360f,
                                        animationSpec =
                                            infiniteRepeatable(tween(1000, easing = LinearEasing)),
                                        label = "rotation",
                                    )
                                    .value
                            Icon(
                                painter = painterResource(CoreR.drawable.ic_loader_circle),
                                contentDescription = null,
                                modifier = Modifier.align(Alignment.Center).rotate(rotation),
                            )
                        } else {
                            Icon(
                                painter = painterResource(CoreR.drawable.ic_rotate_ccw),
                                contentDescription = stringResource(R.string.refresh),
                                modifier = Modifier.align(Alignment.Center),
                            )
                        }
                    }
                }
            }

            if (!isOfflineMode) {
                Surface(
                    onClick = onSearchClick,
                    modifier = Modifier.fillMaxHeight().aspectRatio(1f),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painter = painterResource(CoreR.drawable.ic_search),
                            contentDescription = null,
                        )
                    }
                }
            }

            Surface(
                onClick = onUserClick,
                modifier = Modifier.fillMaxHeight().aspectRatio(1f),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(CoreR.drawable.ic_user),
                        contentDescription = null,
                    )
                }
            }
        }
    }
}

@Composable
@Preview(showBackground = true)
private fun HomeHeaderLoadingPreview() {
    FindroidTheme {
        HomeHeader(
            serverName = "Jellyfin",
            isLoading = true,
            isError = false,
            onErrorClick = {},
            onRetryClick = {},
            onSearchClick = {},
            onUserClick = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
private fun HomeHeaderErrorPreview() {
    FindroidTheme {
        HomeHeader(
            serverName = "Jellyfin",
            isLoading = false,
            isError = true,
            onErrorClick = {},
            onRetryClick = {},
            onSearchClick = {},
            onUserClick = {},
        )
    }
}
