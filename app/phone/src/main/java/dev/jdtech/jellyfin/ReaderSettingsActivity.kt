package dev.jdtech.jellyfin

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import dagger.hilt.android.AndroidEntryPoint
import dev.jdtech.jellyfin.presentation.reader.ReaderFontScreen
import dev.jdtech.jellyfin.presentation.settings.SettingsScreen
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.utils.LocalHideStatusBar
import dev.jdtech.jellyfin.settings.R as SettingsR
import dev.jdtech.jellyfin.settings.domain.AppPreferences
import javax.inject.Inject

/** Hosts the standard Reader settings page when it is opened from inside the reader. */
@AndroidEntryPoint
class ReaderSettingsActivity : AppCompatActivity() {
    @Inject lateinit var appPreferences: AppPreferences

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
            CompositionLocalProvider(LocalHideStatusBar provides hideStatusBar) {
                FindroidTheme {
                    var showFontPicker by remember { mutableStateOf(false) }

                    BackHandler(enabled = showFontPicker) { showFontPicker = false }

                    if (showFontPicker) {
                        ReaderFontScreen(navigateBack = { showFontPicker = false })
                    } else {
                        SettingsScreen(
                            indexes = listOf(SettingsR.string.settings_category_reader),
                            navigateToSettings = {},
                            navigateToSettingsFileEdit = {},
                            navigateToServers = {},
                            navigateToUsers = {},
                            navigateToAbout = {},
                            navigateToSubtitleFont = {},
                            navigateToReaderFont = { showFontPicker = true },
                            navigateBack = { finish() },
                        )
                    }
                }
            }
        }
    }
}
