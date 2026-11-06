package dev.hdytrfli.onigiri.presentation.player

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.hdytrfli.onigiri.core.R as CoreR
import dev.hdytrfli.onigiri.feature.film.R as FilmR
import dev.hdytrfli.onigiri.feature.film.presentation.components.OverviewText
import dev.hdytrfli.onigiri.player.local.R as PlayerR
import dev.hdytrfli.onigiri.player.local.music.MusicPlaybackItem
import dev.hdytrfli.onigiri.player.local.music.MusicPlaybackState
import dev.hdytrfli.onigiri.player.local.music.MusicRepeatMode
import dev.hdytrfli.onigiri.ui.components.AppButtonSize
import dev.hdytrfli.onigiri.ui.components.AppButtonVariant
import dev.hdytrfli.onigiri.ui.components.AppDropdownMenu
import dev.hdytrfli.onigiri.ui.components.AppIconButton
import dev.hdytrfli.onigiri.ui.components.MenuActionItem
import dev.hdytrfli.onigiri.ui.components.ReservedWidthText
import dev.hdytrfli.onigiri.ui.components.preciseSeekable
import dev.hdytrfli.onigiri.ui.theme.Red
import dev.hdytrfli.onigiri.ui.theme.spacings
import dev.hdytrfli.onigiri.ui.utils.rememberSafePadding
import kotlin.math.abs
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

private val PlayerBackgroundTop = Color(0xFF181818)
private val PlayerOnBackground = Color.White
private val PlayerOnBackgroundMuted = Color(0xB3FFFFFF)

