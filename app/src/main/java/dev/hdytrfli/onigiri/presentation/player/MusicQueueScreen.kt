package dev.hdytrfli.onigiri.presentation.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import dev.hdytrfli.onigiri.core.R as CoreR
import dev.hdytrfli.onigiri.feature.film.R as FilmR
import dev.hdytrfli.onigiri.player.local.music.MusicPlaybackItem
import dev.hdytrfli.onigiri.ui.components.AppButtonVariant
import dev.hdytrfli.onigiri.ui.components.AppDropdownMenu
import dev.hdytrfli.onigiri.ui.components.AppIconButton
import dev.hdytrfli.onigiri.ui.components.MenuActionItem
import dev.hdytrfli.onigiri.ui.components.PageScaffold
import dev.hdytrfli.onigiri.ui.components.PageTopBar
import dev.hdytrfli.onigiri.ui.theme.Dimens
import dev.hdytrfli.onigiri.ui.theme.spacings
import java.util.UUID
import kotlin.math.roundToInt

/** A regular page, in the same shape as the reader's Contents page. */
@Composable
fun MusicQueueScreen(
    queue: List<MusicPlaybackItem>,
    currentIndex: Int,
    navigateBack: () -> Unit,
    onPlayNow: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onMove: (fromIndex: Int, toIndex: Int) -> Unit,
) {
    val listState = rememberLazyListState()
    val latestQueue by rememberUpdatedState(queue)
    val latestMove by rememberUpdatedState(onMove)
    val density = LocalDensity.current
    val rowHeightPx = with(density) { 72.dp.toPx() }
    var draggingId by remember { mutableStateOf<UUID?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }

    PageScaffold(
        topBar = {
            PageTopBar(
                title = stringResource(FilmR.string.music_queue),
                onBack = navigateBack,
            )
        }
    ) { innerPadding ->
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .padding(innerPadding)
                    .padding(all = MaterialTheme.spacings.gutter),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (queue.isEmpty()) {
                Text(
                    text = stringResource(FilmR.string.music_empty_queue),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Card(
                    modifier =
                        Modifier.widthIn(max = Dimens.contentMaxWidth).fillMaxWidth().weight(1f),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ),
                ) {
                    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                        itemsIndexed(queue, key = { _, item -> item.id }) { index, item ->
                            val isDragging = draggingId == item.id
                            QueueRow(
                                item = item,
                                index = index,
                                currentIndex = currentIndex,
                                onPlayNow = { onPlayNow(index) },
                                onPlayNext = {
                                    val target = (currentIndex + 1).coerceIn(0, queue.lastIndex)
                                    if (target != index) onMove(index, target)
                                },
                                onPlayEnd = {
                                    if (index != queue.lastIndex) onMove(index, queue.lastIndex)
                                },
                                onRemove = { onRemove(index) },
                                onDragStart = {
                                    draggingId = item.id
                                    dragOffset = 0f
                                },
                                onDrag = { amount ->
                                    dragOffset += amount
                                    val from = latestQueue.indexOfFirst { it.id == item.id }
                                    if (from >= 0) {
                                        val shift = (dragOffset / rowHeightPx).roundToInt()
                                        val to = (from + shift).coerceIn(0, latestQueue.lastIndex)
                                        if (to != from) {
                                            latestMove(from, to)
                                            dragOffset -= (to - from) * rowHeightPx
                                        }
                                    }
                                },
                                onDragEnd = {
                                    draggingId = null
                                    dragOffset = 0f
                                },
                                modifier =
                                    if (isDragging) {
                                        Modifier.zIndex(1f).offset {
                                            IntOffset(0, dragOffset.roundToInt())
                                        }
                                    } else {
                                        Modifier.animateItem()
                                    },
                                trailingDivider = index < queue.lastIndex,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QueueRow(
    item: MusicPlaybackItem,
    index: Int,
    currentIndex: Int,
    onPlayNow: () -> Unit,
    onPlayNext: () -> Unit,
    onPlayEnd: () -> Unit,
    onRemove: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier,
    trailingDivider: Boolean,
) {
    val latestDragStart by rememberUpdatedState(onDragStart)
    val latestDrag by rememberUpdatedState(onDrag)
    val latestDragEnd by rememberUpdatedState(onDragEnd)
    val isCurrent = index == currentIndex

    Column(
        modifier = modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(
            modifier =
                Modifier.fillMaxWidth()
                    .height(72.dp)
                    .background(
                        if (isCurrent) MaterialTheme.colorScheme.surfaceContainerHigh
                        else MaterialTheme.colorScheme.surfaceContainer
                    )
                    .pointerInput(item.id) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { latestDragStart() },
                            onDrag = { change, amount ->
                                change.consume()
                                latestDrag(amount.y)
                            },
                            onDragEnd = { latestDragEnd() },
                            onDragCancel = { latestDragEnd() },
                        )
                    }
                    .clickable(onClick = onPlayNow)
                    .padding(start = MaterialTheme.spacings.small),
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

            Column(
                modifier = Modifier.weight(1f).padding(horizontal = MaterialTheme.spacings.small)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isCurrent) {
                        Icon(
                            painter = painterResource(CoreR.drawable.ic_play),
                            contentDescription = stringResource(FilmR.string.music_now_playing),
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(6.dp))
                    }
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color =
                            if (isCurrent) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = listOfNotNull(item.artist, item.album).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            QueueMenu(
                isCurrent = isCurrent,
                onPlayNow = onPlayNow,
                onPlayNext = onPlayNext,
                onPlayEnd = onPlayEnd,
                onRemove = onRemove,
            )
        }

        if (trailingDivider) {
            HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.2f))
        }
    }
}

@Composable
private fun QueueMenu(
    isCurrent: Boolean,
    onPlayNow: () -> Unit,
    onPlayNext: () -> Unit,
    onPlayEnd: () -> Unit,
    onRemove: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        AppIconButton(
            onClick = { expanded = true },
            variant = AppButtonVariant.Ghost,
            content = {
                Icon(
                    painter = painterResource(CoreR.drawable.ic_ellipsis),
                    contentDescription = stringResource(CoreR.string.more_options),
                )
            },
        )
        AppDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            alignEnd = true,
        ) {
            MenuActionItem(
                label = stringResource(FilmR.string.music_play_now),
                icon = CoreR.drawable.ic_play,
                enabled = !isCurrent,
                onClick = {
                    expanded = false
                    onPlayNow()
                },
            )
            MenuActionItem(
                label = stringResource(FilmR.string.music_play_next),
                icon = CoreR.drawable.ic_skip_forward,
                onClick = {
                    expanded = false
                    onPlayNext()
                },
            )
            MenuActionItem(
                label = stringResource(FilmR.string.music_play_end),
                icon = CoreR.drawable.ic_arrow_down_to_line,
                onClick = {
                    expanded = false
                    onPlayEnd()
                },
            )
            MenuActionItem(
                label = stringResource(FilmR.string.music_remove_from_queue),
                icon = CoreR.drawable.ic_x,
                onClick = {
                    expanded = false
                    onRemove()
                },
            )
        }
    }
}
