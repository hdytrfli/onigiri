package dev.jdtech.jellyfin.presentation.settings.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.tooling.preview.Preview
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.settings.R as SettingsR
import dev.jdtech.jellyfin.settings.domain.models.Preference
import dev.jdtech.jellyfin.settings.presentation.models.OptionIcon
import dev.jdtech.jellyfin.settings.presentation.models.PreferenceSlider

@Composable
fun SettingsSliderCard(
    preference: PreferenceSlider,
    onUpdate: (value: String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val optionNames = stringArrayResource(preference.options)
    val optionValues = stringArrayResource(preference.optionValues)
    val index = optionValues.indexOfFirst { it == preference.value }.coerceAtLeast(0)

    var showDialog by remember { mutableStateOf(false) }

    SettingsBaseCard(
        preference = preference,
        modifier = modifier,
        onClick = { showDialog = true },
        value = optionNames.getOrElse(index) { "" },
    )

    if (showDialog) {
        SettingsSliderDialog(
            preference = preference,
            optionNames = optionNames.toList(),
            optionValues = optionValues.toList(),
            onUpdate = { value ->
                showDialog = false
                onUpdate(value)
            },
            onDismissRequest = { showDialog = false },
        )
    }
}

@Preview
@Composable
private fun SettingsSliderCardPreview() {
    FindroidTheme {
        SettingsSliderCard(
            preference =
                PreferenceSlider(
                    nameStringResource = SettingsR.string.title_portrait_width,
                    descriptionStringRes = SettingsR.string.title_image_summary,
                    optionIcon = OptionIcon.Width,
                    backendPreference = Preference("", "0.7"),
                    options = SettingsR.array.title_width_options,
                    optionValues = SettingsR.array.title_width_values,
                ),
            onUpdate = {},
        )
    }
}
