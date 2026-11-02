package dev.hdytrfli.onigiri.feature.film.presentation.components

import android.app.DownloadManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import dev.hdytrfli.onigiri.core.R as CoreR
import dev.hdytrfli.onigiri.feature.film.presentation.downloader.DownloaderState
import dev.hdytrfli.onigiri.models.UiText
import dev.hdytrfli.onigiri.ui.components.AppIconButton
import dev.hdytrfli.onigiri.ui.theme.FindroidTheme
import dev.hdytrfli.onigiri.ui.theme.Green
import dev.hdytrfli.onigiri.ui.theme.Yellow
import dev.hdytrfli.onigiri.ui.theme.spacings
import kotlin.math.roundToInt

@Composable
fun DownloaderCard(state: DownloaderState, onCancelClick: () -> Unit, onRetryClick: () -> Unit) {
    val animatedProgress by
        animateFloatAsState(
            targetValue = state.progress,
            animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
        )

    val textColor =
        when (state.status) {
            DownloadManager.STATUS_PAUSED -> Yellow
            DownloadManager.STATUS_FAILED -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.onSurface
        }

    val statusText =
        when (state.status) {
            DownloadManager.STATUS_PENDING -> stringResource(CoreR.string.download_pending)
            DownloadManager.STATUS_PAUSED -> stringResource(CoreR.string.download_paused)
            DownloadManager.STATUS_FAILED -> stringResource(CoreR.string.download_failed)
            else -> stringResource(CoreR.string.download_downloading)
        }

    val progressIndicatorColor =
        when (state.status) {
            DownloadManager.STATUS_PAUSED -> Yellow
            DownloadManager.STATUS_SUCCESSFUL -> Green
            DownloadManager.STATUS_FAILED -> MaterialTheme.colorScheme.error
            else -> ProgressIndicatorDefaults.linearColor
        }

    val progressTrackColor =
        when (state.status) {
            DownloadManager.STATUS_FAILED -> MaterialTheme.colorScheme.errorContainer
            else -> ProgressIndicatorDefaults.linearTrackColor
        }

    OutlinedCard {
        Row(
            modifier = Modifier.fillMaxWidth().padding(MaterialTheme.spacings.medium),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.medium),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = statusText,
                        color = textColor,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = animatedProgress.times(100).roundToInt().toString() + "%",
                        color = textColor,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                Spacer(Modifier.height(MaterialTheme.spacings.small))
                when (state.status) {
                    DownloadManager.STATUS_PENDING -> {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                    else -> {
                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier.fillMaxWidth(),
                            color = progressIndicatorColor,
                            trackColor = progressTrackColor,
                        )
                    }
                }
                Spacer(Modifier.height(MaterialTheme.spacings.small))
                if (state.errorText != null) {
                    Text(
                        text = state.errorText.asString(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            when (state.status) {
                DownloadManager.STATUS_PENDING,
                DownloadManager.STATUS_RUNNING -> {
                    AppIconButton(
                        onClick = onCancelClick,
                        content = {
                            Icon(
                                painter = painterResource(CoreR.drawable.ic_x),
                                contentDescription = null,
                            )
                        },
                    )
                }
                DownloadManager.STATUS_FAILED -> {
                    AppIconButton(
                        onClick = onRetryClick,
                        content = {
                            Icon(
                                painter = painterResource(CoreR.drawable.ic_rotate_ccw),
                                contentDescription = null,
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
@Preview
private fun DownloaderCardPendingPreview() {
    FindroidTheme {
        DownloaderCard(
            state = DownloaderState(status = DownloadManager.STATUS_PENDING),
            onCancelClick = {},
            onRetryClick = {},
        )
    }
}

@Composable
@Preview
private fun DownloaderCardDownloadingPreview() {
    FindroidTheme {
        DownloaderCard(
            state = DownloaderState(status = DownloadManager.STATUS_RUNNING, progress = 0.5f),
            onCancelClick = {},
            onRetryClick = {},
        )
    }
}

@Composable
@Preview
private fun DownloaderCardFailedPreview() {
    FindroidTheme {
        DownloaderCard(
            state =
                DownloaderState(
                    status = DownloadManager.STATUS_FAILED,
                    progress = 0.5f,
                    errorText = UiText.DynamicString("Not enough storage space"),
                ),
            onCancelClick = {},
            onRetryClick = {},
        )
    }
}
