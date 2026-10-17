package dev.jdtech.jellyfin.presentation.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.jdtech.jellyfin.R
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.player.local.mpv.PlaybackStats
import dev.jdtech.jellyfin.presentation.theme.spacings
import kotlinx.coroutines.delay

/** How often mpv is polled for the panel. */
private const val SAMPLE_INTERVAL_MS = 500L

/** Stands in for a reading mpv did not provide, so the panel never changes shape. */
private const val NO_VALUE = "—"

/** Separator between the parts of one row, e.g. `aac · 2 ch`. */
private const val VALUE_SEPARATOR = " · "

private val PANEL_WIDTH = 300.dp
private val PANEL_CORNER = 12.dp
private val LABEL_WIDTH = 78.dp

/**
 * mpv's playback statistics: what is being decoded, how, and how well it is keeping up.
 *
 * The panel is a fixed-size table - every row is always present and reads [NO_VALUE] when mpv has
 * nothing to report - so the box neither resizes nor reflows as readings come and go. [loadStats]
 * is polled only while the panel is on screen.
 */
@Composable
fun PlayerStatsPanel(
    loadStats: () -> PlaybackStats,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    sampleIntervalMs: Long = SAMPLE_INTERVAL_MS,
) {
    var stats by remember { mutableStateOf(PlaybackStats()) }

    LaunchedEffect(sampleIntervalMs) {
        while (true) {
            // Read on the main thread: an mpv property read is cheap, and staying on the same
            // thread as the player's teardown means a reading can never race the native handle
            // being destroyed.
            stats = loadStats()
            delay(sampleIntervalMs)
        }
    }

    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier =
            modifier
                .width(PANEL_WIDTH)
                .clip(RoundedCornerShape(PANEL_CORNER))
                .background(colorResource(CoreR.color.player_background))
                .then(
                    if (onClick == null) {
                        Modifier
                    } else {
                        Modifier.clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = onClick,
                        )
                    }
                )
                .padding(MaterialTheme.spacings.medium),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.extraSmall),
    ) {
        StatsRow(
            label = stringResource(R.string.player_stats_video),
            value =
                if (stats.width > 0 && stats.height > 0) {
                    stringResource(R.string.player_stats_resolution, stats.width, stats.height)
                } else {
                    NO_VALUE
                },
        )
        StatsRow(
            label = stringResource(R.string.player_stats_fps),
            value = stats.fps?.let { decimal(it, 2) } ?: NO_VALUE,
        )
        StatsRow(
            label = stringResource(R.string.player_stats_codec),
            value = stats.videoCodec ?: NO_VALUE,
        )
        StatsRow(
            label = stringResource(R.string.player_stats_bitrate),
            value =
                stats.videoBitrate?.let {
                    stringResource(R.string.player_stats_mbps, decimal(it / 1_000_000, 1))
                } ?: NO_VALUE,
        )
        StatsRow(
            label = stringResource(R.string.player_stats_audio),
            value = audioValue(stats) ?: NO_VALUE,
        )
        StatsRow(
            label = stringResource(R.string.player_stats_audio_bitrate),
            value =
                stats.audioBitrate?.let {
                    stringResource(R.string.player_stats_kbps, decimal(it / 1_000, 0))
                } ?: NO_VALUE,
        )
        StatsRow(
            label = stringResource(R.string.player_stats_decoder),
            value = stats.hardwareDecoder ?: NO_VALUE,
        )
        StatsRow(label = stringResource(R.string.player_stats_dropped), value = droppedValue(stats))
        StatsRow(
            label = stringResource(R.string.player_stats_sync),
            value =
                stats.avSyncSeconds?.let {
                    stringResource(R.string.player_stats_seconds, decimal(it, 3))
                } ?: NO_VALUE,
        )
        StatsRow(
            label = stringResource(R.string.player_stats_buffer),
            value =
                stats.bufferSeconds?.let {
                    stringResource(R.string.player_stats_seconds, decimal(it, 1))
                } ?: NO_VALUE,
        )
    }
}

/** A label/value line, e.g. `Codec h264 (High)`. */
@Composable
private fun StatsRow(label: String, value: String) {
    Row {
        Text(
            text = label,
            modifier = Modifier.width(LABEL_WIDTH),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** `aac · 2 ch`, or null when there is no audio track to describe. */
@Composable
private fun audioValue(stats: PlaybackStats): String? {
    if (stats.audioCodec == null && stats.audioChannels == null) return null

    return listOfNotNull(
            stats.audioCodec,
            stats.audioChannels?.let { stringResource(R.string.player_stats_channels, it) },
        )
        .joinToString(VALUE_SEPARATOR)
}

/** The drop total, with the per-counter breakdown as soon as any counter is non-zero. */
@Composable
private fun droppedValue(stats: PlaybackStats): String =
    listOfNotNull(
            stats.totalDroppedFrames.toString(),
            if (stats.totalDroppedFrames > 0 || stats.delayedFrames > 0) {
                stringResource(
                    R.string.player_stats_dropped_detail,
                    stats.droppedFrames,
                    stats.decoderDroppedFrames,
                    stats.delayedFrames,
                )
            } else {
                null
            },
        )
        .joinToString(VALUE_SEPARATOR)

/** Formats [value] to [digits] decimals. */
private fun decimal(value: Double, digits: Int): String = "%.${digits}f".format(value)
