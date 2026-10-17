package dev.jdtech.jellyfin.presentation.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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

/** How often mpv is polled for the panel, and how many readings the graph keeps. */
private const val SAMPLE_INTERVAL_MS = 500L
private const val SAMPLE_COUNT = 60

/** Separator between the parts of one row, e.g. `h264 (High) · 5.2 Mbps`. */
private const val VALUE_SEPARATOR = " · "

private val PANEL_CORNER = 12.dp
private val LABEL_WIDTH = 62.dp
private val GRAPH_WIDTH = 220.dp
private val GRAPH_HEIGHT = 28.dp
private val BAR_GAP = 1.dp

/**
 * mpv's playback statistics: what is being decoded, how, and how many frames were lost getting to
 * the screen. [loadStats] is polled while the panel is on screen; the frame-drop graph keeps a
 * rolling window of [sampleCount] readings and right-aligns them so the newest lands at the end.
 */
@Composable
fun PlayerStatsPanel(
    loadStats: () -> PlaybackStats,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    sampleIntervalMs: Long = SAMPLE_INTERVAL_MS,
    sampleCount: Int = SAMPLE_COUNT,
) {
    var stats by remember { mutableStateOf(PlaybackStats()) }
    var droppedPerSample by remember { mutableStateOf<List<Int>>(emptyList()) }

    LaunchedEffect(sampleIntervalMs, sampleCount) {
        // The first reading only sets the baseline: the counters are totals since the file loaded,
        // so graphing the first value would draw every drop so far as if it just happened.
        var previousTotal = -1L
        while (true) {
            // Read on the main thread: an mpv property read is cheap, and staying on the same
            // thread as the player's teardown means a reading can never race the native handle
            // being destroyed.
            val sample = loadStats()
            if (previousTotal >= 0L) {
                val delta = (sample.totalDroppedFrames - previousTotal).coerceAtLeast(0L)
                droppedPerSample = (droppedPerSample + delta.toInt()).takeLast(sampleCount)
            }
            previousTotal = sample.totalDroppedFrames
            stats = sample
            delay(sampleIntervalMs)
        }
    }

    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier =
            modifier
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
                .padding(MaterialTheme.spacings.small),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        StatsRowOrNothing(
            label = R.string.player_stats_video,
            parts =
                listOfNotNull(
                    if (stats.width > 0 && stats.height > 0) {
                        stringResource(R.string.player_stats_resolution, stats.width, stats.height)
                    } else {
                        null
                    },
                    stats.fps?.let { stringResource(R.string.player_stats_fps, decimal(it, 2)) },
                ),
        )
        StatsRowOrNothing(
            label = R.string.player_stats_codec,
            parts =
                listOfNotNull(
                    stats.videoCodec,
                    stats.videoBitrate?.let {
                        stringResource(R.string.player_stats_mbps, decimal(it / 1_000_000, 1))
                    },
                ),
        )
        StatsRowOrNothing(
            label = R.string.player_stats_audio,
            parts =
                listOfNotNull(
                    stats.audioCodec,
                    stats.audioChannels?.let { stringResource(R.string.player_stats_channels, it) },
                    stats.audioBitrate?.let {
                        stringResource(R.string.player_stats_mbps, decimal(it / 1_000_000, 1))
                    },
                ),
        )
        stats.hardwareDecoder?.let {
            StatsRow(label = stringResource(R.string.player_stats_decoder), value = it)
        }
        StatsRowOrNothing(
            label = R.string.player_stats_dropped,
            parts =
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
                ),
        )
        stats.avSyncSeconds?.let {
            StatsRow(
                label = stringResource(R.string.player_stats_sync),
                value = stringResource(R.string.player_stats_seconds, decimal(it, 3)),
            )
        }
        stats.bufferSeconds?.let {
            StatsRow(
                label = stringResource(R.string.player_stats_buffer),
                value = stringResource(R.string.player_stats_seconds, decimal(it, 1)),
            )
        }

        Spacer(modifier = Modifier.height(MaterialTheme.spacings.extraSmall))

        Text(
            text = stringResource(R.string.player_stats_graph),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FrameDropGraph(
            droppedPerSample = droppedPerSample,
            sampleCount = sampleCount,
            modifier = Modifier.width(GRAPH_WIDTH).height(GRAPH_HEIGHT),
        )
    }
}

/** A label/value line, e.g. `Codec h264 (High) · 5.2 Mbps`. */
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

/** Drops the row entirely when mpv reported none of its [parts], so the panel stays tight. */
@Composable
private fun StatsRowOrNothing(label: Int, parts: List<String>) {
    if (parts.isEmpty()) return
    StatsRow(label = stringResource(label), value = parts.joinToString(VALUE_SEPARATOR))
}

/**
 * Frame drops over time, one bar per reading, newest on the right. Bars are scaled against the
 * worst reading in the window so a single dropped frame is still visible.
 */
@Composable
private fun FrameDropGraph(droppedPerSample: List<Int>, sampleCount: Int, modifier: Modifier) {
    val barColor = MaterialTheme.colorScheme.error
    val axisColor = MaterialTheme.colorScheme.outlineVariant

    Canvas(modifier = modifier) {
        drawLine(
            color = axisColor,
            start = Offset(0f, size.height),
            end = Offset(size.width, size.height),
            strokeWidth = 1f,
        )

        val slotWidth = size.width / sampleCount
        val barWidth = (slotWidth - BAR_GAP.toPx()).coerceAtLeast(1f)
        val tallest = droppedPerSample.maxOrNull() ?: 0
        val firstSlot = sampleCount - droppedPerSample.size
        droppedPerSample.forEachIndexed { index, dropped ->
            if (dropped <= 0) return@forEachIndexed
            val barHeight = size.height * dropped.toFloat() / tallest
            drawRect(
                color = barColor,
                topLeft = Offset((firstSlot + index) * slotWidth, size.height - barHeight),
                size = Size(barWidth, barHeight),
            )
        }
    }
}

/** Formats [value] to [digits] decimals. */
private fun decimal(value: Double, digits: Int): String = "%.${digits}f".format(value)
