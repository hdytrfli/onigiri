package dev.hdytrfli.onigiri.presentation.player

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
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
import dev.hdytrfli.onigiri.ui.components.AppDropdownMenu
import dev.hdytrfli.onigiri.ui.components.AppIconButton
import dev.hdytrfli.onigiri.ui.components.MenuActionItem
import dev.hdytrfli.onigiri.ui.theme.Dimens
import dev.hdytrfli.onigiri.ui.theme.Red
import dev.hdytrfli.onigiri.ui.theme.spacings
import dev.hdytrfli.onigiri.ui.utils.rememberSafePadding
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private val PlayerBackgroundTop = Color(0xFF181818)
private val PlayerOnBackground = Color.White
private val PlayerOnBackgroundMuted = Color(0xB3FFFFFF)

@Composable
fun MusicPlayerScreen(
    state: MusicPlaybackState,
    artworkType: String,
    navigateBack: () -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleFavoriteItem: (java.util.UUID) -> Unit,
    onOpenQueue: () -> Unit,
    onArtistClick: (java.util.UUID) -> Unit,
    onAlbumClick: (java.util.UUID) -> Unit,
    onPlayQueueItem: (Int) -> Unit,
    onRemoveQueueItem: (Int) -> Unit,
    onMoveQueueItem: (fromIndex: Int, toIndex: Int) -> Unit,
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
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val current = state.current
    val toggleShuffle: () -> Unit = {
        overlayMessage = if (state.shuffleEnabled) shuffleOff else shuffleOn
        onToggleShuffle()
    }
    val cycleRepeat: () -> Unit = {
        overlayMessage =
            when (state.repeatMode) {
                MusicRepeatMode.OFF -> repeatAll
                MusicRepeatMode.ALL -> repeatOne
                MusicRepeatMode.ONE -> repeatOff
            }
        onCycleRepeat()
    }
    val gutter = MaterialTheme.spacings.gutter
    val isAudiobook = current?.isAudiobook == true

    Box(
        modifier =
            Modifier.fillMaxSize()
                .background(Brush.verticalGradient(listOf(PlayerBackgroundTop, Color.Black)))
                .padding(top = safePadding.top, bottom = safePadding.bottom)
    ) {
        if (current != null && isLandscape) {
            Row(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(0.5f).fillMaxHeight()) {
                    BoxWithConstraints(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        val artSize =
                            minOf(
                                    maxWidth,
                                    maxHeight - 48.dp - AppButtonSize.Default.value,
                                    Dimens.contentMaxWidth,
                                )
                                .coerceAtLeast(0.dp)
                        Column(
                            modifier =
                                Modifier.widthIn(max = Dimens.contentMaxWidth)
                                    .fillMaxWidth()
                                    .padding(horizontal = MaterialTheme.spacings.small),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            AlbumArtwork(
                                item = current,
                                artworkType = artworkType,
                                isPlaying = state.isPlaying,
                                modifier = Modifier.size(artSize),
                            )
                            Spacer(Modifier.height(48.dp))
                            TransportControls(
                                state = state,
                                onPlayPause = onPlayPause,
                                onPrevious = onPrevious,
                                onNext = onNext,
                                onToggleShuffle = toggleShuffle,
                                onCycleRepeat = cycleRepeat,
                                onToggleFavorite = onToggleFavorite,
                                onArtistClick = onArtistClick,
                                onAlbumClick = onAlbumClick,
                            )
                        }
                    }
                    PlayerTopBar(
                        navigateBack = navigateBack,
                        onOpenQueue = onOpenQueue,
                        showQueueButton = false,
                        modifier = Modifier.align(Alignment.TopCenter),
                    )
                }
                if (isAudiobook) {
                    Box(
                        modifier = Modifier.weight(0.5f).fillMaxHeight(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            modifier =
                                Modifier.fillMaxWidth()
                                    .padding(
                                        horizontal = MaterialTheme.spacings.medium,
                                        vertical = 48.dp,
                                    )
                        ) {
                            PlayerDetails(item = current)
                            Spacer(Modifier.height(MaterialTheme.spacings.medium))
                            PlayerSeekBar(
                                state = state,
                                onSeek = onSeek,
                                onClearError = onClearError,
                            )
                        }
                    }
                } else {
                    Column(modifier = Modifier.weight(0.5f).fillMaxHeight().padding(end = gutter)) {
                        Column(
                            modifier =
                                Modifier.fillMaxWidth()
                                    .padding(
                                        start = MaterialTheme.spacings.medium,
                                        end = MaterialTheme.spacings.medium,
                                        top = 48.dp,
                                        bottom = 48.dp,
                                    )
                        ) {
                            PlayerDetails(item = current)
                            Spacer(Modifier.height(MaterialTheme.spacings.medium))
                            PlayerSeekBar(
                                state = state,
                                onSeek = onSeek,
                                onClearError = onClearError,
                            )
                        }
                        MusicQueueContent(
                            queue = state.queue,
                            currentIndex = state.currentIndex,
                            onPlayNow = onPlayQueueItem,
                            onRemove = onRemoveQueueItem,
                            onMove = onMoveQueueItem,
                            onToggleFavorite = onToggleFavoriteItem,
                            isShuffling = state.isShuffling,
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            contentBottomPadding = MaterialTheme.spacings.gutter,
                        )
                    }
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                PlayerTopBar(
                    navigateBack = navigateBack,
                    onOpenQueue = onOpenQueue,
                    showQueueButton = !isLandscape && !isAudiobook,
                )
                if (current == null) {
                    EmptyPlayer()
                } else {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        BoxWithConstraints(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            val artSize =
                                minOf(
                                        maxWidth,
                                        maxHeight - 48.dp - AppButtonSize.Default.value,
                                        Dimens.contentMaxWidth,
                                    )
                                    .coerceAtLeast(0.dp)
                            Column(
                                modifier =
                                    Modifier.widthIn(max = Dimens.contentMaxWidth)
                                        .fillMaxWidth()
                                        .padding(horizontal = MaterialTheme.spacings.small),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                AlbumArtwork(
                                    item = current,
                                    artworkType = artworkType,
                                    isPlaying = state.isPlaying,
                                    modifier = Modifier.size(artSize),
                                )
                                Spacer(Modifier.height(48.dp))
                                TransportControls(
                                    state = state,
                                    onPlayPause = onPlayPause,
                                    onPrevious = onPrevious,
                                    onNext = onNext,
                                    onToggleShuffle = toggleShuffle,
                                    onCycleRepeat = cycleRepeat,
                                    onToggleFavorite = onToggleFavorite,
                                    onArtistClick = onArtistClick,
                                    onAlbumClick = onAlbumClick,
                                )
                            }
                        }
                    }
                    PlayerDetails(
                        item = current,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = gutter),
                    )
                    Spacer(Modifier.height(MaterialTheme.spacings.extraSmall))
                    PlayerSeekBar(
                        state = state,
                        onSeek = onSeek,
                        onClearError = onClearError,
                        modifier =
                            Modifier.padding(
                                start = gutter,
                                end = gutter,
                                bottom = MaterialTheme.spacings.medium,
                            ),
                    )
                }
            }
        }
        AnimatedVisibility(
            visible = overlayMessage != null,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 72.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xCC000000),
                contentColor = Color.White,
                shadowElevation = 6.dp,
            ) {
                Text(
                    text = overlayMessage.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                )
            }
        }
    }
}

