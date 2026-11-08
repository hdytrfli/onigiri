package dev.hdytrfli.onigiri.player.local.music

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.hdytrfli.onigiri.models.FindroidMusicSong
import dev.hdytrfli.onigiri.repository.JellyfinRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * App-wide bridge to the music [MusicPlaybackService].
 *
 * The service owns the actual ExoPlayer/MediaSession. This manager keeps a [MediaController] and
 * exposes a small, UI-friendly state model for the mini player, the full player and the detail
 * pages. It also resolves Jellyfin audio URLs before handing media items to the controller.
 */
@Singleton
class MusicPlayerManager
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val repository: JellyfinRepository,
    private val appPreferences: dev.hdytrfli.onigiri.settings.domain.AppPreferences,
) : Player.Listener {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val sessionToken =
        SessionToken(context, ComponentName(context, MusicPlaybackService::class.java))
    private val pendingActions = mutableListOf<(MediaController) -> Unit>()
    private val favoriteOverrides = mutableMapOf<UUID, Boolean>()

    private val _state = MutableStateFlow(MusicPlaybackState())
    val state = _state.asStateFlow()

    private var controller: MediaController? = null
    private var connecting = false
    private var batchDepth = 0

    init {
        connect()
        scope.launch {
            appPreferences.observe(appPreferences.currentUser).drop(1).collect {
                favoriteOverrides.clear()
                stop()
                _state.value = MusicPlaybackState(isConnected = controller != null)
            }
        }
        scope.launch {
            while (isActive) {
                if (controller?.isPlaying == true) updateProgress()
                delay(500)
            }
        }
    }

    private fun connect() {
        if (controller != null || connecting) return
        connecting = true

        val future = MediaController.Builder(context, sessionToken).buildAsync()
        future.addListener(
            {
                connecting = false
                try {
                    val connected = future.get()
                    controller = connected
                    connected.addListener(this)
                    _state.update { it.copy(isConnected = true, error = null) }
                    sync()
                    val actions = pendingActions.toList()
                    pendingActions.clear()
                    actions.forEach { it(connected) }
                } catch (e: Exception) {
                    Timber.e(e, "Unable to connect to the music playback service")
                    _state.update {
                        it.copy(
                            isConnected = false,
                            error = e.localizedMessage ?: "Unable to start music playback",
                        )
                    }
                }
            },
            ContextCompat.getMainExecutor(context),
        )
    }

    private fun runOnController(action: (MediaController) -> Unit) {
        val connected = controller
        if (connected != null) {
            action(connected)
        } else {
            pendingActions += action
            connect()
        }
    }

    /**
     * Queue-wide edits (notably shuffling) fire one player event per move. Rebuilding and
     * publishing the whole UI state for every event makes large queues stutter, so keep the
     * intermediate events internal and sync only once when the batch is complete.
     */
    private inline fun batchPlayerUpdate(block: () -> Unit) {
        batchDepth++
        try {
            block()
        } finally {
            batchDepth--
            if (batchDepth == 0) sync()
        }
    }

    fun playSongs(
        songs: List<FindroidMusicSong>,
        startIndex: Int = 0,
        shuffle: Boolean = false,
        isAudiobook: Boolean = false,
        overview: String? = null,
    ) {
        scope.launch {
            val items = buildMediaItems(songs, isAudiobook = isAudiobook, overview = overview)
            if (items.isEmpty()) {
                _state.update { it.copy(error = "No playable music stream was found") }
                return@launch
            }
            val safeIndex = startIndex.coerceIn(0, items.lastIndex)
            runOnController { player ->
                player.shuffleModeEnabled = false
                player.setMediaItems(items, safeIndex, 0L)
                player.prepare()
                if (shuffle) {
                    val order =
                        intArrayOf(safeIndex) +
                            (items.indices.filter { it != safeIndex }).shuffled()
                    setShuffleOrder(order)
                    player.shuffleModeEnabled = true
                }
                player.play()
            }
        }
    }

    fun playNext(songs: List<FindroidMusicSong>) {
        scope.launch {
            val items = buildMediaItems(songs)
            if (items.isEmpty()) return@launch
            runOnController { player ->
                if (player.mediaItemCount == 0) {
                    player.setMediaItems(items)
                    player.prepare()
                    player.play()
                } else {
                    val index =
                        (player.currentMediaItemIndex + 1).coerceAtMost(player.mediaItemCount)
                    player.addMediaItems(index, items)
                }
            }
        }
    }

    fun addToQueue(songs: List<FindroidMusicSong>) {
        scope.launch {
            val items = buildMediaItems(songs)
            if (items.isEmpty()) return@launch
            runOnController { player ->
                if (player.mediaItemCount == 0) {
                    player.setMediaItems(items)
                    player.prepare()
                    player.play()
                } else {
                    player.addMediaItems(items)
                }
            }
        }
    }

    fun playQueueItem(index: Int) {
        val state = _state.value
        val item = state.queue.getOrNull(index) ?: return
        runOnController { player ->
            if (item.timelineIndex !in 0 until player.mediaItemCount) return@runOnController
            batchPlayerUpdate {
                val current = state.currentIndex
                var seekIndex = item.timelineIndex
                if (index != current) {
                    if (player.shuffleModeEnabled) {
                        val order = state.queue.map { it.timelineIndex }.toMutableList()
                        // Place the clicked track directly after the current one. For a track that
                        // was already played, removing it shifts the current position back by one,
                        // so the insertion point is the old current index rather than current + 1.
                        val target =
                            if (index < current) {
                                current
                            } else {
                                (current + 1).coerceAtMost(order.lastIndex)
                            }
                        if (index != target) {
                            order.add(target, order.removeAt(index))
                            setShuffleOrder(order.toIntArray())
                        }
                    } else {
                        // Jumping to another track moves only the clicked track next to the current
                        // one. Tracks in between stay in the played history instead of being moved
                        // back into the up-next queue.
                        val physicalCurrent = player.currentMediaItemIndex
                        val target = (physicalCurrent + 1).coerceAtMost(player.mediaItemCount - 1)
                        player.moveMediaItem(item.timelineIndex, target)
                        seekIndex = target
                    }
                }
                player.seekTo(seekIndex, 0L)
                player.play()
            }
        }
    }

    /**
     * Jumps directly to a queue position without reordering the queue. Moving forward marks the
     * skipped items as played; moving back into the played history brings that item and everything
     * after it back into the current queue.
     */
    fun jumpToQueueItem(index: Int) {
        val item = _state.value.queue.getOrNull(index) ?: return
        runOnController { player ->
            if (item.timelineIndex !in 0 until player.mediaItemCount) return@runOnController
            player.seekTo(item.timelineIndex, 0L)
            player.play()
        }
    }

    fun removeQueueItem(index: Int) {
        val item = _state.value.queue.getOrNull(index) ?: return
        runOnController { player ->
            if (item.timelineIndex in 0 until player.mediaItemCount) {
                player.removeMediaItem(item.timelineIndex)
            }
        }
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        runOnController { player ->
            if (player.shuffleModeEnabled) {
                reorderShuffle(fromIndex, toIndex)
            } else if (
                fromIndex in 0 until player.mediaItemCount &&
                    toIndex in 0 until player.mediaItemCount &&
                    fromIndex != toIndex
            ) {
                player.moveMediaItem(fromIndex, toIndex)
            }
        }
    }

    fun togglePlayPause() {
        runOnController { player ->
            if (player.isPlaying) {
                player.pause()
            } else {
                if (player.playbackState == Player.STATE_ENDED) player.seekTo(0L)
                player.play()
            }
        }
    }

    fun pause() {
        runOnController { player -> if (player.isPlaying) player.pause() }
    }

    /**
     * Holds music playback at 2x while the artwork is held. The speed is ramped so both the audio
     * and the disc rotation speed up and settle back smoothly.
     */
    fun setSpeedBoost(active: Boolean) {
        val target = if (active) 2f else 1f
        runOnController { it.setPlaybackSpeed(target) }
    }

    fun stop() {
        runOnController { player ->
            player.stop()
            player.clearMediaItems()
        }
    }

    fun previous() {
        runOnController { player ->
            if (player.currentPosition >= 5_000L) {
                player.seekTo(0L)
            } else if (player.hasPreviousMediaItem()) {
                player.seekToPreviousMediaItem()
                player.play()
            } else {
                player.seekTo(0L)
            }
        }
    }

    fun next() {
        runOnController { player ->
            if (player.hasNextMediaItem()) {
                player.seekToNextMediaItem()
                player.play()
            }
        }
    }

    fun seekTo(positionMs: Long) {
        runOnController { it.seekTo(positionMs.coerceAtLeast(0L)) }
    }

    fun toggleShuffle() {
        runOnController { player ->
            if (player.mediaItemCount <= 1) {
                player.shuffleModeEnabled = false
                sync()
                return@runOnController
            }
            if (player.shuffleModeEnabled) {
                player.shuffleModeEnabled = false
            } else {
                val current = player.currentMediaItemIndex
                val order =
                    (0 until player.mediaItemCount)
                        .filter { it != current }
                        .shuffled()
                        .let { intArrayOf(current) + it }
                setShuffleOrder(order)
                player.shuffleModeEnabled = true
            }
            sync()
        }
    }

    private fun setShuffleOrder(order: IntArray) {
        val args =
            Bundle().apply {
                putIntArray(MusicSessionCommands.EXTRA_SHUFFLE_ORDER, order)
                putLong(MusicSessionCommands.EXTRA_SHUFFLE_SEED, System.currentTimeMillis())
            }
        controller?.sendCustomCommand(MusicSessionCommands.SetShuffleOrder, args)
    }

    private fun reorderShuffle(fromIndex: Int, toIndex: Int) {
        val order = _state.value.queue.map { it.timelineIndex }.toMutableList()
        if (fromIndex !in order.indices || toIndex !in order.indices || fromIndex == toIndex) return
        order.add(toIndex, order.removeAt(fromIndex))
        setShuffleOrder(order.toIntArray())
    }

    fun cycleRepeatMode() {
        runOnController { player ->
            player.repeatMode =
                when (player.repeatMode) {
                    Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                    Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                    else -> Player.REPEAT_MODE_OFF
                }
        }
    }

    fun toggleFavorite() {
        val item = _state.value.current ?: return
        toggleFavorite(item.id)
    }

    fun toggleFavorite(itemId: UUID) {
        val item =
            _state.value.queue.firstOrNull { it.id == itemId }
                ?: _state.value.current?.takeIf { it.id == itemId }
                ?: return
        scope.launch {
            val favorite = !item.favorite
            try {
                if (favorite) {
                    repository.markAsFavorite(itemId)
                } else {
                    repository.unmarkAsFavorite(itemId)
                }
                favoriteOverrides[itemId] = favorite
                sync()
            } catch (e: Exception) {
                Timber.e(e, "Unable to update favorite state for $itemId")
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    private suspend fun buildMediaItems(
        songs: List<FindroidMusicSong>,
        isAudiobook: Boolean = false,
        overview: String? = null,
    ): List<MediaItem> = coroutineScope {
        songs
            .map { song ->
                async { song.toMediaItem(isAudiobook = isAudiobook, overview = overview) }
            }
            .awaitAll()
            .filterNotNull()
    }

    private suspend fun FindroidMusicSong.toMediaItem(
        isAudiobook: Boolean = false,
        overview: String? = null,
    ): MediaItem? {
        val url = repository.getAudioStreamUrl(id)
        if (url.isBlank()) return null

        val extras =
            Bundle().apply {
                putLong(EXTRA_DURATION_MS, runtimeTicks / 10_000L)
                putBoolean(EXTRA_FAVORITE, favorite)
                putBoolean(EXTRA_IS_AUDIOBOOK, isAudiobook)
                putString(EXTRA_OVERVIEW, overview)
                putString(EXTRA_ARTIST_ID, artistId?.toString())
                putString(EXTRA_ARTIST_IMAGE, artistImage?.toString())
                putString(EXTRA_ALBUM_ID, albumId?.toString())
            }
        val metadata =
            MediaMetadata.Builder()
                .setTitle(name)
                .setArtist(artistName)
                .setAlbumTitle(albumName)
                .setArtworkUri(images.primary)
                .setDurationMs(runtimeTicks / 10_000L)
                .setExtras(extras)
                .build()

        return MediaItem.Builder()
            .setMediaId(id.toString())
            .setUri(url)
            .setMediaMetadata(metadata)
            .build()
    }

    private fun MediaItem.toPlaybackItem(timelineIndex: Int): MusicPlaybackItem? {
        val id =
            mediaId?.let { value -> runCatching { UUID.fromString(value) }.getOrNull() }
                ?: return null
        val metadata = mediaMetadata
        val extras = metadata.extras
        return MusicPlaybackItem(
            id = id,
            title = metadata.title?.toString().orEmpty(),
            artist = metadata.artist?.toString(),
            artistId =
                extras?.getString(EXTRA_ARTIST_ID)?.let {
                    runCatching { UUID.fromString(it) }.getOrNull()
                },
            artistImageUri = extras?.getString(EXTRA_ARTIST_IMAGE),
            album = metadata.albumTitle?.toString(),
            albumId =
                extras?.getString(EXTRA_ALBUM_ID)?.let {
                    runCatching { UUID.fromString(it) }.getOrNull()
                },
            imageUri = metadata.artworkUri?.toString(),
            durationMs =
                metadata.durationMs?.takeIf { it > 0L } ?: extras?.getLong(EXTRA_DURATION_MS) ?: 0L,
            favorite = favoriteOverrides[id] ?: extras?.getBoolean(EXTRA_FAVORITE) ?: false,
            isAudiobook = extras?.getBoolean(EXTRA_IS_AUDIOBOOK) ?: false,
            overview = extras?.getString(EXTRA_OVERVIEW),
            timelineIndex = timelineIndex,
        )
    }

    private fun sync() {
        val player = controller ?: return
        val physicalQueue =
            (0 until player.mediaItemCount).mapNotNull { index ->
                player.getMediaItemAt(index).toPlaybackItem(index)
            }
        val physicalCurrent = player.currentMediaItemIndex.coerceAtLeast(0)
        val (queue, currentIndex) =
            if (player.shuffleModeEnabled) {
                playbackOrder(player, physicalQueue)
            } else {
                physicalQueue to physicalCurrent
            }
        val current = queue.getOrNull(currentIndex)
        val duration =
            when (val value = player.duration) {
                C.TIME_UNSET,
                C.TIME_END_OF_SOURCE -> current?.durationMs ?: 0L
                else -> value.coerceAtLeast(0L)
            }
        _state.value =
            MusicPlaybackState(
                current = current,
                queue = queue,
                currentIndex = currentIndex,
                hasPrevious = player.hasPreviousMediaItem(),
                hasNext = player.hasNextMediaItem(),
                isPlaying = player.isPlaying,
                playWhenReady = player.playWhenReady,
                hasEnded = player.playbackState == Player.STATE_ENDED,
                isBuffering = player.playbackState == Player.STATE_BUFFERING,
                positionMs = player.currentPosition.coerceAtLeast(0L),
                bufferedPositionMs = player.bufferedPosition.coerceAtLeast(0L),
                durationMs = duration,
                shuffleEnabled = player.shuffleModeEnabled,
                isShuffling = _state.value.isShuffling,
                repeatMode =
                    when (player.repeatMode) {
                        Player.REPEAT_MODE_ALL -> MusicRepeatMode.ALL
                        Player.REPEAT_MODE_ONE -> MusicRepeatMode.ONE
                        else -> MusicRepeatMode.OFF
                    },
                isConnected = true,
            )
    }

    private fun playbackOrder(
        player: Player,
        physicalQueue: List<MusicPlaybackItem>,
    ): Pair<List<MusicPlaybackItem>, Int> {
        val current = player.currentMediaItemIndex
        val timeline = player.currentTimeline
        if (current < 0 || timeline.isEmpty || physicalQueue.isEmpty()) {
            return physicalQueue to current.coerceAtLeast(0)
        }

        val before = mutableListOf<Int>()
        var previous = timeline.getPreviousWindowIndex(current, Player.REPEAT_MODE_OFF, true)
        while (previous != C.INDEX_UNSET) {
            before.add(previous)
            previous = timeline.getPreviousWindowIndex(previous, Player.REPEAT_MODE_OFF, true)
        }
        before.reverse()

        val after = mutableListOf<Int>()
        var next = timeline.getNextWindowIndex(current, Player.REPEAT_MODE_OFF, true)
        while (next != C.INDEX_UNSET) {
            after.add(next)
            next = timeline.getNextWindowIndex(next, Player.REPEAT_MODE_OFF, true)
        }

        val byIndex = physicalQueue.associateBy { it.timelineIndex }
        val ordered = (before + current + after).mapNotNull { byIndex[it] }
        if (ordered.size != physicalQueue.size) {
            return physicalQueue to current.coerceAtLeast(0)
        }
        return ordered to before.size
    }

    private fun updateProgress() {
        val player = controller ?: return
        val duration =
            when (val value = player.duration) {
                C.TIME_UNSET,
                C.TIME_END_OF_SOURCE -> _state.value.durationMs
                else -> value.coerceAtLeast(0L)
            }
        _state.update {
            it.copy(
                positionMs = player.currentPosition.coerceAtLeast(0L),
                durationMs = duration,
            )
        }
    }

    override fun onEvents(player: Player, events: Player.Events) {
        if (batchDepth == 0) sync()
    }

    override fun onPlayerError(error: PlaybackException) {
        Timber.e(error, "Music playback error")
        _state.update { it.copy(error = error.localizedMessage ?: "Music playback failed") }
    }

    private companion object {
        const val EXTRA_DURATION_MS = "music_duration_ms"
        const val EXTRA_FAVORITE = "music_favorite"
        const val EXTRA_IS_AUDIOBOOK = "music_is_audiobook"
        const val EXTRA_OVERVIEW = "music_overview"
        const val EXTRA_ARTIST_ID = "music_artist_id"
        const val EXTRA_ARTIST_IMAGE = "music_artist_image"
        const val EXTRA_ALBUM_ID = "music_album_id"
    }
}
