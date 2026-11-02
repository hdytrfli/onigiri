package dev.hdytrfli.onigiri.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * A stepped slider in the player's seek-bar style: white played track, faint unplayed track and the
 * thin white handle. [index] is the selected step (0 until [count]); the slider snaps to steps.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SteppedSlider(
    index: Int,
    count: Int,
    onIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val steps = count.coerceAtLeast(2)
    val valueRange = 0f..(steps - 1).toFloat()
    val state = remember(steps) { SliderState(index.toFloat(), steps - 2, valueRange) }
    state.value = index.coerceIn(0, steps - 1).toFloat()
    Slider(
        state = state,
        onValueChange = { onIndexChange(it.roundToInt()) },
        colors = playerSliderColors(),
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
            SliderDefaults.Track(
                sliderState = state,
                colors = playerSliderColors(),
                modifier = Modifier.height(12.dp),
            )
        },
        modifier = modifier,
    )
}

/** Matches the player's seek bar; the step indicators are deliberately faint. */
@Composable
private fun playerSliderColors() =
    SliderDefaults.colors(
        thumbColor = Color.White,
        activeTrackColor = Color.White,
        inactiveTrackColor = Color(0x33FFFFFF),
        activeTickColor = Color(0x24000000),
        inactiveTickColor = Color(0x14FFFFFF),
    )
