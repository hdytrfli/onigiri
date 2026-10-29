package dev.jdtech.jellyfin

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.LocalContentSpacingScale
import dev.jdtech.jellyfin.presentation.utils.BackdropRatios
import dev.jdtech.jellyfin.presentation.utils.GridColumns
import dev.jdtech.jellyfin.presentation.utils.GridGapBase
import dev.jdtech.jellyfin.presentation.utils.LocalBackdropRatios
import dev.jdtech.jellyfin.presentation.utils.LocalBlurHash
import dev.jdtech.jellyfin.presentation.utils.LocalGridColumns
import dev.jdtech.jellyfin.presentation.utils.LocalGridGap
import dev.jdtech.jellyfin.presentation.utils.LocalHideStatusBar
import dev.jdtech.jellyfin.presentation.utils.LocalOfflineMode
import dev.jdtech.jellyfin.presentation.utils.LocalPredictiveBack
import dev.jdtech.jellyfin.presentation.utils.LocalSetOfflineMode
import dev.jdtech.jellyfin.presentation.utils.LocalTitleImageConfig
import dev.jdtech.jellyfin.presentation.utils.TitleImageConfig
import dev.jdtech.jellyfin.settings.domain.AppPreferences
import dev.jdtech.jellyfin.utils.restart
import dev.jdtech.jellyfin.viewmodels.MainViewModel
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by viewModels()

    @Inject lateinit var appPreferences: AppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        // Hide the status bar immediately when enabled, so it doesn't flash on launch.
        if (appPreferences.getValue(appPreferences.hideStatusBar)) {
            WindowCompat.getInsetsController(window, window.decorView).apply {
                systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                hide(WindowInsetsCompat.Type.statusBars())
            }
        }

        setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()

            // Optionally hide the system status bar in the main (non-player) UI. Skipped while the
            // state is still loading so we don't show the bar again right after onCreate hid it.
            DisposableEffect(state.isLoading, state.hideStatusBar) {
                if (!state.isLoading) {
                    WindowCompat.getInsetsController(window, window.decorView).apply {
                        systemBarsBehavior =
                            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                        if (state.hideStatusBar) {
                            hide(WindowInsetsCompat.Type.statusBars())
                        } else {
                            show(WindowInsetsCompat.Type.statusBars())
                        }
                    }
                }
                onDispose {}
            }

            FindroidTheme {
                if (!state.isLoading) {
                    // When the status bar is hidden, consume its inset (including the display
                    // cutout, which only becomes non-zero once the bar is gone) for the whole app
                    // so no screen or the nav rail reserves that space.
                    Box(
                        modifier =
                            if (state.hideStatusBar) {
                                Modifier.consumeWindowInsets(
                                    WindowInsets.safeDrawing.only(WindowInsetsSides.Top)
                                )
                            } else {
                                Modifier
                            }
                    ) {
                        CompositionLocalProvider(
                            LocalOfflineMode provides state.isOfflineMode,
                            LocalSetOfflineMode provides
                                { enabled ->
                                    viewModel.setOfflineMode(enabled)
                                    // Same behaviour as the settings switch: relaunch the task so
                                    // the repository bindings are injected for the new mode.
                                    restart()
                                },
                            LocalBlurHash provides state.blurHash,
                            LocalPredictiveBack provides state.predictiveBack,
                            LocalTabletUiMode provides state.tabletUiMode,
                            LocalBackdropRatios provides
                                BackdropRatios(
                                    portrait = state.backdropPortraitRatio,
                                    landscape = state.backdropLandscapeRatio,
                                ),
                            LocalContentSpacingScale provides state.contentSpacingScale,
                            LocalHideStatusBar provides state.hideStatusBar,
                            LocalTitleImageConfig provides
                                TitleImageConfig(
                                    portraitWidth = state.titlePortraitWidth,
                                    landscapeWidth = state.titleLandscapeWidth,
                                ),
                            LocalGridColumns provides
                                GridColumns(
                                    posterPortrait = state.gridPosterPortrait,
                                    posterLandscape = state.gridPosterLandscape,
                                    thumbnailPortrait = state.gridThumbnailPortrait,
                                    thumbnailLandscape = state.gridThumbnailLandscape,
                                    suggestionPortrait = state.gridSuggestionPortrait,
                                    suggestionLandscape = state.gridSuggestionLandscape,
                                ),
                            LocalGridGap provides GridGapBase * state.gridGapScale,
                        ) {
                            NavigationRoot(
                                hasServers = state.hasServers,
                                hasCurrentServer = state.hasCurrentServer,
                                hasCurrentUser = state.hasCurrentUser,
                                confirmExit = state.confirmExit,
                            )
                        }
                    }
                }
            }
        }
    }
}
