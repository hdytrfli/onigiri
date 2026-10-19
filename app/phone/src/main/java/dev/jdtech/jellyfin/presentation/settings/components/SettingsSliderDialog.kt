package dev.jdtech.jellyfin.presentation.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.jdtech.jellyfin.presentation.components.AppButton
import dev.jdtech.jellyfin.presentation.components.AppButtonVariant
import dev.jdtech.jellyfin.presentation.components.BaseDialog
import dev.jdtech.jellyfin.presentation.components.SteppedSlider
import dev.jdtech.jellyfin.presentation.components.drawableRes
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.settings.R as SettingsR
import dev.jdtech.jellyfin.settings.domain.models.Preference
import dev.jdtech.jellyfin.settings.presentation.models.OptionIcon
import dev.jdtech.jellyfin.settings.presentation.models.PreferenceSlider

/**
 * A scalar setting edited in a dialog with a stepped slider: the current step's label is shown
 * large, the description (in the dialog header) explains it, and the endpoints label the range.
 */
@Composable
fun SettingsSliderDialog(
    preference: PreferenceSlider,
    optionNames: List<String>,
    optionValues: List<String>,
    onUpdate: (String?) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val startIndex = optionValues.indexOfFirst { it == preference.value }.coerceAtLeast(0)
    var index by remember { mutableIntStateOf(startIndex) }
    val icon = preference.optionIcon?.drawableRes()

    BaseDialog(
        title = stringResource(preference.nameStringResource),
        description = preference.descriptionStringRes?.let { stringResource(it) },
        onDismiss = onDismissRequest,
        negativeButton = {
            AppButton(onClick = onDismissRequest, variant = AppButtonVariant.Secondary) {
                Text(text = stringResource(SettingsR.string.cancel))
            }
        },
        positiveButton = {
            AppButton(onClick = { onUpdate(optionValues.getOrNull(index)) }) {
                Text(text = stringResource(SettingsR.string.save))
            }
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier.fillMaxWidth().padding(contentPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (icon != null) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spacings.small))
            }
            Text(
                text = optionNames.getOrElse(index) { "" },
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacings.medium))
            SteppedSlider(
                index = index,
                count = optionNames.size,
                onIndexChange = { index = it },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = optionNames.firstOrNull().orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = optionNames.lastOrNull().orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Preview
@Composable
private fun SettingsSliderDialogPreview() {
    FindroidTheme {
        SettingsSliderDialog(
            preference =
                PreferenceSlider(
                    nameStringResource = SettingsR.string.title_portrait_width,
                    descriptionStringRes = SettingsR.string.title_portrait_width_summary,
                    optionIcon = OptionIcon.Width,
                    backendPreference = Preference("", "0.7"),
                    options = SettingsR.array.title_width_options,
                    optionValues = SettingsR.array.title_width_values,
                ),
            optionNames = listOf("40%", "50%", "60%", "70%", "80%", "90%", "100%"),
            optionValues = listOf("0.4", "0.5", "0.6", "0.7", "0.8", "0.9", "1"),
            onUpdate = {},
            onDismissRequest = {},
        )
    }
}
