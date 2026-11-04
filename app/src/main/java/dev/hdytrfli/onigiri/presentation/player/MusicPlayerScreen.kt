package dev.hdytrfli.onigiri.presentation.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import dev.hdytrfli.onigiri.player.local.music.MusicRepeatMode
import dev.hdytrfli.onigiri.ui.components.AppButtonSize
import dev.hdytrfli.onigiri.ui.components.AppButtonVariant
import dev.hdytrfli.onigiri.ui.components.AppIconButton
import dev.hdytrfli.onigiri.ui.theme.Dimens
import dev.hdytrfli.onigiri.ui.theme.spacings
import kotlinx.coroutines.delay

private val PlayerBackgroundTop = Color(0xFF181818)
private val PlayerOnBackground = Color.White
private val PlayerOnBackgroundMuted = Color(0xB3FFFFFF)

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
    onOpenQueue: () -> Unit,
    onClearError: () -> Unit,
) {
    var overlayMessage by remember { mutableStateOf<String?>(null) }

    val shuffleOn = stringResource(FilmR.string.music_shuffle_on)
    val shuffleOff = stringResource(FilmR.string.music_shuffle_off)
    val repeatOff = stringResource(FilmR.string.music_repeat_off)
    val repeatAll = stringResource(FilmR.string.music_repeat_all)
    val repeatOne = stringResource(FilmR.string.music_repeat_one)

    LaunchedEffect(overlayMessage) {
        if (overlayMessage != null) {
            delay(1_500)
            overlayMessage = null
        }
    }

    Box(
        modifier =
            Modifier.fillMaxSize()
                .background(Brush.verticalGradient(listOf(PlayerBackgroundTop, Color.Black)))
                .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            PlayerTopBar(navigateBack = navigateBack, onOpenQueue = onOpenQueue)
            val current = state.current
            if (current == null) {
                EmptyPlayer()
            } else {
                BoxWithConstraints(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    val gutter = MaterialTheme.spacings.gutter
                    val artSize = minOf(maxWidth - gutter * 2, Dimens.contentMaxWidth, maxHeight)
                    AlbumArtwork(item = current, modifier = Modifier.size(artSize))
                }
                BottomControls(
                    state = state,
                    item = current,
                    onPlayPause = onPlayPause,
                    onPrevious = onPrevious,
                    onNext = onNext,
                    onSeek = onSeek,
                    onToggleShuffle = {
                        overlayMessage = if (state.shuffleEnabled) shuffleOff else shuffleOn
                        onToggleShuffle()
                    },
                    onCycleRepeat = {
                        overlayMessage =
                            when (state.repeatMode) {
                                MusicRepeatMode.OFF -> repeatAll
                                MusicRepeatMode.ALL -> repeatOne
                                MusicRepeatMode.ONE -> repeatOff
                            }
                        onCycleRepeat()
                    },
                    onToggleFavorite = onToggleFavorite,
                    onClearError = onClearError,
                )
            }
        }

        overlayMessage?.let { message ->
            Surface(
                modifier = Modifier.align(Alignment.Center),
                shape = CircleShape,
                color = Color(0xCC000000),
                contentColor = Color.White,
                shadowElevation = 6.dp,
            ) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                )
            }
        }
    }
}

@Composable
private fun PlayerTopBar(navigateBack: () -> Unit, onOpenQueue: () -> Unit) {
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
            onClick = navigateBack,
            variant = AppButtonVariant.Ghost,
            content = {
                Icon(
                    painter = painterResource(CoreR.drawable.ic_arrow_left),
                    contentDescription = stringResource(CoreR.string.close),
                    tint = PlayerOnBackground,
                )
            },
        )
        Spacer(modifier = Modifier.weight(1f))
        AppIconButton(
            onClick = onOpenQueue,
            variant = AppButtonVariant.Ghost,
            content = {
                Icon(
                    painter = painterResource(CoreR.drawable.ic_list),
                    contentDescription = stringResource(FilmR.string.music_queue),
                    tint = PlayerOnBackground,
                )
            },
        )
    }
}

