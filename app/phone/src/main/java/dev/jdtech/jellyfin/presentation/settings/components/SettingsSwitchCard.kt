package dev.jdtech.jellyfin.presentation.settings.components

import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.settings.R as SettingsR
import dev.jdtech.jellyfin.settings.domain.models.Preference
import dev.jdtech.jellyfin.settings.presentation.models.PreferenceSwitch

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
                    backendPreference = Preference("", true),
                    enabled = false,
                ),
            onClick = {},
        )
    }
}
