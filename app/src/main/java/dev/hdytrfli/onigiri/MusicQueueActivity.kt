package dev.hdytrfli.onigiri

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import dev.hdytrfli.onigiri.presentation.player.MusicPlayerViewModel
import dev.hdytrfli.onigiri.presentation.player.MusicQueueScreen
import dev.hdytrfli.onigiri.settings.domain.AppPreferences
import dev.hdytrfli.onigiri.ui.theme.FindroidTheme
import dev.hdytrfli.onigiri.ui.utils.LocalHideStatusBar
import javax.inject.Inject

@AndroidEntryPoint
class MusicQueueActivity : ComponentActivity() {
    @Inject lateinit var appPreferences: AppPreferences

    private val viewModel: MusicPlayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val hideStatusBar = appPreferences.getValue(appPreferences.hideStatusBar)
        if (hideStatusBar) {
            WindowCompat.getInsetsController(window, window.decorView).apply {
                systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                hide(WindowInsetsCompat.Type.statusBars())
            }
        }

        setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()

            CompositionLocalProvider(LocalHideStatusBar provides hideStatusBar) {
                FindroidTheme {
                    MusicQueueScreen(
                        queue = state.queue,
                        currentIndex = state.currentIndex,
                        navigateBack = ::finish,
                        onPlayNow = viewModel::playQueueItem,
                        onJumpToItem = viewModel::jumpToQueueItem,
                        onRemove = viewModel::removeQueueItem,
                        onMove = viewModel::moveQueueItem,
                        onToggleFavorite = viewModel::toggleFavoriteItem,
                        isShuffling = state.isShuffling,
                    )
                }
            }
        }
    }
}
