package dev.hdytrfli.onigiri.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.hdytrfli.onigiri.ui.utils.LocalOfflineMode

/**
 * Runs [onModeChanged] after the user toggles offline mode, without firing on first composition.
 * Used by pages that own their data outside the SWR store.
 */
@Composable
fun OfflineModeReload(onModeChanged: () -> Unit) {
    val offlineMode = LocalOfflineMode.current
    var previous by remember { mutableStateOf(offlineMode) }
    LaunchedEffect(offlineMode) {
        if (previous != offlineMode) {
            previous = offlineMode
            onModeChanged()
        }
    }
}
