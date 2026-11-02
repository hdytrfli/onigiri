package dev.hdytrfli.onigiri.presentation.settings.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.util.fastFilterNotNull
import dev.hdytrfli.onigiri.settings.R as SettingsR
import dev.hdytrfli.onigiri.settings.domain.models.Preference as PreferenceBackend
import dev.hdytrfli.onigiri.settings.presentation.models.Preference
import dev.hdytrfli.onigiri.settings.presentation.models.PreferenceIntInput
import dev.hdytrfli.onigiri.settings.presentation.models.PreferenceLongInput
import dev.hdytrfli.onigiri.ui.theme.FindroidTheme

@Composable
fun SettingsIntInputCard(
    preference: PreferenceIntInput,
    onUpdate: (value: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDialog by remember { mutableStateOf(false) }

    val prefix = preference.prefixRes?.let { stringResource(it) }

    val suffix = preference.suffixRes?.let { stringResource(it) }

    SettingsNumberInputCard(
        preference = preference,
        text = listOf(prefix, preference.value, suffix).fastFilterNotNull().joinToString(" "),
        onClick = { showDialog = true },
        modifier = modifier,
    )

    if (showDialog) {
        SettingsIntInputDialog(
            preference = preference,
            onUpdate = { value ->
                showDialog = false
                onUpdate(value)
            },
            onDismissRequest = { showDialog = false },
        )
    }
}

@Composable
fun SettingsLongInputCard(
    preference: PreferenceLongInput,
    onUpdate: (value: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDialog by remember { mutableStateOf(false) }

    val prefix = preference.prefixRes?.let { stringResource(it) }

    val suffix = preference.suffixRes?.let { stringResource(it) }

    SettingsNumberInputCard(
        preference = preference,
        text = listOf(prefix, preference.value, suffix).fastFilterNotNull().joinToString(" "),
        onClick = { showDialog = true },
        modifier = modifier,
    )

    if (showDialog) {
        SettingsLongInputDialog(
            preference = preference,
            onUpdate = { value ->
                showDialog = false
                onUpdate(value)
            },
            onDismissRequest = { showDialog = false },
        )
    }
}

@Composable
fun SettingsNumberInputCard(
    preference: Preference,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsBaseCard(
        preference = preference,
        modifier = modifier,
        onClick = onClick,
        value = text,
    )
}

@Preview
@Composable
private fun SettingsIntInputCardPreview() {
    FindroidTheme {
        SettingsIntInputCard(
            preference =
                PreferenceIntInput(
                    nameStringResource = SettingsR.string.settings_cache_size,
                    backendPreference = PreferenceBackend("", 0),
                    suffixRes = SettingsR.string.mb,
                    value = 25,
                ),
            onUpdate = {},
        )
    }
}

@Preview
@Composable
private fun SettingsLongInputCardPreview() {
    FindroidTheme {
        SettingsLongInputCard(
            preference =
                PreferenceLongInput(
                    nameStringResource = SettingsR.string.settings_cache_size,
                    backendPreference = PreferenceBackend("", 0L),
                    suffixRes = SettingsR.string.mb,
                    value = 25,
                ),
            onUpdate = {},
        )
    }
}
