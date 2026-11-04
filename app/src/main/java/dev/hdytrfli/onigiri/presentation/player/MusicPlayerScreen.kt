package dev.hdytrfli.onigiri.presentation.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.hdytrfli.onigiri.core.R as CoreR
import dev.hdytrfli.onigiri.feature.film.R as FilmR
import dev.hdytrfli.onigiri.player.local.music.MusicPlaybackItem
import dev.hdytrfli.onigiri.player.local.music.MusicPlaybackState
import dev.hdytrfli.onigiri.player.local.music.MusicRepeatMode
import dev.hdytrfli.onigiri.ui.components.AppButtonSize
import dev.hdytrfli.onigiri.ui.components.AppButtonVariant
import dev.hdytrfli.onigiri.ui.components.AppIconButton
import dev.hdytrfli.onigiri.ui.theme.spacings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicPlayerScreen(
    state: MusicPlaybackState,
    navigateBack: () -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleFavorite: () -> Unit,
    onPlayQueueItem: (Int) -> Unit,
    onRemoveQueueItem: (Int) -> Unit,
    onClearError: () -> Unit,
) {
    var showQueue by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val background = MaterialTheme.colorScheme.background
    val surface = MaterialTheme.colorScheme.surfaceContainer

    Box(
        modifier =
            Modifier.fillMaxSize()
                .background(Brush.verticalGradient(listOf(surface, background)))
                .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            PlayerTopBar(
                queueCount = state.queue.size,
                onBack = navigateBack,
                onQueue = { showQueue = true },
            )
            val current = state.current
            if (current == null) {
                EmptyPlayer()
            } else {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    if (maxWidth > maxHeight) {
                        Row(
                            modifier =
                                Modifier.fillMaxSize()
                                    .padding(
                                        start = MaterialTheme.spacings.gutter,
                                        end = MaterialTheme.spacings.gutter,
                                        bottom = MaterialTheme.spacings.gutter,
                                    ),
                            horizontalArrangement =
                                Arrangement.spacedBy(MaterialTheme.spacings.large),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AlbumArtwork(item = current, modifier = Modifier.weight(0.48f))
                            PlayerControls(
                                state = state,
                                item = current,
                                onPlayPause = onPlayPause,
                                onPrevious = onPrevious,
                                onNext = onNext,
                                onSeek = onSeek,
                                onToggleShuffle = onToggleShuffle,
                                onCycleRepeat = onCycleRepeat,
                                onToggleFavorite = onToggleFavorite,
                                onClearError = onClearError,
                                modifier = Modifier.weight(0.52f),
                            )
                        }
                    } else {
                        Column(
                            modifier =
                                Modifier.fillMaxSize()
                                    .padding(horizontal = MaterialTheme.spacings.gutter),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Spacer(Modifier.weight(0.25f))
                            AlbumArtwork(item = current, modifier = Modifier.fillMaxWidth(0.82f))
                            Spacer(Modifier.height(MaterialTheme.spacings.large))
                            PlayerControls(
                                state = state,
                                item = current,
                                onPlayPause = onPlayPause,
                                onPrevious = onPrevious,
                                onNext = onNext,
                                onSeek = onSeek,
                                onToggleShuffle = onToggleShuffle,
                                onCycleRepeat = onCycleRepeat,
                                onToggleFavorite = onToggleFavorite,
                                onClearError = onClearError,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(Modifier.weight(0.35f))
                        }
                    }
                }
            }
        }
    }

    if (showQueue) {
        ModalBottomSheet(onDismissRequest = { showQueue = false }, sheetState = sheetState) {
            QueueSheet(
                queue = state.queue,
                currentIndex = state.currentIndex,
                onPlayQueueItem = {
                    onPlayQueueItem(it)
                    showQueue = false
                },
                onRemoveQueueItem = onRemoveQueueItem,
            )
        }
    }
}

@Composable
private fun PlayerTopBar(queueCount: Int, onBack: () -> Unit, onQueue: () -> Unit) {
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .padding(
                    horizontal = MaterialTheme.spacings.gutter,
                    vertical = MaterialTheme.spacings.small,
                ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIconButton(
            onClick = onBack,
            variant = AppButtonVariant.Secondary,
            content = {
                Icon(
                    painter = painterResource(CoreR.drawable.ic_arrow_left),
                    contentDescription = stringResource(CoreR.string.close),
                )
            },
        )
        Text(
            text = stringResource(FilmR.string.music_now_playing),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f).padding(horizontal = MaterialTheme.spacings.small),
        )
        AppIconButton(
            onClick = onQueue,
            variant = AppButtonVariant.Secondary,
            content = {
                Icon(
                    painter = painterResource(CoreR.drawable.ic_list),
                    contentDescription = stringResource(FilmR.string.music_queue),
                )
            },
        )
    }
}

