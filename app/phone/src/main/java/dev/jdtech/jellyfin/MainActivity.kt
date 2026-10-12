package dev.jdtech.jellyfin

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.LocalContentSpacingScale
import dev.jdtech.jellyfin.presentation.utils.BackdropRatios
import dev.jdtech.jellyfin.presentation.utils.LocalBackdropRatios
import dev.jdtech.jellyfin.presentation.utils.LocalHideStatusBar
import dev.jdtech.jellyfin.presentation.utils.LocalNaiveTitleOverlay
import dev.jdtech.jellyfin.presentation.utils.LocalOfflineMode
import dev.jdtech.jellyfin.presentation.utils.LocalTitleImageConfig
import dev.jdtech.jellyfin.presentation.utils.TitleImageConfig
import dev.jdtech.jellyfin.settings.domain.AppPreferences
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
            WindowCompat.getInsetsController(window, window.decorView)
                .hide(WindowInsetsCompat.Type.statusBars())
        }

        setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()

            // Optionally hide the system status bar in the main (non-player) UI.
            DisposableEffect(state.hideStatusBar) {
                val controller = WindowCompat.getInsetsController(window, window.decorView)
                if (state.hideStatusBar) {
                    controller.hide(WindowInsetsCompat.Type.statusBars())
                } else {
                    controller.show(WindowInsetsCompat.Type.statusBars())
                }
                onDispose {}
            }

            FindroidTheme(dynamicColor = state.isDynamicColors) {
                if (!state.isLoading) {
                    CompositionLocalProvider(
                        LocalOfflineMode provides state.isOfflineMode,
                        LocalBackdropRatios provides
                            BackdropRatios(
                                portrait = state.backdropPortraitRatio,
                                landscape = state.backdropLandscapeRatio,
                            ),
                        LocalContentSpacingScale provides state.contentSpacingScale,
                        LocalHideStatusBar provides state.hideStatusBar,
                        LocalNaiveTitleOverlay provides state.naiveTitleOverlay,
                        LocalTitleImageConfig provides
                            TitleImageConfig(
                                portraitWidth = state.titlePortraitWidth,
                                landscapeWidth = state.titleLandscapeWidth,
                                ratio = state.titleImageRatio,
                            ),
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
