package dev.jdtech.jellyfin

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.LocalContentSpacingScale
import dev.jdtech.jellyfin.presentation.utils.BackdropRatios
import dev.jdtech.jellyfin.presentation.utils.LocalBackdropRatios
import dev.jdtech.jellyfin.presentation.utils.LocalNaiveTitleOverlay
import dev.jdtech.jellyfin.presentation.utils.LocalOfflineMode
import dev.jdtech.jellyfin.presentation.utils.LocalTitleImageConfig
import dev.jdtech.jellyfin.presentation.utils.TitleImageConfig
import dev.jdtech.jellyfin.viewmodels.MainViewModel

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()

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
                            navigationAnimation = state.navigationAnimation,
                        )
                    }
                }
            }
        }
    }
}
