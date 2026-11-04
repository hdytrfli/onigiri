package dev.hdytrfli.onigiri.presentation.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.hdytrfli.onigiri.core.R as CoreR
import dev.hdytrfli.onigiri.feature.film.R as FilmR
import dev.hdytrfli.onigiri.player.local.music.MusicPlaybackItem
import dev.hdytrfli.onigiri.player.local.music.MusicPlaybackState
import dev.hdytrfli.onigiri.ui.components.AppButtonVariant
import dev.hdytrfli.onigiri.ui.components.AppIconButton
import dev.hdytrfli.onigiri.ui.theme.spacings

/**
 * Compact now-playing bar: one wide, snackbar-style player pill plus separate transport buttons.
 * Tap the pill to open the full player; the buttons stay independent targets.
 */
@Composable
fun MiniMusicPlayer(
    state: MusicPlaybackState,
    onOpen: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val item = state.current ?: return

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = MaterialTheme.spacings.gutter)
                .padding(vertical = MaterialTheme.spacings.extraSmall),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            onClick = onOpen,
            modifier = Modifier.weight(1f).height(52.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 3.dp,
            shadowElevation = 6.dp,
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(start = 6.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MusicThumbnail(item = item)
                Column(modifier = Modifier.padding(start = MaterialTheme.spacings.small)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = listOfNotNull(item.artist, item.album).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        AppIconButton(
            onClick = onPlayPause,
            variant = AppButtonVariant.Secondary,
            content = {
                Icon(
                    painter =
                        painterResource(
                            if (state.isPlaying) CoreR.drawable.ic_diamond
                            else CoreR.drawable.ic_play
                        ),
                    contentDescription =
                        stringResource(
                            if (state.isPlaying) FilmR.string.music_pause else CoreR.string.play
                        ),
                )
            },
        )
        AppIconButton(
            onClick = onNext,
            variant = AppButtonVariant.Secondary,
            content = {
                Icon(
                    painter = painterResource(CoreR.drawable.ic_skip_forward),
                    contentDescription = stringResource(FilmR.string.music_next),
                )
            },
        )
    }
}

@Composable
private fun MusicThumbnail(item: MusicPlaybackItem) {
    Box(
        modifier =
            Modifier.size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
    ) {
        if (item.imageUri != null) {
            AsyncImage(
                model = item.imageUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(CircleShape),
            )
        } else {
            Icon(
                painter = painterResource(CoreR.drawable.ic_music),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
