package dev.hdytrfli.onigiri.presentation.settings.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.hdytrfli.onigiri.settings.R as SettingsR
import dev.hdytrfli.onigiri.settings.domain.models.Preference as PreferenceBackend
import dev.hdytrfli.onigiri.settings.presentation.models.Preference
import dev.hdytrfli.onigiri.settings.presentation.models.PreferenceIntInput
import dev.hdytrfli.onigiri.settings.presentation.models.PreferenceLongInput
import dev.hdytrfli.onigiri.ui.components.AppButton
import dev.hdytrfli.onigiri.ui.components.AppButtonVariant
import dev.hdytrfli.onigiri.ui.components.AppTextField
import dev.hdytrfli.onigiri.ui.components.BaseDialog
import dev.hdytrfli.onigiri.ui.theme.FindroidTheme

@Composable
fun SettingsIntInputDialog(
    preference: PreferenceIntInput,
    onUpdate: (value: Int) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val suffix = preference.suffixRes?.let { stringResource(it) }

    SettingsNumberInputDialog(
        preference = preference,
        initialValue = preference.value.toString(),
        onUpdate = { value -> value.toIntOrNull()?.let { value -> onUpdate(value) } },
        onDismissRequest = onDismissRequest,
        suffix = suffix,
    )
}

@Composable
fun SettingsLongInputDialog(
    preference: PreferenceLongInput,
    onUpdate: (value: Long) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val suffix = preference.suffixRes?.let { stringResource(it) }

    SettingsNumberInputDialog(
        preference = preference,
        initialValue = preference.value.toString(),
        onUpdate = { value -> value.toLongOrNull()?.let { value -> onUpdate(value) } },
        onDismissRequest = onDismissRequest,
        suffix = suffix,
    )
}

@Composable
fun SettingsNumberInputDialog(
    preference: Preference,
    initialValue: String,
    onUpdate: (String) -> Unit,
    onDismissRequest: () -> Unit,
    suffix: String? = null,
) {
    var text by remember { mutableStateOf(initialValue) }

    val focusRequester = remember { FocusRequester() }

    // Only digits pattern
    val pattern = remember { Regex("^\\d+\$") }

    LaunchedEffect(true) { focusRequester.requestFocus() }

    val iconRes = preference.iconDrawableId
    val leadingIcon: (@Composable () -> Unit)? =
        if (iconRes != null) {
            {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            null
        }

    BaseDialog(
        title = stringResource(preference.nameStringResource),
        onDismiss = onDismissRequest,
        negativeButton = {
            AppButton(onClick = onDismissRequest, variant = AppButtonVariant.Secondary) {
                Text(text = stringResource(SettingsR.string.cancel))
            }
        },
        positiveButton = {
            AppButton(onClick = { onUpdate(text) }) {
                Text(text = stringResource(SettingsR.string.save))
            }
        },
    ) { contentPadding ->
        Column(modifier = Modifier.padding(contentPadding)) {
            AppTextField(
                value = text,
                onValueChange = { if (it.isEmpty() || it.matches(pattern)) text = it },
                label = stringResource(preference.nameStringResource),
                leadingContent = leadingIcon,
                supportingText = preference.descriptionStringRes?.let { stringResource(it) },
                modifier = Modifier.focusRequester(focusRequester),
                trailingText = suffix,
                keyboardOptions =
                    KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onUpdate(text) }),
            )
        }
    }
}

@Preview
@Composable
private fun SettingsNumberInputDialogPreview() {
    FindroidTheme {
        SettingsNumberInputDialog(
            preference =
                PreferenceIntInput(
                    nameStringResource = SettingsR.string.settings_cache_size,
                    descriptionStringRes = SettingsR.string.settings_cache_size_message,
                    backendPreference = PreferenceBackend("", 0),
                    suffixRes = SettingsR.string.mb,
                ),
            initialValue = "20",
            onUpdate = {},
            onDismissRequest = {},
        )
    }
}
