package dev.jdtech.jellyfin.presentation.settings.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.settings.R as SettingsR
import dev.jdtech.jellyfin.settings.domain.models.Preference
import dev.jdtech.jellyfin.settings.presentation.models.PreferenceMultiSelect

@Composable
fun SettingsMultiSelectCard(
    preference: PreferenceMultiSelect,
    onUpdate: (value: Set<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val optionValues = stringArrayResource(preference.optionValues)
    val optionNames = stringArrayResource(preference.options)
    val noneString = stringResource(CoreR.string.none)

    val options = remember(preference.nameStringResource) { optionValues.zip(optionNames) }

    val optionsMap = remember(options) { options.toMap() }

    var showDialog by remember { mutableStateOf(false) }

    SettingsBaseCard(
        preference = preference,
        modifier = modifier,
        onClick = { showDialog = true },
        value =
            preference.value
                .joinToString(", ") { key -> optionsMap[key].toString() }
                .ifEmpty { noneString },
    )

    if (showDialog) {
        SettingsMultiSelectDialog(
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

@Preview
@Composable
private fun SettingsSelectCardPreview() {
    FindroidTheme {
        SettingsMultiSelectCard(
            preference =
                PreferenceMultiSelect(
                    nameStringResource =
                        SettingsR.string.pref_player_media_segments_skip_button_type,
                    iconDrawableId = CoreR.drawable.ic_speaker,
                    backendPreference = Preference("", setOf("INTRO", "OUTRO")),
                    options = SettingsR.array.media_segments_type,
                    optionValues = SettingsR.array.media_segments_type_values,
                ),
            onUpdate = {},
        )
    }
}
