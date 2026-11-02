package dev.hdytrfli.onigiri.feature.film.presentation.components

import androidx.annotation.DrawableRes
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import dev.hdytrfli.onigiri.core.R as CoreR
import dev.hdytrfli.onigiri.models.FindroidItem
import dev.hdytrfli.onigiri.ui.components.AppButton
import dev.hdytrfli.onigiri.ui.preview.dummyEpisode
import dev.hdytrfli.onigiri.ui.preview.dummyMovie
import dev.hdytrfli.onigiri.ui.theme.FindroidTheme

@Composable
fun PlayButton(
    item: FindroidItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: String? = null,
    @DrawableRes iconRes: Int = CoreR.drawable.ic_play,
) {
    val runtimeMinutesLeft by
        remember(item.playbackPositionTicks) {
            mutableLongStateOf((item.runtimeTicks - item.playbackPositionTicks) / 600000000)
        }

    AppButton(onClick = onClick, modifier = modifier, enabled = enabled) {
        Icon(painter = painterResource(iconRes), contentDescription = null)
        Text(
            text =
                label
                    ?: if (item.playbackPositionTicks > 0) {
                        stringResource(CoreR.string.runtime_minutes_left, runtimeMinutesLeft)
                    } else {
                        stringResource(CoreR.string.play)
                    }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PlayButtonMoviePreview() {
    FindroidTheme { PlayButton(item = dummyMovie, onClick = {}) }
}

@Preview(showBackground = true)
@Composable
private fun PlayButtonEpisodePreview() {
    FindroidTheme { PlayButton(item = dummyEpisode, onClick = {}) }
}
