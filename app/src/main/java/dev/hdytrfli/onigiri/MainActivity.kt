package dev.hdytrfli.onigiri

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
import dev.hdytrfli.onigiri.settings.domain.AppPreferences
import dev.hdytrfli.onigiri.swr.SwrStore
import dev.hdytrfli.onigiri.ui.theme.FindroidTheme
import dev.hdytrfli.onigiri.ui.theme.LocalContentSpacingScale
import dev.hdytrfli.onigiri.ui.utils.BackdropRatios
import dev.hdytrfli.onigiri.ui.utils.GridColumns
import dev.hdytrfli.onigiri.ui.utils.GridGapBase
import dev.hdytrfli.onigiri.ui.utils.LocalBackdropRatios
import dev.hdytrfli.onigiri.ui.utils.LocalBlurHash
import dev.hdytrfli.onigiri.ui.utils.LocalGridColumns
import dev.hdytrfli.onigiri.ui.utils.LocalGridGap
import dev.hdytrfli.onigiri.ui.utils.LocalHideStatusBar
import dev.hdytrfli.onigiri.ui.utils.LocalOfflineMode
import dev.hdytrfli.onigiri.ui.utils.LocalPredictiveBack
import dev.hdytrfli.onigiri.ui.utils.LocalSetOfflineMode
import dev.hdytrfli.onigiri.ui.utils.LocalTitleImageConfig
import dev.hdytrfli.onigiri.ui.utils.TitleImageConfig
import dev.hdytrfli.onigiri.viewmodels.MainViewModel
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by viewModels()

    @Inject lateinit var appPreferences: AppPreferences
    @Inject lateinit var swrStore: SwrStore

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
                                    profilePortrait = state.gridProfilePortrait,
                                    profileLandscape = state.gridProfileLandscape,
                                ),
                            LocalGridGap provides GridGapBase * state.gridGapScale,
                        ) {
                            NavigationRoot(
                                hasServers = state.hasServers,
                                hasCurrentServer = state.hasCurrentServer,
                                hasCurrentUser = state.hasCurrentUser,
                                onboardingComplete = state.onboardingComplete,
                                profileLoggedOut = state.profileLoggedOut,
                                confirmExit = state.confirmExit,
                                swrStore = swrStore,
                            )
                        }
                    }
                }
            }
        }
    }
}
