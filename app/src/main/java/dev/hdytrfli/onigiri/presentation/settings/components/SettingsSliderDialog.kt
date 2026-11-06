package dev.hdytrfli.onigiri.presentation.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import dev.hdytrfli.onigiri.settings.R as SettingsR
import dev.hdytrfli.onigiri.settings.domain.models.Preference
import dev.hdytrfli.onigiri.settings.presentation.models.OptionIcon
import dev.hdytrfli.onigiri.settings.presentation.models.PreferenceSlider
import dev.hdytrfli.onigiri.ui.components.AppButton
import dev.hdytrfli.onigiri.ui.components.AppButtonVariant
import dev.hdytrfli.onigiri.ui.components.BaseDialog
import dev.hdytrfli.onigiri.ui.components.SteppedSlider
import dev.hdytrfli.onigiri.ui.theme.FindroidTheme
import dev.hdytrfli.onigiri.ui.theme.spacings

/**
 * A scalar setting edited with a stepped slider. The current value reads as a sentence, the
 * endpoints label the range, and the preference's description explains it underneath.
 */
@Composable
fun SettingsSliderDialog(
    preference: PreferenceSlider,
    optionNames: List<String>,
    optionValues: List<String>,
    onUpdate: (String?) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val startIndex = sliderIndex(optionValues, preference.value)
    var index by remember { mutableIntStateOf(startIndex) }
    val currentValue = optionNames.getOrElse(index) { "" }
    val currentValuePrefix = stringResource(SettingsR.string.current_value)

    BaseDialog(
        title = stringResource(preference.nameStringResource),
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
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.medium),
        ) {
            Text(
                text =
                    buildAnnotatedString {
                        append(currentValuePrefix)
                        append(" ")
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(currentValue) }
                    },
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small),
            ) {
                Text(
                    text = optionNames.firstOrNull().orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                )
                SteppedSlider(
                    index = index,
                    count = optionNames.size,
                    onIndexChange = { index = it },
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = optionNames.lastOrNull().orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
            preference.descriptionStringRes?.let {
                Text(
                    text = stringResource(it),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
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
                    descriptionStringRes = SettingsR.string.title_image_summary,
                    optionIcon = OptionIcon.Width,
                    backendPreference = Preference("", "0.7"),
                    options = SettingsR.array.title_width_options,
                    optionValues = SettingsR.array.title_width_values,
                ),
            optionNames =
                listOf("0%", "10%", "20%", "30%", "40%", "50%", "60%", "70%", "80%", "90%", "100%"),
            optionValues =
                listOf("0", "0.1", "0.2", "0.3", "0.4", "0.5", "0.6", "0.7", "0.8", "0.9", "1"),
            onUpdate = {},
            onDismissRequest = {},
        )
    }
}
