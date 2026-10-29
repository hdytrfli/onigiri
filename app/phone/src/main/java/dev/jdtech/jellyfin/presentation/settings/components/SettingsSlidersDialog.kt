package dev.jdtech.jellyfin.presentation.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import dev.jdtech.jellyfin.presentation.components.AppButton
import dev.jdtech.jellyfin.presentation.components.AppButtonVariant
import dev.jdtech.jellyfin.presentation.components.BaseDialog
import dev.jdtech.jellyfin.presentation.components.SteppedSlider
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.settings.R as SettingsR
import dev.jdtech.jellyfin.settings.presentation.models.PreferenceSliders

/** One stepped-slider row of [SettingsSlidersDialog]: its label, step names, and step values. */
private data class SliderRow(
    val nameStringResource: Int,
    val names: List<String>,
    val values: List<String>,
    val startIndex: Int,
)

/**
 * A multi-axis setting edited with a stepped slider per row - a column count for portrait and
 * landscape, for example. Each row shows its current value like the single slider dialog, and
 * saving returns one value per row for the caller to apply to the rows' backend preferences.
 */
@Composable
fun SettingsSlidersDialog(
    preference: PreferenceSliders,
    onUpdate: (values: List<String>) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val rows =
        preference.sliders.map { slider ->
            val names = stringArrayResource(slider.options).toList()
            val values = stringArrayResource(slider.optionValues).toList()
            SliderRow(
                nameStringResource = slider.nameStringResource,
                names = names,
                values = values,
                startIndex = sliderIndex(values, slider.value),
            )
        }
    val indexes: SnapshotStateList<Int> =
        remember(rows) { rows.map { it.startIndex }.toMutableStateList() }

    BaseDialog(
        title = stringResource(preference.nameStringResource),
        onDismiss = onDismissRequest,
        negativeButton = {
            AppButton(onClick = onDismissRequest, variant = AppButtonVariant.Secondary) {
                Text(text = stringResource(SettingsR.string.cancel))
            }
        },
        positiveButton = {
            AppButton(
                onClick = { onUpdate(rows.mapIndexed { i, row -> row.values[indexes[i]] }) }
            ) {
                Text(text = stringResource(SettingsR.string.save))
            }
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier.fillMaxWidth().padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.medium),
        ) {
            rows.forEachIndexed { i, row ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(row.nameStringResource),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = row.names.getOrElse(indexes[i]) { "" },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small),
                    ) {
                        Text(
                            text = row.names.firstOrNull().orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline,
                        )
                        SteppedSlider(
                            index = indexes[i],
                            count = row.names.size,
                            onIndexChange = { indexes[i] = it },
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = row.names.lastOrNull().orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                }
            }
            preference.descriptionStringRes?.let {
                Text(
                    text = stringResource(it),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
