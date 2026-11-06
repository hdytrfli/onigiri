package dev.hdytrfli.onigiri.presentation.settings.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import dev.hdytrfli.onigiri.settings.R as SettingsR
import dev.hdytrfli.onigiri.settings.domain.models.Preference
import dev.hdytrfli.onigiri.settings.presentation.models.OptionIcon
import dev.hdytrfli.onigiri.settings.presentation.models.PreferenceSelect
import dev.hdytrfli.onigiri.ui.components.AppOptionRow
import dev.hdytrfli.onigiri.ui.components.BaseDialog
import dev.hdytrfli.onigiri.ui.components.drawableRes
import dev.hdytrfli.onigiri.ui.theme.FindroidTheme

@Composable
fun SettingsSelectDialog(
    preference: PreferenceSelect,
    options: List<Pair<String?, String>>,
    onUpdate: (value: String?) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val lazyListState = rememberLazyListState()

    BaseDialog(
        title = stringResource(preference.nameStringResource),
        description = preference.descriptionStringRes?.let { stringResource(it) },
        onDismiss = onDismissRequest,
    ) {
        LazyColumn(modifier = Modifier.fillMaxWidth(), state = lazyListState) {
            items(items = options, key = { it.first ?: "null" }) { option ->
                AppOptionRow(
                    text = option.second,
                    onClick = { onUpdate(option.first) },
                    selected = option.first == preference.value,
                    iconDrawableId = preference.optionIcon?.drawableRes(option.first),
                    role = Role.RadioButton,
                )
            }
        }
    }
}

@Preview
@Composable
private fun SettingsSelectDialogPreview() {
    FindroidTheme {
        SettingsSelectDialog(
            preference =
                PreferenceSelect(
                    nameStringResource = SettingsR.string.settings_preferred_audio_language,
                    descriptionStringRes =
                        SettingsR.string.settings_preferred_audio_language_summary,
                    optionIcon = OptionIcon.Audio,
                    backendPreference = Preference("", ""),
                    options = SettingsR.array.languages,
                    optionValues = SettingsR.array.languages_values,
                ),
            options = listOf("a" to "Option A", "b" to "Option B", "c" to "Option C"),
            onUpdate = {},
            onDismissRequest = {},
        )
    }
}
