package dev.hdytrfli.onigiri

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import dev.hdytrfli.onigiri.presentation.player.MusicPlayerScreen
import dev.hdytrfli.onigiri.presentation.player.MusicPlayerViewModel
import dev.hdytrfli.onigiri.ui.theme.FindroidTheme

@AndroidEntryPoint
class MusicPlayerActivity : ComponentActivity() {
    private val viewModel: MusicPlayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()

            FindroidTheme {
                MusicPlayerScreen(
                    state = state,
                    navigateBack = ::finish,
                    onPlayPause = viewModel::togglePlayPause,
                    onPrevious = viewModel::previous,
                    onNext = viewModel::next,
                    onSeek = viewModel::seekTo,
                    onToggleShuffle = viewModel::toggleShuffle,
                    onCycleRepeat = viewModel::cycleRepeatMode,
                    onToggleFavorite = viewModel::toggleFavorite,
                    onPlayQueueItem = viewModel::playQueueItem,
                    onRemoveQueueItem = viewModel::removeQueueItem,
                    onClearError = viewModel::clearError,
                )
            }
        }
    }
}