@Composable
private fun PlayerTopBar(
    navigateBack: () -> Unit,
    onOpenQueue: () -> Unit,
    showQueueButton: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
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
        Spacer(modifier = Modifier.weight(1f))
        if (showQueueButton) {
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
}

@Composable
private fun MusicEntityLink(
    label: String,
    imageUri: String?,
    placeholder: Int,
) {
    if (label.isBlank()) return
    Row(
        modifier = Modifier.padding(vertical = MaterialTheme.spacings.extraSmall),
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
        Spacer(Modifier.width(MaterialTheme.spacings.small))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = PlayerOnBackgroundMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 220.dp),
        )
    }
}

@Composable
private fun AlbumArtwork(
    item: MusicPlaybackItem,
    artworkType: String,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
) {
    if (artworkType == "disc") {
        val rotation = remember { Animatable(0f) }
        LaunchedEffect(isPlaying) {
            if (isPlaying) {
                while (isActive) {
                    rotation.animateTo(
                        targetValue = rotation.value + 360f,
                        animationSpec = tween(durationMillis = 12_000, easing = LinearEasing),
                    )
                }
            }
        }

        BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
            val scrimSize = minOf(maxWidth, maxHeight) * 0.3f
            Box(
                modifier =
                    Modifier.fillMaxSize()
                        .graphicsLayer { rotationZ = rotation.value }
                        .clip(CircleShape)
                        .background(Color(0xFF242424))
                        .border(
                            width = 8.dp,
                            color = Color(0xFF242424),
                            shape = CircleShape,
                        ),
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
            Box(
                modifier =
                    Modifier.size(scrimSize)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
            )
            Box(
                modifier =
                    Modifier.size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.background)
            )
        }
    } else {
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
}

