package dev.hdytrfli.onigiri.feature.film.presentation.components

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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import dev.hdytrfli.onigiri.core.R as CoreR
import dev.hdytrfli.onigiri.ui.components.AppButtonGroup
import dev.hdytrfli.onigiri.ui.components.AppButtonSize
import dev.hdytrfli.onigiri.ui.components.AppButtonVariant
import dev.hdytrfli.onigiri.ui.components.AppIconButton
import dev.hdytrfli.onigiri.ui.theme.FindroidTheme
import dev.hdytrfli.onigiri.ui.theme.spacings
import dev.hdytrfli.onigiri.ui.utils.LocalOfflineMode

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
        modifier = modifier.fillMaxWidth().height(AppButtonSize.Default.value),
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

        Spacer(Modifier.width(MaterialTheme.spacings.small))

        AppButtonGroup {
            AnimatedVisibility(visible = isError, enter = fadeIn(), exit = fadeOut()) {
                AppIconButton(
                    onClick = onErrorClick,
                    variant = AppButtonVariant.Destructive,
                    content = {
                        Icon(
                            painter = painterResource(CoreR.drawable.ic_alert_circle),
                            contentDescription = null,
                        )
                    },
                )
            }

            AnimatedVisibility(visible = isLoading || isError, enter = fadeIn(), exit = fadeOut()) {
                AppIconButton(
                    onClick = onRetryClick,
                    enabled = !isLoading,
                    content = {
                        // Keep the spinner through the success fade-out: switching to the retry
                        // icon first made the refresh button flash a rotate arrow as it vanished.
                        if (isError && !isLoading) {
                            Icon(
                                painter = painterResource(CoreR.drawable.ic_rotate_ccw),
                                contentDescription = stringResource(CoreR.string.refresh),
                            )
                        } else {
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
                                modifier = Modifier.rotate(rotation),
                            )
                        }
                    },
                )
            }

            if (!isOfflineMode) {
                AppIconButton(
                    onClick = onSearchClick,
                    content = {
                        Icon(
                            painter = painterResource(CoreR.drawable.ic_search),
                            contentDescription = null,
                        )
                    },
                )
            }

            AppIconButton(
                onClick = onUserClick,
                content = {
                    Icon(
                        painter = painterResource(CoreR.drawable.ic_user),
                        contentDescription = null,
                    )
                },
            )
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
