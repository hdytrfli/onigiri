package dev.hdytrfli.onigiri.player.local.music

import java.util.UUID

/** One queue entry, independent of Media3 so UI code does not depend on player internals. */
data class MusicPlaybackItem(
    val id: UUID,
    val title: String,
    val artist: String?,
    val artistId: UUID?,
    val artistImageUri: String?,
    val album: String?,
    val albumId: UUID?,
    val imageUri: String?,
    val durationMs: Long,
    val favorite: Boolean,
)

enum class MusicRepeatMode {
    OFF,
    ALL,
    ONE,
}

data class MusicPlaybackState(
    val current: MusicPlaybackItem? = null,
    val queue: List<MusicPlaybackItem> = emptyList(),
    val currentIndex: Int = 0,
    val hasPrevious: Boolean = false,
    val hasNext: Boolean = false,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val shuffleEnabled: Boolean = false,
    val repeatMode: MusicRepeatMode = MusicRepeatMode.OFF,
    val isConnected: Boolean = false,
    val error: String? = null,
)
