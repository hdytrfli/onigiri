package dev.hdytrfli.onigiri.presentation.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.hdytrfli.onigiri.player.local.music.MusicPlaybackItem
import dev.hdytrfli.onigiri.player.local.music.MusicPlayerManager
import dev.hdytrfli.onigiri.settings.domain.AppPreferences
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class MusicPlayerViewModel
@Inject
constructor(
    private val manager: MusicPlayerManager,
    appPreferences: AppPreferences,
) : ViewModel() {
    val state = manager.state

    val artworkType: kotlinx.coroutines.flow.StateFlow<String> =
        appPreferences
            .observe(appPreferences.musicArtworkType)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = appPreferences.getValue(appPreferences.musicArtworkType),
            )

    /** Changes only when the track changes, so the navigation shell does not tick with progress. */
    val current: kotlinx.coroutines.flow.StateFlow<MusicPlaybackItem?> =
        manager.state
            .map { it.current }
            .distinctUntilChanged()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = manager.state.value.current,
            )

    /** The mini player only exists while audio is actually playing. */
    val miniPlayerVisible: kotlinx.coroutines.flow.StateFlow<Boolean> =
        manager.state
            .map { it.current != null && it.isPlaying }
            .distinctUntilChanged()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = manager.state.value.current != null && manager.state.value.isPlaying,
            )

    fun togglePlayPause() = manager.togglePlayPause()

    fun pause() = manager.pause()

    fun stop() = manager.stop()

    fun previous() = manager.previous()

    fun next() = manager.next()

    fun seekTo(positionMs: Long) = manager.seekTo(positionMs)

    fun toggleShuffle() = manager.toggleShuffle()

    fun cycleRepeatMode() = manager.cycleRepeatMode()

    fun toggleFavorite() = manager.toggleFavorite()

    fun playQueueItem(index: Int) = manager.playQueueItem(index)

    fun removeQueueItem(index: Int) = manager.removeQueueItem(index)

    fun moveQueueItem(fromIndex: Int, toIndex: Int) = manager.moveQueueItem(fromIndex, toIndex)

    fun clearError() = manager.clearError()
}
