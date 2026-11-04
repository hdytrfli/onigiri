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

    init {
        connect()
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

    fun playSongs(
        songs: List<FindroidMusicSong>,
        startIndex: Int = 0,
        shuffle: Boolean = false,
    ) {
        scope.launch {
            val items = buildMediaItems(songs)
            if (items.isEmpty()) {
                _state.update { it.copy(error = "No playable music stream was found") }
                return@launch
            }
            val safeIndex = startIndex.coerceIn(0, items.lastIndex)
            runOnController { player ->
                player.shuffleModeEnabled = shuffle
                player.setMediaItems(items, safeIndex, 0L)
                player.prepare()
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
        runOnController { player ->
            if (index in 0 until player.mediaItemCount) {
                player.seekTo(index, 0L)
                player.play()
            }
        }
    }

    fun removeQueueItem(index: Int) {
        runOnController { player ->
            if (index in 0 until player.mediaItemCount) {
                player.removeMediaItem(index)
            }
        }
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        runOnController { player ->
            if (
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
        runOnController { it.shuffleModeEnabled = !it.shuffleModeEnabled }
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
        scope.launch {
            val favorite = !item.favorite
            try {
                if (favorite) {
                    repository.markAsFavorite(item.id)
                } else {
                    repository.unmarkAsFavorite(item.id)
                }
                favoriteOverrides[item.id] = favorite
                sync()
            } catch (e: Exception) {
                Timber.e(e, "Unable to update favorite state for ${item.id}")
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    private suspend fun buildMediaItems(songs: List<FindroidMusicSong>): List<MediaItem> =
        coroutineScope {
            songs.map { song -> async { song.toMediaItem() } }.awaitAll().filterNotNull()
        }

    private suspend fun FindroidMusicSong.toMediaItem(): MediaItem? {
        val url = repository.getAudioStreamUrl(id)
        if (url.isBlank()) return null

        val extras =
            Bundle().apply {
                putLong(EXTRA_DURATION_MS, runtimeTicks / 10_000L)
                putBoolean(EXTRA_FAVORITE, favorite)
            }
        val metadata =
            MediaMetadata.Builder()
                .setTitle(name)
                .setArtist(artistName)
                .setAlbumTitle(albumName)
                .setArtworkUri(images.primary)
                .setExtras(extras)
                .build()

        return MediaItem.Builder()
            .setMediaId(id.toString())
            .setUri(url)
            .setMediaMetadata(metadata)
            .build()
    }

    private fun MediaItem.toPlaybackItem(): MusicPlaybackItem? {
        val id =
            mediaId?.let { value -> runCatching { UUID.fromString(value) }.getOrNull() }
                ?: return null
        val metadata = mediaMetadata
        val extras = metadata.extras
        return MusicPlaybackItem(
            id = id,
            title = metadata.title?.toString().orEmpty(),
            artist = metadata.artist?.toString(),
            album = metadata.albumTitle?.toString(),
            imageUri = metadata.artworkUri?.toString(),
            durationMs = extras?.getLong(EXTRA_DURATION_MS) ?: 0L,
            favorite = favoriteOverrides[id] ?: extras?.getBoolean(EXTRA_FAVORITE) ?: false,
        )
    }

    private fun sync() {
        val player = controller ?: return
        val queue =
            (0 until player.mediaItemCount).mapNotNull {
                player.getMediaItemAt(it).toPlaybackItem()
            }
        val current = player.currentMediaItem?.toPlaybackItem()
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
                currentIndex = player.currentMediaItemIndex.coerceAtLeast(0),
                isPlaying = player.isPlaying,
                isBuffering = player.playbackState == Player.STATE_BUFFERING,
                positionMs = player.currentPosition.coerceAtLeast(0L),
                durationMs = duration,
                shuffleEnabled = player.shuffleModeEnabled,
                repeatMode =
                    when (player.repeatMode) {
                        Player.REPEAT_MODE_ALL -> MusicRepeatMode.ALL
                        Player.REPEAT_MODE_ONE -> MusicRepeatMode.ONE
                        else -> MusicRepeatMode.OFF
                    },
                isConnected = true,
            )
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
        sync()
    }

    override fun onPlayerError(error: PlaybackException) {
        Timber.e(error, "Music playback error")
        _state.update { it.copy(error = error.localizedMessage ?: "Music playback failed") }
    }

    private companion object {
        const val EXTRA_DURATION_MS = "music_duration_ms"
        const val EXTRA_FAVORITE = "music_favorite"
    }
}
