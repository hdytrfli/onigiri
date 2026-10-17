package dev.jdtech.jellyfin.presentation.settings.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.settings.R as SettingsR
import dev.jdtech.jellyfin.settings.presentation.models.PreferenceFileEdit

@Composable
fun SettingsFileEditCard(preference: PreferenceFileEdit, modifier: Modifier = Modifier) {
    val fileName = preference.filePath.split("/").last()

    SettingsBaseCard(
        preference = preference,
        modifier = modifier,
        onClick = { preference.onClick(preference) },
        title = stringResource(preference.nameStringResource, fileName),
    )
}

@Preview
@Composable
private fun SettingsFileEditCardPreview() {
    FindroidTheme {
        SettingsFileEditCard(
            preference =
                PreferenceFileEdit(
                    nameStringResource = SettingsR.string.settings_category_player,
                    filePath = "",
                )
        )
    }
}

@Preview
@Composable
private fun SettingsFileEditCardDescriptionPreview() {
    FindroidTheme {
        SettingsFileEditCard(
            preference =
                PreferenceFileEdit(
                    nameStringResource = SettingsR.string.settings_category_player,
                    descriptionStringRes = SettingsR.string.settings_category_player,
                    filePath = "",
                )
        )
    }
}

@Preview
@Composable
private fun SettingsFileEditCardIconPreview() {
    FindroidTheme {
        SettingsFileEditCard(
            preference =
                PreferenceFileEdit(
                    nameStringResource = SettingsR.string.settings_category_player,
                    iconDrawableId = CoreR.drawable.ic_play,
                    filePath = "",
                )
        )
    }
}
