package dev.jdtech.jellyfin

import androidx.compose.runtime.compositionLocalOf
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Re-taps of the already-selected navigation item: the suite emits the route and the matching tab
 * root reacts (Home and Collections cycle their tabs, Movies and Shows open their search). A plain
 * tap on another item still just navigates.
 */
class TabReselect {
    private val _events = MutableSharedFlow<NavKey>(extraBufferCapacity = 1)
    val events: SharedFlow<NavKey> = _events.asSharedFlow()

    /**
     * A request to open the Downloads tab. It stays set until the Home page consumes it, so a
     * request made before Home is composed is not lost.
     */
    private val _openDownloads = MutableStateFlow(false)
    val openDownloads: StateFlow<Boolean> = _openDownloads.asStateFlow()

    fun reselect(route: NavKey) {
        _events.tryEmit(route)
    }

    fun requestDownloads() {
        _openDownloads.value = true
    }

    fun consumeDownloads() {
        _openDownloads.value = false
    }
}

val LocalTabReselect = compositionLocalOf { TabReselect() }

/** Navigates to the Downloads tab from anywhere, used by the offline notice. */
val LocalOpenDownloads = compositionLocalOf { {} }