@Composable
fun MusicPlayerScreen(
    state: MusicPlaybackState,
    artworkType: String,
    swipeArtwork: Boolean,
    preciseSeeking: Boolean,
    navigateBack: () -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSpeedBoost: (Boolean) -> Unit,
    onSeek: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleFavoriteItem: (java.util.UUID) -> Unit,
    onOpenQueue: () -> Unit,
    onArtistClick: (java.util.UUID) -> Unit,
    onAlbumClick: (java.util.UUID) -> Unit,
    onPlayQueueItem: (Int) -> Unit,
    onJumpQueueItem: (Int) -> Unit,
    onRemoveQueueItem: (Int) -> Unit,
    onMoveQueueItem: (fromIndex: Int, toIndex: Int) -> Unit,
    onClearError: () -> Unit,
) {
    var overlayMessage by remember { mutableStateOf<String?>(null) }
    var displayedOverlayMessage by remember { mutableStateOf("") }

    val shuffleOn = stringResource(FilmR.string.music_shuffle_on)
    val shuffleOff = stringResource(FilmR.string.music_shuffle_off)
    val shuffleUnavailable = stringResource(FilmR.string.music_shuffle_unavailable)
    val repeatOff = stringResource(FilmR.string.music_repeat_off)
    val repeatAll = stringResource(FilmR.string.music_repeat_all)
    val repeatOne = stringResource(FilmR.string.music_repeat_one)
    val addFavorite = stringResource(CoreR.string.add_to_favorites)
    val removeFavorite = stringResource(CoreR.string.remove_from_favorites)
    val speedUp = stringResource(PlayerR.string.player_speed_up)

    LaunchedEffect(overlayMessage) {
        if (overlayMessage != null) {
            displayedOverlayMessage = overlayMessage.orEmpty()
            delay(1_500)
            overlayMessage = null
        }
    }

    val safePadding = rememberSafePadding()
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val current = state.current
    val discRotation = remember { mutableFloatStateOf(0f) }
    val rotationSpeed = remember { mutableFloatStateOf(1f) }
    LaunchedEffect(state.playWhenReady) {
        if (!state.playWhenReady) return@LaunchedEffect
        var lastFrame = 0L
        while (isActive) {
            withFrameNanos { now ->
                if (lastFrame != 0L) {
                    val seconds = ((now - lastFrame) / 1_000_000_000f).coerceAtMost(0.1f)
                    discRotation.floatValue =
                        (discRotation.floatValue +
                            NORMAL_DISC_DEGREES_PER_SECOND * rotationSpeed.floatValue * seconds) %
                            360f
                }
                lastFrame = now
            }
        }
    }
    val toggleShuffle: () -> Unit = {
        if (state.queue.size <= 1) {
            overlayMessage = shuffleUnavailable
        } else {
            overlayMessage = if (state.shuffleEnabled) shuffleOff else shuffleOn
            onToggleShuffle()
        }
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
    val toggleFavorite: () -> Unit = {
        overlayMessage = if (state.current?.favorite == true) removeFavorite else addFavorite
        onToggleFavorite()
    }
    val gutter = MaterialTheme.spacings.gutter
    val artworkHorizontalGutter =
        if (LocalConfiguration.current.smallestScreenWidthDp >= 600) 48.dp else 24.dp
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
                                )
                                .coerceAtLeast(0.dp)
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center,
                            ) {
                                PlayerArtwork(
                                    state = state,
                                    artworkType = artworkType,
                                    swipeEnabled = swipeArtwork,
                                    artSize = artSize,
                                    horizontalGutter = artworkHorizontalGutter,
                                    onPrevious = onPrevious,
                                    onNext = onNext,
                                    onSpeedBoost = { active ->
                                        overlayMessage = if (active) speedUp else null
                                        onSpeedBoost(active)
                                    },
                                    discRotation = discRotation,
                                    rotationSpeed = rotationSpeed,
                                    modifier = Modifier.fillMaxWidth().height(artSize),
                                )
                            }
                            TransportControls(
                                state = state,
                                onPlayPause = onPlayPause,
                                onPrevious = onPrevious,
                                onNext = onNext,
                                onToggleShuffle = toggleShuffle,
                                onCycleRepeat = cycleRepeat,
                                onToggleFavorite = toggleFavorite,
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
                                    .padding(horizontal = 24.dp, vertical = 48.dp)
                        ) {
                            PlayerDetails(item = current)
                            Spacer(Modifier.height(MaterialTheme.spacings.medium))
                            PlayerSeekBar(
                                state = state,
                                preciseSeeking = preciseSeeking,
                                onSeek = onSeek,
                                onClearError = onClearError,
                            )
                        }
                    }
                } else {
                    MusicQueueContent(
                        queue = state.queue,
                        currentIndex = state.currentIndex,
                        onPlayNow = onPlayQueueItem,
                        onJumpToItem = onJumpQueueItem,
                        onRemove = onRemoveQueueItem,
                        onMove = onMoveQueueItem,
                        onToggleFavorite = onToggleFavoriteItem,
                        isShuffling = state.isShuffling,
                        modifier = Modifier.weight(0.5f).fillMaxHeight().padding(end = gutter),
                        contentBottomPadding = MaterialTheme.spacings.gutter,
                        header = {
                            Column(
                                modifier =
                                    Modifier.fillMaxWidth()
                                        .padding(
                                            start = MaterialTheme.spacings.gutter,
                                            end = MaterialTheme.spacings.gutter,
                                            top = 48.dp,
                                            bottom = MaterialTheme.spacings.medium,
                                        )
                            ) {
                                PlayerDetails(item = current)
                                Spacer(Modifier.height(MaterialTheme.spacings.medium))
                                PlayerSeekBar(
                                    state = state,
                                    preciseSeeking = preciseSeeking,
                                    onSeek = onSeek,
                                    onClearError = onClearError,
                                )
                            }
                        },
                    )
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
                                    )
                                    .coerceAtLeast(0.dp)
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    PlayerArtwork(
                                        state = state,
                                        artworkType = artworkType,
                                        swipeEnabled = swipeArtwork,
                                        artSize = artSize,
                                        horizontalGutter = artworkHorizontalGutter,
                                        onPrevious = onPrevious,
                                        onNext = onNext,
                                        onSpeedBoost = { active ->
                                            overlayMessage = if (active) speedUp else null
                                            onSpeedBoost(active)
                                        },
                                        discRotation = discRotation,
                                        rotationSpeed = rotationSpeed,
                                        modifier = Modifier.fillMaxWidth().height(artSize),
                                    )
                                }
                                TransportControls(
                                    state = state,
                                    onPlayPause = onPlayPause,
                                    onPrevious = onPrevious,
                                    onNext = onNext,
                                    onToggleShuffle = toggleShuffle,
                                    onCycleRepeat = cycleRepeat,
                                    onToggleFavorite = toggleFavorite,
                                    onArtistClick = onArtistClick,
                                    onAlbumClick = onAlbumClick,
                                )
                            }
                        }
                    }
                    PlayerDetails(
                        item = current,
                        modifier =
                            Modifier.fillMaxWidth()
                                .padding(horizontal = MaterialTheme.spacings.gutter),
                    )
                    Spacer(Modifier.height(MaterialTheme.spacings.extraSmall))
                    PlayerSeekBar(
                        state = state,
                        preciseSeeking = preciseSeeking,
                        onSeek = onSeek,
                        onClearError = onClearError,
                        modifier =
                            Modifier.padding(
                                start = MaterialTheme.spacings.gutter,
                                end = MaterialTheme.spacings.gutter,
                                bottom = MaterialTheme.spacings.medium,
                            ),
                    )
                }
            }
        }
        AnimatedVisibility(
            visible = overlayMessage != null,
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(150)),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 72.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xCC000000),
                contentColor = Color.White,
                shadowElevation = 6.dp,
            ) {
                Text(
                    text = displayedOverlayMessage,
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
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
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
private fun PlayerArtwork(
    state: MusicPlaybackState,
    artworkType: String,
    swipeEnabled: Boolean,
    artSize: Dp,
    horizontalGutter: Dp,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSpeedBoost: (Boolean) -> Unit,
    discRotation: MutableFloatState,
    rotationSpeed: MutableFloatState,
    modifier: Modifier = Modifier,
) {
    val current = state.current ?: return
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val stepPx = with(density) { artSize.toPx() + 16.dp.toPx() }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var visualIndex by remember { mutableIntStateOf(state.currentIndex.coerceAtLeast(0)) }
    val currentId = current.id
    var visualItemId by remember { mutableStateOf(currentId) }
    var settleJob by remember { mutableStateOf<Job?>(null) }
    var rotationSpeedJob by remember { mutableStateOf<Job?>(null) }
    var boostActive by remember { mutableStateOf(false) }
    var swipeActive by remember { mutableStateOf(false) }
    val dragVelocityTracker = remember { VelocityTracker() }

    val settle: () -> Unit = {
        settleJob?.cancel()
        val start = dragOffset
        settleJob = scope.launch {
            animate(start, 0f, animationSpec = tween(260)) { value, _ -> dragOffset = value }
        }
    }

    val stopRotationBoost: () -> Unit = {
        if (boostActive) {
            boostActive = false
            rotationSpeedJob?.cancel()
            rotationSpeedJob = scope.launch {
                animate(
                    rotationSpeed.floatValue,
                    1f,
                    animationSpec = tween(400, easing = LinearEasing),
                ) { value, _ ->
                    rotationSpeed.floatValue = value
                }
            }
        }
    }

    val startRotationBoost: () -> Unit = {
        if (!boostActive) {
            boostActive = true
            rotationSpeedJob?.cancel()
            rotationSpeedJob = scope.launch {
                animate(
                    rotationSpeed.floatValue,
                    4f,
                    animationSpec = tween(250, easing = LinearEasing),
                ) { value, _ ->
                    rotationSpeed.floatValue = value
                }
            }
        }
    }

    LaunchedEffect(artSize) {
        settleJob?.cancel()
        rotationSpeedJob?.cancel()
        dragOffset = 0f
        visualIndex = state.currentIndex.coerceAtLeast(0)
        visualItemId = state.current?.id ?: visualItemId
        rotationSpeed.floatValue = 1f
        boostActive = false
    }

    LaunchedEffect(state.current?.id, state.currentIndex) {
        val actualIndex = state.currentIndex.coerceAtLeast(0)
        val actualItem = state.current ?: return@LaunchedEffect
        if (actualItem.id == visualItemId) {
            // Shuffle/reorder changed the index but the same track is still playing. Keep the
            // artwork exactly where it is instead of sliding it out and back in.
            if (dragOffset == 0f) {
                settleJob?.cancel()
                visualIndex = actualIndex
            }
            return@LaunchedEffect
        }
        val direction = if (actualIndex > visualIndex) 1 else -1
        visualIndex = actualIndex
        visualItemId = actualItem.id
        dragOffset = direction * stepPx
        settle()
    }

    val queue = state.queue
    val restingIndex =
        queue.indexOfFirst { it.id == visualItemId }.takeIf { it >= 0 }
            ?: state.currentIndex.coerceAtLeast(0)
    val renderIndex = if (abs(dragOffset) < 0.5f) restingIndex else visualIndex
    val visualCurrent = queue.getOrNull(renderIndex) ?: current
    val previous = queue.getOrNull(renderIndex - 1)
    val next = queue.getOrNull(renderIndex + 1)
    val previousOffset = dragOffset - stepPx
    val nextOffset = dragOffset + stepPx
    val currentAlpha = (1f - abs(dragOffset) / stepPx).coerceIn(0f, 1f)
    val previousAlpha = (1f - abs(previousOffset) / stepPx).coerceIn(0f, 1f)
    val nextAlpha = (1f - abs(nextOffset) / stepPx).coerceIn(0f, 1f)

    Box(
        modifier =
            modifier
                .clipToBounds()
                .pointerInput(Unit) {
                    val holdMs = viewConfiguration.longPressTimeoutMillis
                    detectTapGestures(
                        onPress = {
                            val releasedEarly = withTimeoutOrNull(holdMs) { tryAwaitRelease() }
                            if (releasedEarly == null && !swipeActive) {
                                onSpeedBoost(true)
                                startRotationBoost()
                                tryAwaitRelease()
                                stopRotationBoost()
                                onSpeedBoost(false)
                            }
                        }
                    )
                }
                .pointerInput(
                    swipeEnabled,
                    state.hasNext,
                    state.hasPrevious,
                    queue.size,
                    stepPx,
                ) {
                    if (!swipeEnabled || queue.size <= 1) return@pointerInput
                    detectHorizontalDragGestures(
                        onDragStart = {
                            settleJob?.cancel()
                            swipeActive = true
                            dragVelocityTracker.resetTracking()
                            stopRotationBoost()
                            onSpeedBoost(false)
                            dragOffset = 0f
                        },
                        onHorizontalDrag = { change, amount ->
                            change.consume()
                            dragVelocityTracker.addPosition(
                                change.uptimeMillis,
                                change.position,
                            )
                            dragOffset = (dragOffset + amount).coerceIn(-stepPx, stepPx)
                        },
                        onDragEnd = {
                            // A swipe either passes 15% of the page or is a deliberate flick.
                            // The velocity path matters on compact screens where 15% is a short
                            // physical distance and a quick flick should still change tracks.
                            val velocity = dragVelocityTracker.calculateVelocity().x
                            val flingThreshold = with(density) { 800.dp.toPx() }
                            val flung = abs(velocity) >= flingThreshold
                            val direction =
                                when {
                                    flung -> if (velocity < 0f) -1 else 1
                                    abs(dragOffset) >= stepPx * 0.15f ->
                                        if (dragOffset < 0f) -1 else 1
                                    else -> 0
                                }
                            val target =
                                when {
                                    direction < 0 && state.hasNext -> 1
                                    direction > 0 && state.hasPrevious -> -1
                                    else -> 0
                                }
                            if (target != 0) {
                                val newIndex = (visualIndex + target).coerceIn(0, queue.lastIndex)
                                visualIndex = newIndex
                                visualItemId = queue.getOrNull(newIndex)?.id ?: visualItemId
                                dragOffset += target * stepPx
                                if (target > 0) onNext() else onPrevious()
                            }
                            settle()
                            swipeActive = false
                        },
                        onDragCancel = {
                            swipeActive = false
                            settle()
                        },
                    )
                },
        contentAlignment = Alignment.Center,
    ) {
        previous?.let {
            ArtworkPage(
                item = it,
                artworkType = artworkType,
                artSize = artSize,
                horizontalPadding = horizontalGutter,
                offsetPx = previousOffset,
                alpha = previousAlpha,
                discRotation = discRotation,
            )
        }
        next?.let {
            ArtworkPage(
                item = it,
                artworkType = artworkType,
                artSize = artSize,
                horizontalPadding = horizontalGutter,
                offsetPx = nextOffset,
                alpha = nextAlpha,
                discRotation = discRotation,
            )
        }
        ArtworkPage(
            item = visualCurrent,
            artworkType = artworkType,
            artSize = artSize,
            horizontalPadding = horizontalGutter,
            offsetPx = dragOffset,
            alpha = currentAlpha,
            discRotation = discRotation,
        )
    }
}

@Composable
private fun ArtworkPage(
    item: MusicPlaybackItem,
    artworkType: String,
    artSize: Dp,
    horizontalPadding: Dp,
    offsetPx: Float,
    alpha: Float = 1f,
    discRotation: MutableFloatState,
) {
    Box(
        modifier =
            Modifier.fillMaxSize().graphicsLayer {
                translationX = offsetPx
                this.alpha = alpha
            },
        contentAlignment = Alignment.Center,
    ) {
        AlbumArtwork(
            item = item,
            artworkType = artworkType,
            discRotation = discRotation,
            modifier =
                Modifier.fillMaxSize()
                    .padding(
                        start = horizontalPadding,
                        end = horizontalPadding,
                        top = horizontalPadding,
                        bottom = 48.dp,
                    ),
        )
    }
}

@Composable
private fun AlbumArtwork(
    item: MusicPlaybackItem,
    artworkType: String,
    discRotation: MutableFloatState,
    modifier: Modifier = Modifier,
) {
    if (artworkType == "disc") {

        BoxWithConstraints(
            modifier = modifier.aspectRatio(1f),
            contentAlignment = Alignment.Center,
        ) {
            val discSize = minOf(maxWidth, maxHeight)
            val scrimSize = discSize * 0.3f
            Box(
                modifier =
                    Modifier.size(discSize)
                        .aspectRatio(1f)
                        .graphicsLayer { rotationZ = discRotation.floatValue }
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
            modifier =
                modifier
                    .aspectRatio(1f)
                    .clip(MaterialTheme.shapes.large)
                    .background(Color(0xFF242424)),
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
                androidx.compose.runtime.key(item.id) {
                    OverviewText(
                        text = overview,
                        maxCollapsedLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
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

    BoxWithConstraints(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val buttonCount = 7
        val gap =
            ((maxWidth - AppButtonSize.Default.value * buttonCount) / (buttonCount - 1)).coerceIn(
                4.dp,
                32.dp,
            )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(gap, Alignment.CenterHorizontally),
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
                        contentDescription =
                            stringResource(CoreR.string.favorite_button_description),
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
                            when {
                                state.queue.size <= 1 -> PlayerOnBackgroundMuted.copy(alpha = 0.4f)
                                state.shuffleEnabled -> PlayerOnBackground
                                else -> PlayerOnBackgroundMuted
                            },
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
                                if (state.isPlaying) CoreR.drawable.ic_pause
                                else CoreR.drawable.ic_play
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerSeekBar(
    state: MusicPlaybackState,
    preciseSeeking: Boolean,
    onSeek: (Long) -> Unit,
    onClearError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var dragging by remember { mutableStateOf(false) }
    var dragMoved by remember { mutableStateOf(false) }
    var animatingSeek by remember { mutableStateOf(false) }
    var pendingSeekTarget by remember { mutableStateOf<Long?>(null) }
    val sliderState = remember { SliderState(0f) }
    val scope = rememberCoroutineScope()
    val latestState by rememberUpdatedState(state)

    LaunchedEffect(state.positionMs, dragging, animatingSeek, pendingSeekTarget) {
        val target = pendingSeekTarget
        if (target != null && abs(state.positionMs - target) <= 1_000L) {
            pendingSeekTarget = null
        }
        if (!dragging && !animatingSeek && pendingSeekTarget == null && state.durationMs > 0L) {
            sliderState.value = (state.positionMs.toFloat() / state.durationMs).coerceIn(0f, 1f)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small),
        ) {
            ReservedWidthText(
                text =
                    formatTime((sliderState.value * state.durationMs).toLong(), state.durationMs),
                referenceText = formatTime(state.durationMs, state.durationMs),
                style = MaterialTheme.typography.bodySmall,
                color = PlayerOnBackgroundMuted,
            )
            Box(modifier = Modifier.weight(1f)) {
                Slider(
                    state = sliderState,
                    onValueChange = {
                        if (dragging) dragMoved = true
                        dragging = true
                        sliderState.value = it
                    },
                    onValueChangeFinished = {
                        dragging = false
                        val target = sliderState.value
                        if (state.durationMs > 0L) {
                            val targetMs = (target * state.durationMs).toLong()
                            if (!dragMoved) {
                                val start =
                                    (state.positionMs.toFloat() / state.durationMs).coerceIn(0f, 1f)
                                sliderState.value = start
                                animatingSeek = true
                                scope.launch {
                                    animate(start, target, animationSpec = tween(180)) { value, _ ->
                                        sliderState.value = value
                                    }
                                    animatingSeek = false
                                }
                            }
                            pendingSeekTarget = targetMs
                            onSeek(targetMs)
                        }
                        dragMoved = false
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
                    modifier = Modifier.fillMaxWidth(),
                )
                if (preciseSeeking && state.durationMs > 0L) {
                    Box(
                        modifier =
                            Modifier.matchParentSize()
                                .preciseSeekable(
                                    enabled = true,
                                    startFraction = {
                                        (latestState.positionMs.toFloat() / latestState.durationMs)
                                            .coerceIn(0f, 1f)
                                    },
                                    onScrub = { fraction ->
                                        dragging = true
                                        sliderState.value = fraction
                                        val targetMs = (fraction * latestState.durationMs).toLong()
                                        pendingSeekTarget = targetMs
                                        onSeek(targetMs)
                                    },
                                    onScrubEnd = { fraction ->
                                        dragging = false
                                        val targetMs = (fraction * latestState.durationMs).toLong()
                                        pendingSeekTarget = targetMs
                                        onSeek(targetMs)
                                    },
                                    onTap = { fraction ->
                                        val targetMs = (fraction * latestState.durationMs).toLong()
                                        pendingSeekTarget = targetMs
                                        onSeek(targetMs)
                                    },
                                )
                    )
                }
            }
            Text(
                text = formatTime(state.durationMs, state.durationMs),
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

private const val NORMAL_DISC_DEGREES_PER_SECOND = 30f

private fun formatTime(ms: Long, totalMs: Long): String {
    val totalSeconds = (ms / 1000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (totalMs >= 3_600_000L) {
        "%02d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(totalSeconds / 60, seconds)
    }
}
