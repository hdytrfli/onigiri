package dev.jdtech.jellyfin

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dagger.hilt.android.AndroidEntryPoint
import dev.jdtech.jellyfin.presentation.reader.ReaderFontScreen
import dev.jdtech.jellyfin.presentation.settings.SettingsScreen
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.settings.R as SettingsR

/** Hosts the standard Reader settings page when it is opened from inside the reader. */
@AndroidEntryPoint
class ReaderSettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            FindroidTheme {
                var showFontPicker by remember { mutableStateOf(false) }

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
