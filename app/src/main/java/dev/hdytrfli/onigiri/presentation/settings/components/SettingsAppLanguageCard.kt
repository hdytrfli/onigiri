package dev.hdytrfli.onigiri.presentation.settings.components

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import dev.hdytrfli.onigiri.settings.presentation.models.Preference

@Composable
fun SettingsAppLanguageCard(preference: Preference, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current

    val currentValue = remember { configuration.locales.get(0).displayName }

    SettingsBaseCard(
        preference = preference,
        modifier = modifier,
        onClick = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.startActivity(
                    Intent(
                        Settings.ACTION_APP_LOCALE_SETTINGS,
                        "package:${context.packageName}".toUri(),
                    )
                )
            }
        },
        value = currentValue,
    )
}
