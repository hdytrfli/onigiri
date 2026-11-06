package dev.hdytrfli.onigiri.presentation.settings.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.util.fastForEachIndexed
import dev.hdytrfli.onigiri.settings.R as SettingsR
import dev.hdytrfli.onigiri.settings.domain.models.Preference
import dev.hdytrfli.onigiri.settings.presentation.models.PreferenceAction
import dev.hdytrfli.onigiri.settings.presentation.models.PreferenceAppLanguage
import dev.hdytrfli.onigiri.settings.presentation.models.PreferenceCategory
import dev.hdytrfli.onigiri.settings.presentation.models.PreferenceFileEdit
import dev.hdytrfli.onigiri.settings.presentation.models.PreferenceGroup
import dev.hdytrfli.onigiri.settings.presentation.models.PreferenceIntInput
import dev.hdytrfli.onigiri.settings.presentation.models.PreferenceLongInput
import dev.hdytrfli.onigiri.settings.presentation.models.PreferenceMultiSelect
import dev.hdytrfli.onigiri.settings.presentation.models.PreferenceSelect
import dev.hdytrfli.onigiri.settings.presentation.models.PreferenceSlider
import dev.hdytrfli.onigiri.settings.presentation.models.PreferenceSliders
import dev.hdytrfli.onigiri.settings.presentation.models.PreferenceStepper
import dev.hdytrfli.onigiri.settings.presentation.models.PreferenceSwitch
import dev.hdytrfli.onigiri.settings.presentation.settings.SettingsAction
import dev.hdytrfli.onigiri.ui.theme.FindroidTheme
import dev.hdytrfli.onigiri.ui.theme.spacings

@Composable
fun SettingsGroupCard(
    group: PreferenceGroup,
    onAction: (SettingsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        group.nameStringResource?.let {
            Text(
                text = stringResource(it),
                modifier = Modifier.padding(start = MaterialTheme.spacings.medium),
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(modifier.height(MaterialTheme.spacings.small))
        }
        Card(
            modifier = modifier,
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
        ) {
            group.preferences.fastForEachIndexed { index, preference ->
                when (preference) {
                    is PreferenceCategory ->
                        SettingsCategoryCard(
                            preference = preference,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    is PreferenceSwitch ->
                        SettingsSwitchCard(
                            preference = preference,
                            onClick = {
                                onAction(
                                    SettingsAction.OnUpdate(
                                        preference.copy(value = !preference.value)
                                    )
                                )
                                preference.onClick(preference)
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    is PreferenceSelect ->
                        SettingsSelectCard(
                            preference = preference,
                            onUpdate = { value ->
                                onAction(SettingsAction.OnUpdate(preference.copy(value = value)))
                                preference.onUpdate(value)
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    is PreferenceSlider ->
                        SettingsSliderCard(
                            preference = preference,
                            onUpdate = { value ->
                                onAction(SettingsAction.OnUpdate(preference.copy(value = value)))
                                preference.onUpdate(value)
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    is PreferenceSliders ->
                        SettingsSlidersCard(
                            preference = preference,
                            onUpdate = { values ->
                                preference.sliders.zip(values).forEach { (slider, value) ->
                                    onAction(SettingsAction.OnUpdate(slider.copy(value = value)))
                                    slider.onUpdate(value)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    is PreferenceStepper ->
                        SettingsStepperCard(
                            preference = preference,
                            onUpdate = { value ->
                                onAction(SettingsAction.OnUpdate(preference.copy(value = value)))
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    is PreferenceMultiSelect ->
                        SettingsMultiSelectCard(
                            preference = preference,
                            onUpdate = { value ->
                                onAction(SettingsAction.OnUpdate(preference.copy(value = value)))
                                preference.onUpdate(value)
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    is PreferenceIntInput ->
                        SettingsIntInputCard(
                            preference = preference,
                            onUpdate = { value ->
                                onAction(SettingsAction.OnUpdate(preference.copy(value = value)))
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    is PreferenceLongInput ->
                        SettingsLongInputCard(
                            preference = preference,
                            onUpdate = { value ->
                                onAction(SettingsAction.OnUpdate(preference.copy(value = value)))
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    is PreferenceAppLanguage ->
                        SettingsAppLanguageCard(
                            preference = preference,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    is PreferenceFileEdit ->
                        SettingsFileEditCard(
                            preference = preference,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    is PreferenceAction ->
                        SettingsActionCard(
                            preference = preference,
                            modifier = Modifier.fillMaxWidth(),
                        )
                }
                if (index < group.preferences.lastIndex) {
                    HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.2f))
                }
            }
        }
    }
}

@Preview
@Composable
private fun SettingsGroupCardPreview() {
    FindroidTheme {
        SettingsGroupCard(
            group =
                PreferenceGroup(
                    nameStringResource = SettingsR.string.mpv_player,
                    preferences =
                        listOf(
                            PreferenceSwitch(
                                nameStringResource = SettingsR.string.mpv_player,
                                descriptionStringRes = SettingsR.string.mpv_player_summary,
                                backendPreference = Preference("", false),
                            ),
                            PreferenceSelect(
                                nameStringResource = SettingsR.string.pref_player_mpv_hwdec,
                                descriptionStringRes =
                                    SettingsR.string.pref_player_mpv_hwdec_summary,
                                dependencies = listOf(Preference("", false)),
                                backendPreference = Preference("", ""),
                                options = SettingsR.array.mpv_hwdec,
                                optionValues = SettingsR.array.mpv_hwdec,
                            ),
                            PreferenceSelect(
                                nameStringResource = SettingsR.string.pref_player_mpv_vo,
                                descriptionStringRes = SettingsR.string.pref_player_mpv_vo_summary,
                                dependencies = listOf(Preference("", false)),
                                backendPreference = Preference("", ""),
                                options = SettingsR.array.mpv_vos,
                                optionValues = SettingsR.array.mpv_vos,
                            ),
                            PreferenceSelect(
                                nameStringResource = SettingsR.string.pref_player_mpv_ao,
                                descriptionStringRes = SettingsR.string.pref_player_mpv_ao_summary,
                                dependencies = listOf(Preference("", false)),
                                backendPreference = Preference("", ""),
                                options = SettingsR.array.mpv_aos,
                                optionValues = SettingsR.array.mpv_aos,
                            ),
                        ),
                ),
            onAction = {},
        )
    }
}
