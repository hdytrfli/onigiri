package dev.hdytrfli.onigiri

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import dev.hdytrfli.onigiri.presentation.player.MusicPlayerViewModel
import dev.hdytrfli.onigiri.presentation.player.MusicQueueScreen
import dev.hdytrfli.onigiri.ui.theme.FindroidTheme

@AndroidEntryPoint
class MusicQueueActivity : ComponentActivity() {
    private val viewModel: MusicPlayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()

            FindroidTheme {
                MusicQueueScreen(
                    queue = state.queue,
                    currentIndex = state.currentIndex,
                    navigateBack = ::finish,
                    onPlayNow = viewModel::playQueueItem,
                    onRemove = viewModel::removeQueueItem,
                    onMove = viewModel::moveQueueItem,
                )
            }
        }
    }
}
