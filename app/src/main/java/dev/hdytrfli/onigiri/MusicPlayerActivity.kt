package dev.hdytrfli.onigiri

import android.content.Intent
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
import dev.hdytrfli.onigiri.presentation.player.MusicPlayerScreen
import dev.hdytrfli.onigiri.presentation.player.MusicPlayerViewModel
import dev.hdytrfli.onigiri.settings.R as SettingsR
import dev.hdytrfli.onigiri.settings.domain.AppPreferences
import dev.hdytrfli.onigiri.ui.theme.FindroidTheme
import dev.hdytrfli.onigiri.ui.utils.LocalHideStatusBar
import javax.inject.Inject

@AndroidEntryPoint
class MusicPlayerActivity : ComponentActivity() {
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
            val artworkType by viewModel.artworkType.collectAsStateWithLifecycle()
            val swipeArtwork by viewModel.swipeArtwork.collectAsStateWithLifecycle()
            val preciseSeeking by viewModel.preciseSeeking.collectAsStateWithLifecycle()

            CompositionLocalProvider(LocalHideStatusBar provides hideStatusBar) {
                FindroidTheme {
                    MusicPlayerScreen(
                        state = state,
                        artworkType = artworkType,
                        swipeArtwork = swipeArtwork,
                        preciseSeeking = preciseSeeking,
                        navigateBack = ::finish,
                        onOpenSettings = {
                            startActivity(
                                Intent(
                                        this@MusicPlayerActivity,
                                        PlayerSettingsActivity::class.java,
                                    )
                                    .putExtra(
                                        PlayerSettingsActivity.EXTRA_CATEGORY,
                                        SettingsR.string.settings_category_music,
                                    )
                            )
                        },
                        onPlayPause = viewModel::togglePlayPause,
                        onPrevious = viewModel::previous,
                        onNext = viewModel::next,
                        onSpeedBoost = viewModel::setSpeedBoost,
                        onSeek = viewModel::seekTo,
                        onToggleShuffle = viewModel::toggleShuffle,
                        onCycleRepeat = viewModel::cycleRepeatMode,
                        onToggleFavorite = viewModel::toggleFavorite,
                        onToggleFavoriteItem = viewModel::toggleFavoriteItem,
                        onOpenQueue = {
                            startActivity(
                                Intent(this@MusicPlayerActivity, MusicQueueActivity::class.java)
                            )
                        },
                        onArtistClick = { artistId ->
                            openMusicDetail(artistId = artistId.toString())
                        },
                        onAlbumClick = { albumId -> openMusicDetail(albumId = albumId.toString()) },
                        onPlayQueueItem = viewModel::playQueueItem,
                        onJumpQueueItem = viewModel::jumpToQueueItem,
                        onRemoveQueueItem = viewModel::removeQueueItem,
                        onMoveQueueItem = viewModel::moveQueueItem,
                        onClearError = viewModel::clearError,
                    )
                }
            }
        }
    }

    private fun openMusicDetail(artistId: String? = null, albumId: String? = null) {
        val intent =
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        artistId?.let { intent.putExtra(ExtraMusicArtistId, it) }
        albumId?.let { intent.putExtra(ExtraMusicAlbumId, it) }
        startActivity(intent)
        finish()
    }
}