@Composable
private fun AlbumArtwork(item: MusicPlaybackItem, modifier: Modifier = Modifier) {
    Box(
        modifier =
            modifier
                .aspectRatio(1f)
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
    ) {
        if (item.imageUri != null) {
            AsyncImage(
                model = item.imageUri,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                painter = painterResource(CoreR.drawable.ic_music),
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PlayerControls(
    state: MusicPlaybackState,
    item: MusicPlaybackItem,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleFavorite: () -> Unit,
    onClearError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var dragging by remember { mutableStateOf(false) }
    var sliderValue by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(state.positionMs, dragging) {
        if (!dragging && state.durationMs > 0L) {
            sliderValue = (state.positionMs.toFloat() / state.durationMs).coerceIn(0f, 1f)
        }
    }

    Column(modifier = modifier) {
        Text(
            text = item.title,
            style = MaterialTheme.typography.headlineSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(MaterialTheme.spacings.extraSmall))
        Text(
            text = listOfNotNull(item.artist, item.album).joinToString(" · "),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(MaterialTheme.spacings.medium))
        Slider(
            value = sliderValue,
            onValueChange = {
                dragging = true
                sliderValue = it
            },
            onValueChangeFinished = {
                dragging = false
                if (state.durationMs > 0L) {
                    onSeek((sliderValue * state.durationMs).toLong())
                }
            },
            enabled = state.durationMs > 0L,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = formatTime((sliderValue * state.durationMs).toLong()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = formatTime(state.durationMs),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(MaterialTheme.spacings.small))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppIconButton(
                onClick = onToggleShuffle,
                variant = AppButtonVariant.Ghost,
                content = {
                    Icon(
                        painter = painterResource(CoreR.drawable.ic_shuffle),
                        contentDescription = stringResource(FilmR.string.music_shuffle),
                        tint =
                            if (state.shuffleEnabled) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                    )
                },
            )
            AppIconButton(
                onClick = onPrevious,
                variant = AppButtonVariant.Ghost,
                size = AppButtonSize.Player,
                content = {
                    Icon(
                        painter = painterResource(CoreR.drawable.ic_skip_back),
                        contentDescription = stringResource(FilmR.string.music_previous),
                    )
                },
            )
            AppIconButton(
                onClick = onPlayPause,
                size = AppButtonSize.Player,
                content = {
                    Icon(
                        painter =
                            painterResource(
                                if (state.isPlaying) CoreR.drawable.ic_pause
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
                variant = AppButtonVariant.Ghost,
                size = AppButtonSize.Player,
                content = {
                    Icon(
                        painter = painterResource(CoreR.drawable.ic_skip_forward),
                        contentDescription = stringResource(FilmR.string.music_next),
                    )
                },
            )
            AppIconButton(
                onClick = onCycleRepeat,
                variant = AppButtonVariant.Ghost,
                content = {
                    Icon(
                        painter =
                            painterResource(
                                if (state.repeatMode == MusicRepeatMode.ONE) {
                                    CoreR.drawable.ic_repeat_one
                                } else {
                                    CoreR.drawable.ic_repeat
                                }
                            ),
                        contentDescription = stringResource(FilmR.string.music_repeat),
                        tint =
                            if (state.repeatMode != MusicRepeatMode.OFF) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                    )
                },
            )
        }

        Spacer(Modifier.height(MaterialTheme.spacings.small))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppIconButton(
                onClick = onToggleFavorite,
                variant = AppButtonVariant.Ghost,
                content = {
                    Icon(
                        painter =
                            painterResource(
                                if (item.favorite) CoreR.drawable.ic_heart_filled
                                else CoreR.drawable.ic_heart
                            ),
                        contentDescription =
                            stringResource(CoreR.string.favorite_button_description),
                        tint =
                            if (item.favorite) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                    )
                },
            )
            if (state.isBuffering) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(start = MaterialTheme.spacings.small).size(18.dp),
                    strokeWidth = 2.dp,
                )
            }
        }

        state.error?.let { error ->
            Spacer(Modifier.height(MaterialTheme.spacings.small))
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.fillMaxWidth().clickable(onClick = onClearError),
            )
        }
    }
}

@Composable
private fun EmptyPlayer() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                painter = painterResource(CoreR.drawable.ic_music),
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(MaterialTheme.spacings.medium))
            Text(
                text = stringResource(FilmR.string.music_empty_queue),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun QueueSheet(
    queue: List<MusicPlaybackItem>,
    currentIndex: Int,
    onPlayQueueItem: (Int) -> Unit,
    onRemoveQueueItem: (Int) -> Unit,
) {
    Column(
        modifier =
            Modifier.fillMaxWidth()
                .padding(horizontal = MaterialTheme.spacings.gutter)
                .padding(bottom = MaterialTheme.spacings.large)
    ) {
        Text(
            text = stringResource(FilmR.string.music_queue),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = MaterialTheme.spacings.small),
        )
        if (queue.isEmpty()) {
            Text(
                text = stringResource(FilmR.string.music_empty_queue),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = MaterialTheme.spacings.medium),
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = MaterialTheme.spacings.medium),
                modifier = Modifier.heightIn(max = 480.dp),
            ) {
                itemsIndexed(queue, key = { index, item -> "$index-${item.id}" }) { index, item ->
                    QueueRow(
                        item = item,
                        isCurrent = index == currentIndex,
                        onClick = { onPlayQueueItem(index) },
                        onRemove = { onRemoveQueueItem(index) },
                    )
                }
            }
        }
    }
}

@Composable
private fun QueueRow(
    item: MusicPlaybackItem,
    isCurrent: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = MaterialTheme.spacings.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier.size(44.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            if (item.imageUri != null) {
                AsyncImage(
                    model = item.imageUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(
                    painter = painterResource(CoreR.drawable.ic_music),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Column(modifier = Modifier.weight(1f).padding(horizontal = MaterialTheme.spacings.small)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium,
                color =
                    if (isCurrent) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
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
        AppIconButton(
            onClick = onRemove,
            size = AppButtonSize.Decorator,
            variant = AppButtonVariant.Ghost,
            content = {
                Icon(
                    painter = painterResource(CoreR.drawable.ic_x),
                    contentDescription = stringResource(FilmR.string.music_remove_from_queue),
                )
            },
        )
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000L).coerceAtLeast(0L)
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}
