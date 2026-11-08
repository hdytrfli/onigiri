package dev.hdytrfli.onigiri

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
import dev.hdytrfli.onigiri.presentation.settings.SettingsFileEditScreen
import dev.hdytrfli.onigiri.presentation.settings.SettingsScreen
import dev.hdytrfli.onigiri.presentation.settings.SubtitleFontScreen
import dev.hdytrfli.onigiri.settings.R as SettingsR
import dev.hdytrfli.onigiri.settings.domain.AppPreferences
import dev.hdytrfli.onigiri.ui.theme.FindroidTheme
import dev.hdytrfli.onigiri.ui.utils.LocalHideStatusBar
import javax.inject.Inject

/** Hosts Player settings as a standalone page with no navigation suite. */
@AndroidEntryPoint
class PlayerSettingsActivity : AppCompatActivity() {
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
                    var indexes by remember {
                        mutableStateOf(listOf(SettingsR.string.settings_category_player))
                    }
                    var filePath by remember { mutableStateOf<String?>(null) }
                    var showSubtitleFont by remember { mutableStateOf(false) }

                    BackHandler(enabled = showSubtitleFont) { showSubtitleFont = false }
                    BackHandler(enabled = filePath != null) { filePath = null }

                    when {
                        showSubtitleFont ->
                            SubtitleFontScreen(navigateBack = { showSubtitleFont = false })
                        filePath != null ->
                            SettingsFileEditScreen(
                                filePath = filePath.orEmpty(),
                                navigateBack = { filePath = null },
                            )
                        else ->
                            SettingsScreen(
                                indexes = indexes,
                                navigateToSettings = { indexes = it },
                                navigateToSettingsFileEdit = { filePath = it },
                                navigateToAbout = {},
                                navigateToSubtitleFont = { showSubtitleFont = true },
                                navigateToReaderFont = {},
                                navigateBack = {
                                    if (indexes.size > 1) {
                                        indexes = indexes.dropLast(1)
                                    } else {
                                        finish()
                                    }
                                },
                            )
                    }
                }
            }
        }
    }
}