@Composable
private fun PlayerDetails(
    item: MusicPlaybackItem,
    modifier: Modifier = Modifier,
) {
    var overviewExpanded by remember(item.id) { mutableStateOf(false) }
    Column(modifier = modifier, horizontalAlignment = Alignment.Start) {
        MusicEntityLink(
            label = item.artist.orEmpty(),
            imageUri = item.artistImageUri,
            placeholder = CoreR.drawable.ic_user,
        )
        Spacer(Modifier.height(MaterialTheme.spacings.extraSmall))
        Text(
            text = item.title,
            style = MaterialTheme.typography.headlineSmall,
            color = PlayerOnBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
        item.overview
            ?.takeIf { it.isNotBlank() }
            ?.let { overview ->
                Spacer(Modifier.height(MaterialTheme.spacings.extraSmall))
                Text(
                    text = overview,
                    style = MaterialTheme.typography.bodySmall,
                    color = PlayerOnBackgroundMuted,
                    maxLines = if (overviewExpanded) Int.MAX_VALUE else 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier =
                        Modifier.fillMaxWidth().animateContentSize().clickable {
                            overviewExpanded = !overviewExpanded
                        },
                )
            }
        item.album?.let { album ->
            Spacer(Modifier.height(MaterialTheme.spacings.extraSmall))
            Text(
                text = album,
                style = MaterialTheme.typography.bodyMedium,
                color = PlayerOnBackgroundMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun TransportControls(
    state: MusicPlaybackState,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleFavorite: () -> Unit,
    onArtistClick: (java.util.UUID) -> Unit,
    onAlbumClick: (java.util.UUID) -> Unit,
    modifier: Modifier = Modifier,
) {
    val item = state.current
    val favorite = item?.favorite == true
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIconButton(
            onClick = onToggleFavorite,
            variant = AppButtonVariant.Ghost,
            content = {
                Icon(
                    painter =
                        painterResource(
                            if (favorite) CoreR.drawable.ic_heart_filled
                            else CoreR.drawable.ic_heart
                        ),
                    contentDescription = stringResource(CoreR.string.favorite_button_description),
                    tint = if (favorite) Red else PlayerOnBackgroundMuted,
                )
            },
        )
        AppIconButton(
            onClick = onToggleShuffle,
            variant = AppButtonVariant.Ghost,
            content = {
                Icon(
                    painter = painterResource(CoreR.drawable.ic_shuffle),
                    contentDescription = stringResource(FilmR.string.music_shuffle),
                    tint =
                        if (state.shuffleEnabled) PlayerOnBackground else PlayerOnBackgroundMuted,
                )
            },
        )
        AppIconButton(
            onClick = onPrevious,
            variant = AppButtonVariant.Ghost,
            content = {
                Icon(
                    painter = painterResource(CoreR.drawable.ic_skip_back),
                    contentDescription = stringResource(FilmR.string.music_previous),
                    tint = PlayerOnBackground,
                )
            },
        )
        Box(
            modifier =
                Modifier.size(AppButtonSize.Default.value)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable(onClick = onPlayPause),
            contentAlignment = Alignment.Center,
        ) {
            if (state.isBuffering) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = Color.Black,
                )
            } else {
                Icon(
                    painter =
                        painterResource(
                            if (state.isPlaying) CoreR.drawable.ic_pause else CoreR.drawable.ic_play
                        ),
                    contentDescription =
                        stringResource(
                            if (state.isPlaying) FilmR.string.music_pause else CoreR.string.play
                        ),
                    tint = Color.Black,
                )
            }
        }
        AppIconButton(
            onClick = onNext,
            variant = AppButtonVariant.Ghost,
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
        Box {
            AppIconButton(
                onClick = { menuExpanded = true },
                variant = AppButtonVariant.Ghost,
                content = {
                    Icon(
                        painter = painterResource(CoreR.drawable.ic_ellipsis),
                        contentDescription = stringResource(CoreR.string.more_options),
                        tint = PlayerOnBackgroundMuted,
                    )
                },
            )
            AppDropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                alignEnd = true,
            ) {
                MenuActionItem(
                    label = stringResource(FilmR.string.music_artist_label),
                    icon = CoreR.drawable.ic_user,
                    enabled = item?.artistId != null,
                    onClick = {
                        menuExpanded = false
                        item?.artistId?.let(onArtistClick)
                    },
                )
                MenuActionItem(
                    label = stringResource(FilmR.string.music_album_label),
                    icon = CoreR.drawable.ic_music,
                    enabled = item?.albumId != null,
                    onClick = {
                        menuExpanded = false
                        item?.albumId?.let(onAlbumClick)
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerSeekBar(
    state: MusicPlaybackState,
    onSeek: (Long) -> Unit,
    onClearError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var dragging by remember { mutableStateOf(false) }
    val sliderState = remember { SliderState(0f) }

    LaunchedEffect(state.positionMs, dragging) {
        if (!dragging && state.durationMs > 0L) {
            sliderState.value = (state.positionMs.toFloat() / state.durationMs).coerceIn(0f, 1f)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small),
        ) {
            Text(
                text = formatTime((sliderState.value * state.durationMs).toLong()),
                style = MaterialTheme.typography.bodySmall,
                color = PlayerOnBackgroundMuted,
            )
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
                modifier = Modifier.weight(1f),
            )
            Text(
                text = formatTime(state.durationMs),
                style = MaterialTheme.typography.bodySmall,
                color = PlayerOnBackgroundMuted,
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
