package dev.hdytrfli.onigiri.presentation.settings.components

import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import dev.hdytrfli.onigiri.settings.R as SettingsR
import dev.hdytrfli.onigiri.settings.domain.models.Preference
import dev.hdytrfli.onigiri.settings.presentation.models.PreferenceSwitch
import dev.hdytrfli.onigiri.ui.theme.FindroidTheme

@Composable
fun SettingsSwitchCard(
    preference: PreferenceSwitch,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsBaseCard(
        preference = preference,
        modifier = modifier,
        onClick = onClick,
        trailing = {
            Switch(
                checked = preference.enabled && preference.value,
                onCheckedChange = { onClick() },
                enabled = preference.enabled,
            )
        },
    )
}

@Preview
@Composable
private fun SettingsSwitchCardPreview() {
    FindroidTheme {
        SettingsSwitchCard(
            preference =
                PreferenceSwitch(
                    nameStringResource = SettingsR.string.settings_use_cache_title,
                    descriptionStringRes = SettingsR.string.settings_use_cache_summary,
                    backendPreference = Preference("", true),
                    value = false,
                ),
            onClick = {},
        )
    }
}

@Preview
@Composable
private fun SettingsSwitchCardDisabledPreview() {
    FindroidTheme {
        SettingsSwitchCard(
            preference =
                PreferenceSwitch(
                    nameStringResource = SettingsR.string.settings_use_cache_title,
                    descriptionStringRes = SettingsR.string.settings_use_cache_summary,
                    backendPreference = Preference("", true),
                    enabled = false,
                ),
            onClick = {},
        )
    }
}
