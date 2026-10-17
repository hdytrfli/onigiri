package dev.jdtech.jellyfin.presentation.settings.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.settings.R as SettingsR
import dev.jdtech.jellyfin.settings.presentation.models.PreferenceCategory

@Composable
fun SettingsCategoryCard(preference: PreferenceCategory, modifier: Modifier = Modifier) {
    SettingsBaseCard(
        preference = preference,
        modifier = modifier,
        onClick = { preference.onClick(preference) },
    )
}

@Preview
@Composable
private fun SettingsCategoryCardPreview() {
    FindroidTheme {
        SettingsCategoryCard(
            preference =
                PreferenceCategory(nameStringResource = SettingsR.string.settings_category_player)
        )
    }
}

@Preview
@Composable
private fun SettingsCategoryCardDescriptionPreview() {
    FindroidTheme {
        SettingsCategoryCard(
            preference =
                PreferenceCategory(
                    nameStringResource = SettingsR.string.settings_category_player,
                    descriptionStringRes = SettingsR.string.settings_category_player,
                )
        )
    }
}

@Preview
@Composable
private fun SettingsCategoryCardIconPreview() {
    FindroidTheme {
        SettingsCategoryCard(
            preference =
                PreferenceCategory(
                    nameStringResource = SettingsR.string.settings_category_player,
                    iconDrawableId = CoreR.drawable.ic_play,
                )
        )
    }
}
