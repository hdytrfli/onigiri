package dev.hdytrfli.onigiri.presentation.settings.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import dev.hdytrfli.onigiri.core.R as CoreR
import dev.hdytrfli.onigiri.settings.R as SettingsR
import dev.hdytrfli.onigiri.settings.domain.models.Preference
import dev.hdytrfli.onigiri.settings.presentation.models.PreferenceSelect
import dev.hdytrfli.onigiri.ui.components.LanguageOption
import dev.hdytrfli.onigiri.ui.components.LanguagePickerDialog
import dev.hdytrfli.onigiri.ui.components.drawableRes
import dev.hdytrfli.onigiri.ui.theme.FindroidTheme

@Composable
fun SettingsSelectCard(
    preference: PreferenceSelect,
    onUpdate: (value: String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val optionValues = stringArrayResource(preference.optionValues)
    val optionNames = stringArrayResource(preference.options)
    val notSetString = stringResource(CoreR.string.not_set)

    val options =
        remember(preference.nameStringResource) {
            val options = mutableListOf<Pair<String?, String>>()

            if (preference.optionsIncludeNull) {
                options.add(Pair(null, notSetString))
            }
            options.addAll(optionValues.zip(optionNames))

            options
        }

    val optionsMap = remember(options) { options.toMap() }

    var showDialog by remember { mutableStateOf(false) }

    SettingsBaseCard(
        preference = preference,
        modifier = modifier,
        onClick = { showDialog = true },
        value = optionsMap.getOrDefault(preference.value, notSetString),
    )

    if (showDialog) {
        if (preference.isLanguagePicker) {
            val languageOptions =
                remember(optionValues, optionNames) {
                    optionValues.mapIndexedNotNull { index, code ->
                        optionNames.getOrNull(index)?.let { LanguageOption(code, it) }
                    }
                }
            LanguagePickerDialog(
                title = stringResource(preference.nameStringResource),
                description = preference.descriptionStringRes?.let { stringResource(it) },
                options = languageOptions,
                selectedCode = preference.value,
                suggestedLabel = stringResource(CoreR.string.suggested),
                suggestedCodes = preference.suggestedValues,
                notSetLabel = notSetString.takeIf { preference.optionsIncludeNull },
                optionIcon = preference.optionIcon?.drawableRes(),
                onSelect = { value ->
                    showDialog = false
                    onUpdate(value)
                },
                onDismiss = { showDialog = false },
            )
        } else {
            SettingsSelectDialog(
                preference = preference,
                options = options,
                onUpdate = { value ->
                    showDialog = false
                    onUpdate(value)
                },
                onDismissRequest = { showDialog = false },
            )
        }
    }
}

@Preview
@Composable
private fun SettingsSelectCardPreview() {
    FindroidTheme {
        SettingsSelectCard(
            preference =
                PreferenceSelect(
                    nameStringResource = SettingsR.string.settings_preferred_audio_language,
                    descriptionStringRes =
                        SettingsR.string.settings_preferred_audio_language_summary,
                    iconDrawableId = CoreR.drawable.ic_speaker,
                    backendPreference = Preference("", ""),
                    options = SettingsR.array.languages,
                    optionValues = SettingsR.array.languages_values,
                ),
            onUpdate = {},
        )
    }
}
