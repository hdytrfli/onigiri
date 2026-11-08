package dev.hdytrfli.onigiri.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp

/**
 * The app-wide seekbar geometry. The video player's native TimeBar uses the same colors, height,
 * rounded ends and thumb proportions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSeekBar(
    fraction: Float,
    modifier: Modifier = Modifier,
    bufferedFraction: Float? = null,
    enabled: Boolean = true,
    preciseSeeking: Boolean = false,
    onSeekStart: (() -> Unit)? = null,
    onSeekFinished: (() -> Unit)? = null,
    onSeek: (Float) -> Unit,
) {
    var dragging by remember { mutableStateOf(false) }
    val sliderState = remember { SliderState(fraction.coerceIn(0f, 1f)) }

    LaunchedEffect(fraction, dragging) {
        if (!dragging) sliderState.value = fraction.coerceIn(0f, 1f)
    }

    Box(modifier = modifier) {
        Slider(
            state = sliderState,
            onValueChange = { value ->
                if (!dragging) onSeekStart?.invoke()
                dragging = true
                sliderState.value = value
            },
            onValueChangeFinished = {
                dragging = false
                onSeek(sliderState.value)
                onSeekFinished?.invoke()
            },
            enabled = enabled,
            colors = seekBarColors(),
            thumb = {
                Box(
                    modifier =
                        Modifier.width(4.dp)
                            .height(16.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White)
                )
            },
            track = { state ->
                BufferedTrack(
                    fraction = state.value,
                    bufferedFraction = bufferedFraction,
                )
            },
            modifier = Modifier.fillMaxWidth(),
        )
        if (preciseSeeking && enabled) {
            Box(
                modifier =
                    Modifier.matchParentSize()
                        .preciseSeekable(
                            enabled = true,
                            startFraction = { fraction.coerceIn(0f, 1f) },
                            onScrub = { value ->
                                if (!dragging) onSeekStart?.invoke()
                                dragging = true
                                onSeek(value)
                            },
                            onScrubEnd = { value ->
                                dragging = false
                                sliderState.value = value
                                onSeek(value)
                                onSeekFinished?.invoke()
                            },
                            onTap = { value ->
                                dragging = false
                                sliderState.value = value
                                onSeek(value)
                                onSeekFinished?.invoke()
                            },
                        )
            )
        }
    }
}

@Composable
private fun BufferedTrack(fraction: Float, bufferedFraction: Float?) {
    Canvas(modifier = Modifier.fillMaxWidth().height(12.dp)) {
        val stroke = size.height
        val y = size.height / 2f
        val start = stroke / 2f
        val end = size.width - stroke / 2f
        fun line(to: Float, color: Color) {
            if (to <= start) return
            drawLine(
                color = color,
                start = Offset(start, y),
                end = Offset(to.coerceAtMost(end), y),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }

        line(end, Color(0x33FFFFFF))
        bufferedFraction?.let { buffered ->
            line(start + (end - start) * buffered.coerceIn(0f, 1f), Color(0x66FFFFFF))
        }
        line(start + (end - start) * fraction.coerceIn(0f, 1f), Color.White)
    }
}

@Composable
private fun seekBarColors() =
    SliderDefaults.colors(
        thumbColor = Color.White,
        activeTrackColor = Color.Transparent,
        inactiveTrackColor = Color.Transparent,
        activeTickColor = Color.Transparent,
        inactiveTickColor = Color.Transparent,
    )
