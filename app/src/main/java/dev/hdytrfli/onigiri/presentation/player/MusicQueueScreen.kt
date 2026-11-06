package dev.hdytrfli.onigiri.presentation.player

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
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
import dev.hdytrfli.onigiri.ui.components.AppButtonVariant
import dev.hdytrfli.onigiri.ui.components.AppIconButton
import dev.hdytrfli.onigiri.ui.components.LocalRootPageContentInset
import dev.hdytrfli.onigiri.ui.components.SearchablePageTopBar
import dev.hdytrfli.onigiri.ui.components.TopBarPageShell
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
    onJumpToItem: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onMove: (fromIndex: Int, toIndex: Int) -> Unit,
    onToggleFavorite: (UUID) -> Unit,
    isShuffling: Boolean = false,
) {
    var searchExpanded by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    BackHandler(enabled = searchExpanded) {
        searchExpanded = false
        searchQuery = ""
    }

    TopBarPageShell(
        topBar = {
            SearchablePageTopBar(
                searchExpanded = searchExpanded,
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                onCloseSearch = {
                    searchExpanded = false
                    searchQuery = ""
                },
                title = stringResource(FilmR.string.music_queue),
                onBack = navigateBack,
                trailingContent = {
                    AppIconButton(
                        onClick = { searchExpanded = true },
                        variant = AppButtonVariant.Secondary,
                        content = {
                            androidx.compose.material3.Icon(
                                painter =
                                    androidx.compose.ui.res.painterResource(
                                        CoreR.drawable.ic_search
                                    ),
                                contentDescription = stringResource(CoreR.string.search),
                            )
                        },
                    )
                },
            )
        }
    ) {
        MusicQueueContent(
            queue = queue,
            currentIndex = currentIndex,
            searchQuery = searchQuery,
            onPlayNow = onPlayNow,
            onJumpToItem = onJumpToItem,
            onRemove = onRemove,
            onMove = onMove,
            onToggleFavorite = onToggleFavorite,
            isShuffling = isShuffling,
            modifier = Modifier.fillMaxSize().padding(horizontal = MaterialTheme.spacings.gutter),
        )
    }
}

private data class QueueEntry(val key: Long, val item: MusicPlaybackItem)

