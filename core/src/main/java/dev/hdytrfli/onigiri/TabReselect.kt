package dev.hdytrfli.onigiri

import androidx.compose.runtime.compositionLocalOf
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter

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

    /** A requested Books subtab: 0 for books, 1 for audiobooks. */
    private val _booksTab = MutableStateFlow<Int?>(null)
    val booksTab: StateFlow<Int?> = _booksTab.asStateFlow()

    fun reselect(route: NavKey) {
        _events.tryEmit(route)
    }

    /** Re-taps of [route], for the page that owns that tab to perform its secondary action. */
    fun reselects(route: NavKey): Flow<NavKey> = events.filter { it == route }

    fun requestDownloads() {
        _openDownloads.value = true
    }

    fun consumeDownloads() {
        _openDownloads.value = false
    }

    fun requestBooksTab(tab: Int) {
        _booksTab.value = tab
    }

    fun consumeBooksTab() {
        _booksTab.value = null
    }
}

val LocalTabReselect = compositionLocalOf { TabReselect() }

/** Navigates to the Downloads tab from anywhere, used by the offline notice. */
val LocalOpenDownloads = compositionLocalOf { {} }
