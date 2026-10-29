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
import dev.jdtech.jellyfin.settings.presentation.models.PreferenceSlider
import dev.jdtech.jellyfin.settings.presentation.models.PreferenceSliders

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
                                options = SettingsR.array.grid_columns,
                                optionValues = SettingsR.array.grid_columns,
                                backendPreference = Preference("", "2"),
                            ),
                            PreferenceSlider(
                                nameStringResource = SettingsR.string.grid_poster_landscape,
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
