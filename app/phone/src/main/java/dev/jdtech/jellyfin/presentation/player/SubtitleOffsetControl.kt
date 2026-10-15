package dev.jdtech.jellyfin.presentation.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.jdtech.jellyfin.R
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.presentation.theme.spacings
import kotlin.math.abs
import kotlinx.coroutines.delay

private const val MAX_OFFSET_SECONDS = 30f
private const val STEP_SECONDS = 0.1f

/**
 * Subtitle timing offset control: `-`/`+` step by 100 ms (hold to repeat faster) and a slider
 * covering ±30 s. Backed by mpv's `sub-delay`; disabled on other backends.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubtitleOffsetControl(
    enabled: Boolean,
    initialDelaySeconds: Float,
    onDelayChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    var delaySeconds by remember(initialDelaySeconds) { mutableStateOf(initialDelaySeconds) }

    fun update(value: Float) {
        delaySeconds = value.coerceIn(-MAX_OFFSET_SECONDS, MAX_OFFSET_SECONDS)
        onDelayChange(delaySeconds)
    }

    Column(
        modifier = modifier.fillMaxWidth().padding(MaterialTheme.spacings.default),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.medium),
    ) {
        Text(
            text = stringResource(R.string.player_subtitle_offset),
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small),
        ) {
            HoldRepeatIcon(
                enabled = enabled,
                iconRes = CoreR.drawable.ic_minus_fat,
                contentDescription = stringResource(R.string.player_subtitle_delay_earlier),
                onStep = { update(delaySeconds - STEP_SECONDS) },
            )
            Slider(
                value = delaySeconds,
                onValueChange = { update(it) },
                valueRange = -MAX_OFFSET_SECONDS..MAX_OFFSET_SECONDS,
                enabled = enabled,
                colors = sliderColors(),
                thumb = {
                    Box(
                        modifier =
                            Modifier.width(3.dp)
                                .height(12.dp)
                                .clip(RoundedCornerShape(1.5.dp))
                                .background(Color.White)
                    )
                },
                track = { state ->
                    SliderDefaults.Track(
                        sliderState = state,
                        colors = sliderColors(),
                        modifier = Modifier.height(8.dp),
                    )
                },
                modifier = Modifier.weight(1f),
            )
            HoldRepeatIcon(
                enabled = enabled,
                iconRes = CoreR.drawable.ic_plus,
                contentDescription = stringResource(R.string.player_subtitle_delay_later),
                onStep = { update(delaySeconds + STEP_SECONDS) },
            )
            ResetIcon(enabled = enabled, onClick = { update(0f) })
        }
        Text(
            text = stringResource(R.string.player_subtitle_offset_disclaimer),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        )
        if (!enabled) {
            Text(
                text = stringResource(R.string.player_subtitle_offset_unavailable),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** An icon that fires [onStep] repeatedly while held, accelerating over time. */
@Composable
private fun HoldRepeatIcon(
    enabled: Boolean,
    iconRes: Int,
    contentDescription: String,
    onStep: () -> Unit,
) {
    var pressed by remember { mutableStateOf(false) }

    LaunchedEffect(pressed) {
        if (!pressed) return@LaunchedEffect
        var count = 0
        while (true) {
            onStep()
            count++
            delay(if (count < 6) 180L else 50L)
        }
    }

    Box(
        modifier =
            Modifier.size(32.dp).pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures(
                    onPress = {
                        pressed = true
                        tryAwaitRelease()
                        pressed = false
                    }
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            modifier = Modifier.size(20.dp),
            tint =
                if (enabled) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
        )
    }
}

/** A 20dp icon (matching the track rows) that resets the offset to zero. */
@Composable
private fun ResetIcon(enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(32.dp).clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(CoreR.drawable.ic_rotate_ccw),
            contentDescription = stringResource(R.string.player_subtitle_offset_reset),
            modifier = Modifier.size(20.dp),
            tint =
                if (enabled) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
        )
    }
}

/** Formats a subtitle offset in seconds, e.g. "+0.5 s" / "-0.3 s" / "0.0 s". */
fun formatSubtitleOffset(seconds: Float): String =
    if (abs(seconds) < 0.05f) "0.0 s" else "%+.1f s".format(seconds)

/** Matches the player's seek bar (RoundedTimeBar): white played/scrubber, faint unplayed. */
@Composable
private fun sliderColors() =
    SliderDefaults.colors(
        thumbColor = Color.White,
        activeTrackColor = Color.White,
        inactiveTrackColor = Color(0x33FFFFFF),
    )