@Composable
private fun AlbumArtwork(item: MusicPlaybackItem, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.clip(MaterialTheme.shapes.large).background(Color(0xFF242424)),
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
                tint = PlayerOnBackgroundMuted,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BottomControls(
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
) {
    var dragging by remember { mutableStateOf(false) }
    val sliderState = remember { SliderState(0f) }

    LaunchedEffect(state.positionMs, dragging) {
        if (!dragging && state.durationMs > 0L) {
            sliderState.value = (state.positionMs.toFloat() / state.durationMs).coerceIn(0f, 1f)
        }
    }

    Column(
        modifier =
            Modifier.fillMaxWidth()
                .padding(
                    start = MaterialTheme.spacings.gutter,
                    end = MaterialTheme.spacings.gutter,
                    bottom = MaterialTheme.spacings.medium,
                ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(modifier = Modifier.widthIn(max = Dimens.contentMaxWidth).fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = PlayerOnBackground,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(MaterialTheme.spacings.extraSmall))
                    Text(
                        text = listOfNotNull(item.artist, item.album).joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = PlayerOnBackgroundMuted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
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
                            tint = if (item.favorite) Color(0xFFE5484D) else PlayerOnBackground,
                        )
                    },
                )
            }

            Spacer(Modifier.height(MaterialTheme.spacings.small))
            Slider(
                state = sliderState,
                onValueChange = { dragging = true },
                onValueChangeFinished = {
                    dragging = false
                    if (state.durationMs > 0L) {
                        onSeek((sliderState.value * state.durationMs).toLong())
                    }
                },
                enabled = state.durationMs > 0L,
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
                track = { sliderState ->
                    SliderDefaults.Track(
                        sliderState = sliderState,
                        colors = playerSliderColors(),
                        modifier = Modifier.height(12.dp),
                    )
                },
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = formatTime((sliderState.value * state.durationMs).toLong()),
                    style = MaterialTheme.typography.bodySmall,
                    color = PlayerOnBackgroundMuted,
                )
                Text(
                    text = formatTime(state.durationMs),
                    style = MaterialTheme.typography.bodySmall,
                    color = PlayerOnBackgroundMuted,
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
                                if (state.shuffleEnabled) PlayerOnBackground
                                else PlayerOnBackgroundMuted,
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
                            tint = PlayerOnBackground,
                        )
                    },
                )
                AppIconButton(
                    onClick = onPlayPause,
                    size = AppButtonSize.Player,
                    content = {
                        if (state.isBuffering) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        } else {
                            Icon(
                                painter =
                                    painterResource(
                                        if (state.isPlaying) CoreR.drawable.ic_diamond
                                        else CoreR.drawable.ic_play
                                    ),
                                contentDescription =
                                    stringResource(
                                        if (state.isPlaying) FilmR.string.music_pause
                                        else CoreR.string.play
                                    ),
                            )
                        }
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
                            tint = PlayerOnBackground,
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
                                if (state.repeatMode != MusicRepeatMode.OFF) PlayerOnBackground
                                else PlayerOnBackgroundMuted,
                        )
                    },
                )
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
}

@Composable
private fun playerSliderColors() =
    SliderDefaults.colors(
        thumbColor = Color.White,
        activeTrackColor = Color.White,
        inactiveTrackColor = Color(0x33FFFFFF),
        activeTickColor = Color.Transparent,
        inactiveTickColor = Color.Transparent,
    )

@Composable
private fun EmptyPlayer() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                painter = painterResource(CoreR.drawable.ic_music),
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = PlayerOnBackgroundMuted,
            )
            Spacer(Modifier.height(MaterialTheme.spacings.medium))
            Text(
                text = stringResource(FilmR.string.music_empty_queue),
                style = MaterialTheme.typography.bodyMedium,
                color = PlayerOnBackgroundMuted,
            )
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000L).coerceAtLeast(0L)
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}
