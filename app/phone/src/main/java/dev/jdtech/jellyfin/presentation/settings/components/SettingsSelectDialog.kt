package dev.jdtech.jellyfin.presentation.settings.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import dev.jdtech.jellyfin.presentation.components.AppOptionRow
import dev.jdtech.jellyfin.presentation.components.BaseDialog
import dev.jdtech.jellyfin.presentation.components.drawableRes
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.settings.R as SettingsR
import dev.jdtech.jellyfin.settings.domain.models.Preference
import dev.jdtech.jellyfin.settings.presentation.models.OptionIcon
import dev.jdtech.jellyfin.settings.presentation.models.PreferenceSelect

@Composable
fun SettingsSelectDialog(
    preference: PreferenceSelect,
    options: List<Pair<String?, String>>,
    onUpdate: (value: String?) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val lazyListState = rememberLazyListState()

    val optionIcon = preference.optionIcon?.drawableRes()

    BaseDialog(
        title = stringResource(preference.nameStringResource),
        onDismiss = onDismissRequest,
    ) {
        if (lazyListState.canScrollBackward) {
            HorizontalDivider()
        }

        LazyColumn(modifier = Modifier.fillMaxWidth(), state = lazyListState) {
            items(items = options, key = { it.first ?: "null" }) { option ->
                AppOptionRow(
                    text = option.second,
                    onClick = { onUpdate(option.first) },
                    selected = option.first == preference.value,
                    iconDrawableId = optionIcon,
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
