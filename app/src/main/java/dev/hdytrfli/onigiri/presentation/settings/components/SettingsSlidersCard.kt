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
import dev.hdytrfli.onigiri.settings.presentation.models.PreferenceSlider
import dev.hdytrfli.onigiri.settings.presentation.models.PreferenceSliders
import dev.hdytrfli.onigiri.ui.theme.FindroidTheme

/**
 * One setting row whose dialog holds one stepped slider per axis (portrait and landscape, say). The
 * trailing value joins each axis' current step, so a poster-count pair reads as "4 · 2".
 */
@Composable
fun SettingsSlidersCard(
    preference: PreferenceSliders,
    onUpdate: (values: List<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val value =
        preference.sliders
            .map { slider ->
                val names = stringArrayResource(slider.options)
                val values = stringArrayResource(slider.optionValues)
                val index = sliderIndex(values.toList(), slider.value)
                names.getOrElse(index) { "" }
            }
            .joinToString(" · ")

    var showDialog by remember { mutableStateOf(false) }

    SettingsBaseCard(
        preference = preference,
        modifier = modifier,
        onClick = { showDialog = true },
        value = value,
    )

    if (showDialog) {
        SettingsSlidersDialog(
            preference = preference,
            onUpdate = { values ->
                showDialog = false
                onUpdate(values)
            },
            onDismissRequest = { showDialog = false },
        )
    }
}

@Preview
@Composable
private fun SettingsSlidersCardPreview() {
    FindroidTheme {
        SettingsSlidersCard(
            preference =
                PreferenceSliders(
                    nameStringResource = SettingsR.string.grid_posters,
                    descriptionStringRes = SettingsR.string.grid_posters_summary,
                    sliders =
                        listOf(
                            PreferenceSlider(
                                nameStringResource = SettingsR.string.grid_poster_portrait,
                                descriptionStringRes = SettingsR.string.grid_posters_summary,
                                options = SettingsR.array.grid_columns,
                                optionValues = SettingsR.array.grid_columns,
                                backendPreference = Preference("", "2"),
                            ),
                            PreferenceSlider(
                                nameStringResource = SettingsR.string.grid_poster_landscape,
                                descriptionStringRes = SettingsR.string.grid_posters_summary,
                                options = SettingsR.array.grid_columns,
                                optionValues = SettingsR.array.grid_columns,
                                backendPreference = Preference("", "4"),
                            ),
                        ),
                ),
            onUpdate = {},
        )
    }
}
