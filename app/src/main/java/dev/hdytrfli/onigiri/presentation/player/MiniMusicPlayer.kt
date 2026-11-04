package dev.hdytrfli.onigiri.presentation.player

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.hdytrfli.onigiri.core.R as CoreR
import dev.hdytrfli.onigiri.feature.film.R as FilmR
import dev.hdytrfli.onigiri.player.local.music.MusicPlaybackItem
import dev.hdytrfli.onigiri.player.local.music.MusicPlaybackState
import dev.hdytrfli.onigiri.ui.components.AppButtonVariant
import dev.hdytrfli.onigiri.ui.components.AppIconButton
import dev.hdytrfli.onigiri.ui.theme.spacings
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**
 * Compact now-playing bar: one wide, snackbar-style player pill plus a pause/resume button. Swipe
 * the pill right for the next track, left for the previous track, or down to close the queue.
 */
@Composable
fun MiniMusicPlayer(
    state: MusicPlaybackState,
    onOpen: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val item = state.current ?: return
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val swipeThreshold = with(density) { 56.dp.toPx() }
    var dragging by remember { mutableStateOf(false) }
    var dragX by remember { mutableFloatStateOf(0f) }
    var dragY by remember { mutableFloatStateOf(0f) }
    var animatedX by remember { mutableFloatStateOf(0f) }
    var animatedY by remember { mutableFloatStateOf(0f) }
    val contentOffsetX = if (dragging) dragX else animatedX
    val containerOffsetY = if (dragging) dragY.coerceAtLeast(0f) else animatedY

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .offset { IntOffset(0, containerOffsetY.roundToInt()) }
                .padding(
                    start = MaterialTheme.spacings.gutter,
                    end = MaterialTheme.spacings.gutter,
                    top = MaterialTheme.spacings.extraSmall,
                    bottom = MaterialTheme.spacings.medium,
                ),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            onClick = onOpen,
            modifier =
                Modifier.weight(1f).height(52.dp).pointerInput(state.hasNext, state.hasPrevious) {
                    detectDragGestures(
                        onDragStart = {
                            dragging = true
                            dragX = 0f
                            dragY = 0f
                        },
                        onDrag = { change, amount ->
                            change.consume()
                            dragX += amount.x
                            dragY += amount.y
                        },
                        onDragEnd = {
                            val startX = dragX
                            val startY = dragY.coerceAtLeast(0f)
                            val horizontal = abs(startX) > abs(startY)
                            dragging = false
                            animatedX = startX
                            animatedY = startY
                            dragX = 0f
                            dragY = 0f

                            if (horizontal && abs(startX) >= swipeThreshold) {
                                if (startX > 0f) {
                                    if (state.hasNext) onNext()
                                } else if (state.hasPrevious) {
                                    onPrevious()
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
                            } else if (!horizontal && startY >= swipeThreshold) {
                                scope.launch {
                                    animate(
                                        initialValue = startY,
                                        targetValue = 180f,
                                        animationSpec = tween(durationMillis = 160),
                                    ) { value, _ ->
                                        animatedY = value
                                    }
                                    onClose()
                                }
                            } else {
                                scope.launch {
                                    animate(
                                        startX,
                                        0f,
                                        animationSpec = tween(durationMillis = 180),
                                    ) { value, _ ->
                                        animatedX = value
                                    }
                                }
                                scope.launch {
                                    animate(
                                        startY,
                                        0f,
                                        animationSpec = tween(durationMillis = 180),
                                    ) { value, _ ->
                                        animatedY = value
                                    }
                                }
                            }
                        },
                        onDragCancel = {
                            val startX = dragX
                            val startY = dragY.coerceAtLeast(0f)
                            dragging = false
                            animatedX = startX
                            animatedY = startY
                            dragX = 0f
                            dragY = 0f
                            scope.launch {
                                animate(startX, 0f, animationSpec = tween(durationMillis = 180)) {
                                    value,
                                    _ ->
                                    animatedX = value
                                }
                            }
                            scope.launch {
                                animate(startY, 0f, animationSpec = tween(durationMillis = 180)) {
                                    value,
                                    _ ->
                                    animatedY = value
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
                        .padding(start = 6.dp, end = 16.dp),
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
                            if (state.isPlaying) CoreR.drawable.ic_pause else CoreR.drawable.ic_play
                        ),
                    contentDescription =
                        stringResource(
                            if (state.isPlaying) FilmR.string.music_pause else CoreR.string.play
                        ),
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
