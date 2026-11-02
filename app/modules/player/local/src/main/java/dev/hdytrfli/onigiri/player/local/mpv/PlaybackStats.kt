package dev.hdytrfli.onigiri.player.local.mpv

/**
 * One reading of mpv's playback statistics, gathered on demand by [MPVPlayer.playbackStats].
 *
 * Anything mpv has nothing to say about stays null, so the stats panel can leave a row out rather
 * than print a zero that looks like a real measurement.
 */
data class PlaybackStats(
    val width: Int = 0,
    val height: Int = 0,
    /** Measured frame rate, falling back to the container's declared rate. */
    val fps: Double? = null,
    /** e.g. `h264 (High)`; the codec string mpv prints in its own stats. */
    val videoCodec: String? = null,
    val videoBitrate: Double? = null,
    val audioCodec: String? = null,
    val audioChannels: Int? = null,
    val audioBitrate: Double? = null,
    /** `mediacodec`, `mediacodec-copy`, … or `software` when mpv decodes on the CPU. */
    val hardwareDecoder: String? = null,
    /** Frames the video output could not display in time. */
    val droppedFrames: Long = 0,
    /** Frames the decoder itself threw away. */
    val decoderDroppedFrames: Long = 0,
    /** Frames the video output held back to keep up. */
    val delayedFrames: Long = 0,
    /** Last audio/video difference mpv measured, in seconds; can be negative. */
    val avSyncSeconds: Double? = null,
    val bufferSeconds: Double? = null,
) {
    /** Every frame lost, however it was lost. */
    val totalDroppedFrames: Long
        get() = droppedFrames + decoderDroppedFrames
}
