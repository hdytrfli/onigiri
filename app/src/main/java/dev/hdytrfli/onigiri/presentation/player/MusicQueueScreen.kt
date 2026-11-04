package dev.hdytrfli.onigiri.presentation.player

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.hdytrfli.onigiri.core.R as CoreR
import dev.hdytrfli.onigiri.feature.film.R as FilmR
import dev.hdytrfli.onigiri.feature.film.presentation.components.ItemCountBadge
import dev.hdytrfli.onigiri.feature.film.presentation.components.MusicRowAction
import dev.hdytrfli.onigiri.feature.film.presentation.components.MusicTrackRow
import dev.hdytrfli.onigiri.feature.film.presentation.components.SectionHeader
import dev.hdytrfli.onigiri.player.local.music.MusicPlaybackItem
import dev.hdytrfli.onigiri.ui.components.PageScaffold
import dev.hdytrfli.onigiri.ui.components.PageTopBar
import dev.hdytrfli.onigiri.ui.theme.Dimens
import dev.hdytrfli.onigiri.ui.theme.Red
import dev.hdytrfli.onigiri.ui.theme.spacings
import java.util.UUID
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

/** A regular page, in the same shape as the reader's Contents page. */
@Composable
fun MusicQueueScreen(
    queue: List<MusicPlaybackItem>,
    currentIndex: Int,
    navigateBack: () -> Unit,
    onPlayNow: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onMove: (fromIndex: Int, toIndex: Int) -> Unit,
    onToggleFavorite: (UUID) -> Unit,
) {
    PageScaffold(
        topBar = {
            PageTopBar(
                title = stringResource(FilmR.string.music_queue),
                onBack = navigateBack,
            )
        }
    ) { innerPadding ->
        MusicQueueContent(
            queue = queue,
            currentIndex = currentIndex,
            onPlayNow = onPlayNow,
            onRemove = onRemove,
            onMove = onMove,
            onToggleFavorite = onToggleFavorite,
            modifier =
                Modifier.fillMaxSize()
                    .padding(innerPadding)
                    .padding(all = MaterialTheme.spacings.gutter),
        )
    }
}

