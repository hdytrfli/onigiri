package dev.hdytrfli.onigiri.presentation.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import dev.hdytrfli.onigiri.ui.utils.rememberSafePadding
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
    onArtistClick: (java.util.UUID) -> Unit,
    onAlbumClick: (java.util.UUID) -> Unit,
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

    val safePadding = rememberSafePadding()

    Box(
        modifier =
            Modifier.fillMaxSize()
                .background(Brush.verticalGradient(listOf(PlayerBackgroundTop, Color.Black)))
                .padding(top = safePadding.top, bottom = safePadding.bottom)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            PlayerTopBar(
                navigateBack = navigateBack,
                favorite = state.current?.favorite == true,
                onToggleFavorite = onToggleFavorite,
                onOpenQueue = onOpenQueue,
                message = overlayMessage,
            )
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
                    onArtistClick = onArtistClick,
                    onAlbumClick = onAlbumClick,
                    onClearError = onClearError,
                )
            }
        }
    }
}

@Composable
private fun PlayerTopBar(
    navigateBack: () -> Unit,
    favorite: Boolean,
    onToggleFavorite: () -> Unit,
    onOpenQueue: () -> Unit,
    message: String?,
) {
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .padding(
                    horizontal = MaterialTheme.spacings.gutter,
                    vertical = MaterialTheme.spacings.small,
                ),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIconButton(
            onClick = navigateBack,
            variant = AppButtonVariant.Secondary,
            content = {
                Icon(
                    painter = painterResource(CoreR.drawable.ic_arrow_left),
                    contentDescription = stringResource(CoreR.string.close),
                )
            },
        )
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            message?.let {
                Surface(
                    shape = CircleShape,
                    color = Color(0xCC000000),
                    contentColor = Color.White,
                    shadowElevation = 6.dp,
                ) {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                    )
                }
            }
        }
        AppIconButton(
            onClick = onToggleFavorite,
            variant = AppButtonVariant.Secondary,
            content = {
                Icon(
                    painter =
                        painterResource(
                            if (favorite) CoreR.drawable.ic_heart_filled
                            else CoreR.drawable.ic_heart
                        ),
                    contentDescription = stringResource(CoreR.string.favorite_button_description),
                    tint =
                        if (favorite) Color(0xFFE5484D)
                        else MaterialTheme.colorScheme.onSecondaryContainer,
                )
            },
        )
        AppIconButton(
            onClick = onOpenQueue,
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
private fun MusicEntityLink(
    label: String,
    imageUri: String?,
    placeholder: Int,
    onClick: () -> Unit,
) {
    if (label.isBlank()) return
    Row(
        modifier =
            Modifier.clip(CircleShape)
                .clickable(onClick = onClick)
                .padding(vertical = MaterialTheme.spacings.extraSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(32.dp).clip(CircleShape).background(Color(0xFF2A2A2A)),
            contentAlignment = Alignment.Center,
        ) {
            if (imageUri != null) {
                AsyncImage(
                    model = imageUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(
                    painter = painterResource(placeholder),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = PlayerOnBackgroundMuted,
                )
            }
        }
        Spacer(Modifier.width(MaterialTheme.spacings.extraSmall))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = PlayerOnBackgroundMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 180.dp),
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
    onArtistClick: (java.util.UUID) -> Unit,
    onAlbumClick: (java.util.UUID) -> Unit,
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
            // Transport controls first, like the video player.
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
                                        if (state.isPlaying) CoreR.drawable.ic_pause
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

            Spacer(Modifier.height(MaterialTheme.spacings.medium))
            item.artistId?.let { artistId ->
                MusicEntityLink(
                    label = item.artist.orEmpty(),
                    imageUri = item.artistImageUri,
                    placeholder = CoreR.drawable.ic_user,
                    onClick = { onArtistClick(artistId) },
                )
            }
                ?: item.artist?.let { artist ->
                    Text(
                        text = artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = PlayerOnBackgroundMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            Spacer(Modifier.height(MaterialTheme.spacings.extraSmall))
            Text(
                text = item.title,
                style = MaterialTheme.typography.headlineSmall,
                color = PlayerOnBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
            item.album?.let { album ->
                Spacer(Modifier.height(MaterialTheme.spacings.extraSmall))
                Text(
                    text = album,
                    style = MaterialTheme.typography.bodyLarge,
                    color = PlayerOnBackgroundMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier =
                        Modifier.fillMaxWidth()
                            .then(
                                item.albumId?.let { albumId ->
                                    Modifier.clickable { onAlbumClick(albumId) }
                                } ?: Modifier
                            ),
                )
            }

            Spacer(Modifier.height(MaterialTheme.spacings.medium))
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
            Slider(
                state = sliderState,
                onValueChange = {
                    dragging = true
                    sliderState.value = it
                },
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
