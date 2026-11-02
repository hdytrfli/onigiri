package dev.hdytrfli.onigiri.player.local.domain

/**
 * How the Jellyfin server is delivering the current stream, as its own clients report it in their
 * playback stats. Only the play method is always known: the rest describes the server's transcode
 * and stays empty when it is passing the file through as it is.
 */
data class StreamInfo(
    val playMethod: PlayMethod,
    val videoCodec: String? = null,
    val audioCodec: String? = null,
    /** The video stream is being passed through, whether or not the container is remuxed. */
    val videoIsDirect: Boolean = false,
    val audioIsDirect: Boolean = false,
    val bitrate: Int? = null,
    val audioChannels: Int? = null,
    /** Frames per second the server is encoding at. */
    val framerate: Float? = null,
    /** Why the server decided to transcode, in readable form. */
    val reasons: List<String> = emptyList(),
) {
    /** How the file reaches the player, worded the way Jellyfin's own clients word it. */
    enum class PlayMethod {
        DIRECT_PLAY,
        DIRECT_STREAM,
        REMUX,
        TRANSCODE,
    }
}