/** Queue contents shared by the full queue page and the landscape player's right column. */
@Composable
fun MusicQueueContent(
    queue: List<MusicPlaybackItem>,
    currentIndex: Int,
    onPlayNow: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onMove: (fromIndex: Int, toIndex: Int) -> Unit,
    onToggleFavorite: (UUID) -> Unit,
    modifier: Modifier = Modifier,
    contentBottomPadding: androidx.compose.ui.unit.Dp = 0.dp,
    header: (@Composable () -> Unit)? = null,
) {
    val listState = rememberLazyListState()
    var displayQueue by remember { mutableStateOf(queue) }
    val latestDisplayQueue by rememberUpdatedState(displayQueue)
    val latestCurrentIndex by rememberUpdatedState(currentIndex)
    val latestOnMove by rememberUpdatedState(onMove)

    LaunchedEffect(queue) {
        if (displayQueue.map { it.id } != queue.map { it.id }) {
            displayQueue = queue
        } else if (displayQueue != queue) {
            val updatedById = queue.associateBy { it.id }
            displayQueue = displayQueue.map { updatedById[it.id] ?: it }
        }
    }

    val reorderableState =
        rememberReorderableLazyListState(listState) { from, to ->
            val fromId = from.key as? UUID ?: return@rememberReorderableLazyListState
            val toId = to.key as? UUID ?: return@rememberReorderableLazyListState
            val current = latestDisplayQueue
            val fromIndex = current.indexOfFirst { it.id == fromId }
            val toIndex = current.indexOfFirst { it.id == toId }
            val firstUpNext = latestCurrentIndex + 1
            if (fromIndex < firstUpNext || toIndex < firstUpNext) {
                return@rememberReorderableLazyListState
            }

            displayQueue = current.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
            latestOnMove(fromIndex, toIndex)
        }

    val safeCurrentIndex = currentIndex.coerceIn(0, displayQueue.lastIndex.coerceAtLeast(0))
    val played =
        if (displayQueue.isEmpty()) {
            emptyList()
        } else {
            (safeCurrentIndex downTo 0).map { it to displayQueue[it] }
        }
    val upNext =
        if (displayQueue.isEmpty()) {
            emptyList()
        } else {
            ((safeCurrentIndex + 1) until displayQueue.size).map { it to displayQueue[it] }
        }

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        if (displayQueue.isEmpty()) {
            Text(
                text = stringResource(FilmR.string.music_empty_queue),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.widthIn(max = Dimens.contentMaxWidth).fillMaxWidth().weight(1f),
                contentPadding =
                    androidx.compose.foundation.layout.PaddingValues(bottom = contentBottomPadding),
            ) {
                if (header != null) {
                    item(key = "player-header") { header() }
                }
                item(key = "played-header") {
                    SectionHeader(
                        title = stringResource(FilmR.string.music_played),
                        modifier = Modifier.padding(horizontal = MaterialTheme.spacings.medium),
                        trailingContent = { ItemCountBadge(count = played.size) },
                    )
                }
                itemsIndexed(played, key = { _, entry -> entry.second.id }) { _, entry ->
                    val index = entry.first
                    val item = entry.second
                    QueueRow(
                        item = item,
                        index = index,
                        currentIndex = currentIndex,
                        dragging = false,
                        onPlayNow = { onPlayNow(index) },
                        onPlayNext = {
                            val target = (currentIndex + 1).coerceIn(0, displayQueue.lastIndex)
                            if (target != index) onMove(index, target)
                        },
                        onPlayEnd = {
                            if (index != displayQueue.lastIndex) {
                                onMove(index, displayQueue.lastIndex)
                            }
                        },
                        onRemove = { onRemove(index) },
                        onToggleFavorite = { onToggleFavorite(item.id) },
                        trailingDivider = index > 0,
                    )
                }
                item(key = "up-next-header") {
                    SectionHeader(
                        title = stringResource(FilmR.string.music_up_next),
                        modifier =
                            Modifier.padding(
                                start = MaterialTheme.spacings.medium,
                                end = MaterialTheme.spacings.medium,
                                top = MaterialTheme.spacings.medium,
                            ),
                        trailingContent = { ItemCountBadge(count = upNext.size) },
                    )
                }
                if (upNext.isEmpty()) {
                    item(key = "up-next-empty") {
                        Text(
                            text = stringResource(FilmR.string.music_empty_queue),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier =
                                Modifier.padding(
                                    horizontal = MaterialTheme.spacings.medium,
                                    vertical = MaterialTheme.spacings.small,
                                ),
                        )
                    }
                } else {
                    itemsIndexed(upNext, key = { _, entry -> entry.second.id }) { _, entry ->
                        val index = entry.first
                        val item = entry.second
                        ReorderableItem(reorderableState, key = item.id) { isDragging ->
                            QueueRow(
                                item = item,
                                index = index,
                                currentIndex = currentIndex,
                                dragging = isDragging,
                                onPlayNow = { onPlayNow(index) },
                                onPlayNext = {
                                    val target =
                                        (currentIndex + 1).coerceIn(0, displayQueue.lastIndex)
                                    if (target != index) onMove(index, target)
                                },
                                onPlayEnd = {
                                    if (index != displayQueue.lastIndex) {
                                        onMove(index, displayQueue.lastIndex)
                                    }
                                },
                                onRemove = { onRemove(index) },
                                onToggleFavorite = { onToggleFavorite(item.id) },
                                modifier = Modifier.longPressDraggableHandle(),
                                trailingDivider = index < displayQueue.lastIndex,
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
    dragging: Boolean,
    onPlayNow: () -> Unit,
    onPlayNext: () -> Unit,
    onPlayEnd: () -> Unit,
    onRemove: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
    trailingDivider: Boolean,
) {
    val isCurrent = index == currentIndex
    MusicTrackRow(
        title = item.title,
        subtitle = listOfNotNull(item.artist, item.album).joinToString(" · "),
        imageUri = item.imageUri,
        isCurrent = isCurrent,
        actions =
            listOf(
                MusicRowAction(
                    label = stringResource(FilmR.string.music_play_now),
                    icon = CoreR.drawable.ic_play,
                    enabled = !isCurrent,
                    onClick = onPlayNow,
                ),
                MusicRowAction(
                    label = stringResource(FilmR.string.music_play_next),
                    icon = CoreR.drawable.ic_skip_forward,
                    onClick = onPlayNext,
                ),
                MusicRowAction(
                    label = stringResource(FilmR.string.music_play_end),
                    icon = CoreR.drawable.ic_arrow_down_to_line,
                    onClick = onPlayEnd,
                ),
                MusicRowAction(
                    label =
                        stringResource(
                            if (item.favorite) CoreR.string.remove_from_favorites
                            else CoreR.string.add_to_favorites
                        ),
                    icon =
                        if (item.favorite) CoreR.drawable.ic_heart_filled
                        else CoreR.drawable.ic_heart,
                    iconTint = if (item.favorite) Red else null,
                    dismissOnClick = false,
                    onClick = onToggleFavorite,
                ),
                MusicRowAction(
                    label = stringResource(FilmR.string.music_remove_from_queue),
                    icon = CoreR.drawable.ic_x,
                    onClick = onRemove,
                ),
            ),
        onClick = onPlayNow,
        modifier = modifier,
        dragging = dragging,
        trailingDivider = trailingDivider,
    )
}