/** Queue contents shared by the full queue page and the landscape player's right column. */
@Composable
fun MusicQueueContent(
    queue: List<MusicPlaybackItem>,
    currentIndex: Int,
    onPlayNow: (Int) -> Unit,
    onJumpToItem: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onMove: (fromIndex: Int, toIndex: Int) -> Unit,
    onToggleFavorite: (UUID) -> Unit,
    modifier: Modifier = Modifier,
    searchQuery: String = "",
    isShuffling: Boolean = false,
    contentBottomPadding: androidx.compose.ui.unit.Dp = 0.dp,
    header: (@Composable () -> Unit)? = null,
) {
    val listState = rememberLazyListState()
    val initialEntries = remember {
        queue.mapIndexed { index, item -> QueueEntry(index.toLong(), item) }
    }
    var displayQueue by remember { mutableStateOf(initialEntries) }
    var nextKey by remember { mutableLongStateOf(initialEntries.size.toLong()) }
    val latestDisplayQueue by rememberUpdatedState(displayQueue)
    val latestCurrentIndex by rememberUpdatedState(currentIndex)
    val latestOnMove by rememberUpdatedState(onMove)

    LaunchedEffect(queue) {
        val keysById =
            displayQueue.groupBy { it.item.id }.mapValues { (_, entries) -> entries.map { it.key } }
        val occurrences = mutableMapOf<UUID, Int>()
        val updated = queue.map { item ->
            val occurrence = occurrences.getOrDefault(item.id, 0)
            occurrences[item.id] = occurrence + 1
            val key = keysById[item.id]?.getOrNull(occurrence) ?: nextKey.also { nextKey = it + 1 }
            QueueEntry(key, item)
        }
        if (updated != displayQueue) {
            displayQueue = updated
        }
    }

    val reorderableState =
        rememberReorderableLazyListState(listState) { from, to ->
            val fromKey = from.key as? Long ?: return@rememberReorderableLazyListState
            val toKey = to.key as? Long ?: return@rememberReorderableLazyListState
            val current = latestDisplayQueue
            val fromIndex = current.indexOfFirst { it.key == fromKey }
            val toIndex = current.indexOfFirst { it.key == toKey }
            val firstUpNext = latestCurrentIndex + 1
            if (fromIndex < firstUpNext || toIndex < firstUpNext) {
                return@rememberReorderableLazyListState
            }

            displayQueue = current.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
            latestOnMove(fromIndex, toIndex)
        }

    val safeCurrentIndex = currentIndex.coerceIn(0, displayQueue.lastIndex.coerceAtLeast(0))
    val currentQueue =
        if (displayQueue.isEmpty()) {
            emptyList()
        } else {
            (safeCurrentIndex until displayQueue.size).map { it to displayQueue[it] }
        }
    val played =
        if (displayQueue.isEmpty()) {
            emptyList()
        } else {
            ((safeCurrentIndex - 1) downTo 0).map { it to displayQueue[it] }
        }
    val query = searchQuery.trim()
    val canReorder = query.isBlank()
    fun matches(item: MusicPlaybackItem): Boolean =
        query.isBlank() ||
            item.title.contains(query, ignoreCase = true) ||
            item.artist?.contains(query, ignoreCase = true) == true ||
            item.album?.contains(query, ignoreCase = true) == true
    val visibleCurrentQueue = currentQueue.filter { matches(it.second.item) }
    val visiblePlayed = played.filter { matches(it.second.item) }

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        if (displayQueue.isEmpty()) {
            Text(
                text = stringResource(FilmR.string.music_empty_queue),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else if (isShuffling) {
            if (header != null) header()
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.widthIn(max = Dimens.contentMaxWidth).fillMaxWidth().weight(1f),
                contentPadding =
                    androidx.compose.foundation.layout.PaddingValues(
                        top = LocalRootPageContentInset.current,
                        bottom = contentBottomPadding,
                    ),
            ) {
                if (header != null) {
                    item(key = "player-header") { header() }
                }
                item(key = "current-queue-header") {
                    SectionHeader(
                        title = stringResource(FilmR.string.music_current_queue),
                        modifier = Modifier.padding(horizontal = MaterialTheme.spacings.medium),
                        trailingContent = { ItemCountBadge(count = visibleCurrentQueue.size) },
                    )
                }
                itemsIndexed(visibleCurrentQueue, key = { _, entry -> entry.second.key }) {
                    queueIndex,
                    entry ->
                    val index = entry.first
                    val item = entry.second.item
                    if (index == safeCurrentIndex || !canReorder) {
                        QueueRow(
                            item = item,
                            index = index,
                            currentIndex = currentIndex,
                            dragging = false,
                            onPlayNow = { onPlayNow(index) },
                            onJumpToItem = { onJumpToItem(index) },
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
                            modifier = Modifier.animateItem(),
                            trailingDivider = visibleCurrentQueue.size > 1,
                        )
                    } else {
                        ReorderableItem(reorderableState, key = entry.second.key) { isDragging ->
                            QueueRow(
                                item = item,
                                index = index,
                                currentIndex = currentIndex,
                                dragging = isDragging,
                                onPlayNow = { onPlayNow(index) },
                                onJumpToItem = { onJumpToItem(index) },
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
                if (visiblePlayed.isNotEmpty()) {
                    item(key = "played-header") {
                        SectionHeader(
                            title = stringResource(FilmR.string.music_played),
                            modifier =
                                Modifier.padding(
                                    start = MaterialTheme.spacings.medium,
                                    end = MaterialTheme.spacings.medium,
                                    top = MaterialTheme.spacings.medium,
                                ),
                            trailingContent = { ItemCountBadge(count = visiblePlayed.size) },
                        )
                    }
                    itemsIndexed(visiblePlayed, key = { _, entry -> entry.second.key }) { _, entry
                        ->
                        val index = entry.first
                        val item = entry.second.item
                        QueueRow(
                            item = item,
                            index = index,
                            currentIndex = currentIndex,
                            dragging = false,
                            onPlayNow = { onPlayNow(index) },
                            onJumpToItem = { onJumpToItem(index) },
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
                            modifier = Modifier.animateItem(),
                            trailingDivider = index > 0,
                        )
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
    onJumpToItem: () -> Unit,
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
                    label = stringResource(FilmR.string.music_jump_to_here),
                    icon = CoreR.drawable.ic_play,
                    enabled = !isCurrent,
                    onClick = onJumpToItem,
                ),
                MusicRowAction(
                    label = stringResource(FilmR.string.music_play_next),
                    icon = CoreR.drawable.ic_skip_forward,
                    enabled = !isCurrent,
                    onClick = onPlayNext,
                ),
                MusicRowAction(
                    label = stringResource(FilmR.string.music_play_end),
                    icon = CoreR.drawable.ic_arrow_down_to_line,
                    enabled = !isCurrent,
                    onClick = onPlayEnd,
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
