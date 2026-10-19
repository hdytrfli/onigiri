package dev.jdtech.jellyfin.presentation.player

import androidx.compose.foundation.background
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.jdtech.jellyfin.R
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.presentation.components.AppButtonSize
import dev.jdtech.jellyfin.presentation.components.AppButtonVariant
import dev.jdtech.jellyfin.presentation.components.AppIconButton
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
        modifier =
            modifier
                .fillMaxWidth()
                .padding(
                    start = 20.dp,
                    top = MaterialTheme.spacings.small,
                    end = 20.dp,
                    bottom = MaterialTheme.spacings.extraSmall,
                ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.medium),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small),
        ) {
            HoldRepeatIcon(
                iconRes = CoreR.drawable.ic_minus_fat,
                contentDescription = stringResource(R.string.player_subtitle_delay_earlier),
                onStep = { update(delaySeconds - STEP_SECONDS) },
            )
            Slider(
                value = delaySeconds,
                onValueChange = { update(it) },
                valueRange = -MAX_OFFSET_SECONDS..MAX_OFFSET_SECONDS,
                colors = sliderColors(),
                thumb = {
                    Box(
                        modifier =
                            Modifier.width(4.dp)
                                .height(26.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.White)
                    )
                },
                track = { state ->
                    SliderDefaults.Track(
                        sliderState = state,
                        colors = sliderColors(),
                        modifier = Modifier.height(12.dp),
                    )
                },
                modifier = Modifier.weight(1f),
            )
            HoldRepeatIcon(
                iconRes = CoreR.drawable.ic_plus,
                contentDescription = stringResource(R.string.player_subtitle_delay_later),
                onStep = { update(delaySeconds + STEP_SECONDS) },
            )
            ResetIcon(onClick = { update(0f) })
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small),
        ) {
            Text(
                text = stringResource(R.string.player_subtitle_offset_by),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = formatSubtitleOffset(delaySeconds),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
            )
        }
        Text(
            text = stringResource(R.string.player_subtitle_offset_disclaimer),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** An icon that fires [onStep] repeatedly while held, accelerating over time. */
@Composable
private fun HoldRepeatIcon(
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

    AppIconButton(
        onClick = {},
        onHold = { pressed = it },
        size = AppButtonSize.Decorator,
        variant = AppButtonVariant.Ghost,
        content = {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = contentDescription,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        },
    )
}

/** A 20dp icon (matching the track rows) that resets the offset to zero. */
@Composable
private fun ResetIcon(onClick: () -> Unit) {
    AppIconButton(
        onClick = onClick,
        size = AppButtonSize.Decorator,
        variant = AppButtonVariant.Ghost,
        content = {
            Icon(
                painter = painterResource(CoreR.drawable.ic_rotate_ccw),
                contentDescription = stringResource(R.string.player_subtitle_offset_reset),
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        },
    )
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
