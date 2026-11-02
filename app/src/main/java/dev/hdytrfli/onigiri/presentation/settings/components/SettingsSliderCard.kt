package dev.hdytrfli.onigiri.presentation.settings.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.tooling.preview.Preview
import dev.hdytrfli.onigiri.settings.R as SettingsR
import dev.hdytrfli.onigiri.settings.domain.models.Preference
import dev.hdytrfli.onigiri.settings.presentation.models.OptionIcon
import dev.hdytrfli.onigiri.settings.presentation.models.PreferenceSlider
import dev.hdytrfli.onigiri.ui.theme.FindroidTheme

@Composable
fun SettingsSliderCard(
    preference: PreferenceSlider,
    onUpdate: (value: String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val optionNames = stringArrayResource(preference.options)
    val optionValues = stringArrayResource(preference.optionValues)
    val index = sliderIndex(optionValues.toList(), preference.value)

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

/** Finds a stored value in the slider options, tolerating `1.5` vs `1.50` formatting. */
internal fun sliderIndex(optionValues: List<String>, value: String?): Int {
    val exact = optionValues.indexOfFirst { it == value }
    if (exact >= 0) return exact
    val number = value?.toFloatOrNull() ?: return 0
    return optionValues.indexOfFirst { it.toFloatOrNull() == number }.coerceAtLeast(0)
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
