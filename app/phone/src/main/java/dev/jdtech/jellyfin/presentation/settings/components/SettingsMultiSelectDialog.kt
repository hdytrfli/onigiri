package dev.jdtech.jellyfin.presentation.settings.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import dev.jdtech.jellyfin.presentation.components.AppOptionRow
import dev.jdtech.jellyfin.presentation.components.BaseDialog
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.settings.R as SettingsR
import dev.jdtech.jellyfin.settings.domain.models.Preference
import dev.jdtech.jellyfin.settings.presentation.models.PreferenceMultiSelect

@Composable
fun SettingsMultiSelectDialog(
    preference: PreferenceMultiSelect,
    options: List<Pair<String, String>>,
    onUpdate: (value: Set<String>) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val lazyListState = rememberLazyListState()

    var selectedOptions by remember { mutableStateOf(preference.value) }

    BaseDialog(
        title = stringResource(preference.nameStringResource),
        onDismiss = onDismissRequest,
        negativeButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(SettingsR.string.cancel))
            }
        },
        positiveButton = {
            TextButton(onClick = { onUpdate(selectedOptions) }) {
                Text(text = stringResource(SettingsR.string.save))
            }
        },
    ) {
        if (lazyListState.canScrollBackward) {
            HorizontalDivider()
        }
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
            state = lazyListState,
        ) {
            items(items = options, key = { it.first }) { option ->
                val checked = selectedOptions.contains(option.first)
                AppOptionRow(
                    text = option.second,
                    onClick = {
                        selectedOptions =
                            if (checked) {
                                selectedOptions - setOfNotNull(option.first)
                            } else {
                                selectedOptions + listOfNotNull(option.first)
                            }
                    },
                    selected = checked,
                    role = Role.Checkbox,
                )
            }
        }
        if (lazyListState.canScrollForward) {
            HorizontalDivider()
        }
    }
}

@Preview
@Composable
private fun SettingsMultiSelectDialogPreview() {
    FindroidTheme {
        SettingsMultiSelectDialog(
            preference =
                PreferenceMultiSelect(
                    nameStringResource =
                        SettingsR.string.pref_player_media_segments_skip_button_type,
                    backendPreference = Preference("", emptySet()),
                    options = SettingsR.array.media_segments_type,
                    optionValues = SettingsR.array.media_segments_type_values,
                ),
            options = listOf("a" to "Option A", "b" to "Option B", "c" to "Option C"),
            onUpdate = {},
            onDismissRequest = {},
        )
    }
}
