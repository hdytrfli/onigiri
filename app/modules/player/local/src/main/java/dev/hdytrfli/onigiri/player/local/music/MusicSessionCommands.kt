package dev.hdytrfli.onigiri.player.local.music

/** Commands exchanged between the UI-side controller and the playback service. */
object MusicSessionCommands {
    const val ACTION_SET_SHUFFLE_ORDER = "dev.hdytrfli.onigiri.music.SET_SHUFFLE_ORDER"
    const val EXTRA_SHUFFLE_ORDER = "shuffle_order"
    const val EXTRA_SHUFFLE_SEED = "shuffle_seed"

    val SetShuffleOrder =
        androidx.media3.session.SessionCommand(ACTION_SET_SHUFFLE_ORDER, android.os.Bundle.EMPTY)
}
