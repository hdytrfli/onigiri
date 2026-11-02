package dev.hdytrfli.onigiri.ui.utils

import androidx.compose.runtime.compositionLocalOf
import java.util.UUID
import org.jellyfin.sdk.model.api.BaseItemKind

/** Launches the app's player without coupling feature modules to the player activity. */
fun interface PlaybackLauncher {
    fun launch(itemId: UUID, itemKind: BaseItemKind, startFromBeginning: Boolean)
}

val LocalPlaybackLauncher = compositionLocalOf<PlaybackLauncher> { PlaybackLauncher { _, _, _ -> } }
