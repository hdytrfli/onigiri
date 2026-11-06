package dev.hdytrfli.onigiri.presentation.player

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.hdytrfli.onigiri.core.R as CoreR
import dev.hdytrfli.onigiri.player.local.music.MusicPlaybackItem
import dev.hdytrfli.onigiri.player.local.music.MusicPlaybackState
import dev.hdytrfli.onigiri.ui.theme.Dimens
import dev.hdytrfli.onigiri.ui.theme.spacings
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**
 * Compact now-playing bar. Swipe right for the next track and left for the previous track. It is
 * shown only while playback is active, so no close or play/pause controls are needed here.
 */
@Composable
fun MiniMusicPlayer(
    state: MusicPlaybackState,
    onOpen: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val item = state.current ?: return
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val swipeThreshold = with(density) { 40.dp.toPx() }
    var dragging by remember { mutableStateOf(false) }
    var dragX by remember { mutableFloatStateOf(0f) }
    var animatedX by remember { mutableFloatStateOf(0f) }
    val contentOffsetX = if (dragging) dragX else animatedX

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(
                    start = MaterialTheme.spacings.gutter,
                    end = MaterialTheme.spacings.gutter,
                    top = MaterialTheme.spacings.extraSmall,
                    bottom = 12.dp,
                ),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            onClick = onOpen,
            modifier =
                Modifier.widthIn(max = Dimens.contentMaxWidth)
                    .fillMaxWidth()
                    .height(64.dp)
                    .pointerInput(
                        state.hasNext,
                        state.hasPrevious,
                    ) {
                        detectDragGestures(
                            onDragStart = {
                                dragging = true
                                dragX = 0f
                            },
                            onDrag = { change, amount ->
                                change.consume()
                                dragX += amount.x
                            },
                            onDragEnd = {
                                val startX = dragX
                                dragging = false
                                animatedX = startX
                                dragX = 0f

                                if (abs(startX) >= swipeThreshold) {
                                    if (startX < 0f) {
                                        if (state.hasNext) onNext()
                                    } else if (state.hasPrevious) {
                                        onPrevious()
                                    }
                                }

                                scope.launch {
                                    animate(
                                        startX,
                                        0f,
                                        animationSpec = tween(durationMillis = 180),
                                    ) { value, _ ->
                                        animatedX = value
                                    }
                                }
                            },
                            onDragCancel = {
                                val startX = dragX
                                dragging = false
                                animatedX = startX
                                dragX = 0f
                                scope.launch {
                                    animate(
                                        startX,
                                        0f,
                                        animationSpec = tween(durationMillis = 180),
                                    ) { value, _ ->
                                        animatedX = value
                                    }
                                }
                            },
                        )
                    },
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 3.dp,
            shadowElevation = 6.dp,
        ) {
            Row(
                modifier =
                    Modifier.fillMaxSize()
                        .offset { IntOffset(contentOffsetX.roundToInt(), 0) }
                        .padding(start = 8.dp, end = 20.dp),
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
    }
}

@Composable
private fun MusicThumbnail(item: MusicPlaybackItem) {
    Box(
        modifier =
            Modifier.size(48.dp)
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
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
