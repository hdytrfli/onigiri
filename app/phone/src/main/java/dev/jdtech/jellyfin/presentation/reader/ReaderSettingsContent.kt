package dev.jdtech.jellyfin.presentation.reader

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.jdtech.jellyfin.presentation.components.AppButton
import dev.jdtech.jellyfin.presentation.components.AppButtonGroup
import dev.jdtech.jellyfin.presentation.components.AppButtonVariant
import dev.jdtech.jellyfin.presentation.components.AppOptionRow
import dev.jdtech.jellyfin.presentation.components.SteppedSlider
import java.util.Locale
import kotlin.math.roundToInt

/**
 * The reader preferences editor, shared by the in-reader sheet and the app's Settings screen so
 * both always offer the same controls.
 */
@Composable
fun ReaderSettingsContent(
    settings: ReaderSettings,
    onSettingsChange: (ReaderSettings) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        SettingsLabel("Theme")
        AppButtonGroup {
            SettingsChoice(
                text = "Light",
                selected = settings.theme == "light",
                onClick = { onSettingsChange(settings.copy(theme = "light")) },
            )
            SettingsChoice(
                text = "Sepia",
                selected = settings.theme == "sepia",
                onClick = { onSettingsChange(settings.copy(theme = "sepia")) },
            )
            SettingsChoice(
                text = "Dark",
                selected = settings.theme == "dark",
                onClick = { onSettingsChange(settings.copy(theme = "dark")) },
            )
        }

        SettingsLabel("Brightness")
        SettingsSlider(
            value = settings.brightness,
            start = 0.2f,
            end = 1f,
            count = 17,
            valueText = "${(settings.brightness * 100).roundToInt()}%",
            onValueChange = { onSettingsChange(settings.copy(brightness = it)) },
        )

        SettingsLabel("Reading mode")
        AppButtonGroup {
            SettingsChoice(
                text = "Scroll",
                selected = settings.scroll,
                onClick = { onSettingsChange(settings.copy(scroll = true, columnCount = 1)) },
            )
            SettingsChoice(
                text = "Single page",
                selected = !settings.scroll && settings.columnCount != 2,
                onClick = { onSettingsChange(settings.copy(scroll = false, columnCount = 1)) },
            )
            SettingsChoice(
                text = "Dual page",
                selected = !settings.scroll && settings.columnCount == 2,
                onClick = { onSettingsChange(settings.copy(scroll = false, columnCount = 2)) },
            )
        }

        SettingsLabel("Font")
        AppButtonGroup {
            SettingsChoice(
                text = "Publisher",
                selected = settings.fontFamily.isEmpty(),
                onClick = { onSettingsChange(settings.copy(fontFamily = "")) },
            )
            SettingsChoice(
                text = "Serif",
                selected = settings.fontFamily == "serif",
                onClick = { onSettingsChange(settings.copy(fontFamily = "serif")) },
            )
            SettingsChoice(
                text = "Sans",
                selected = settings.fontFamily == "sans-serif",
                onClick = { onSettingsChange(settings.copy(fontFamily = "sans-serif")) },
            )
            SettingsChoice(
                text = "Mono",
                selected = settings.fontFamily == "monospace",
                onClick = { onSettingsChange(settings.copy(fontFamily = "monospace")) },
            )
        }

        SettingsSlider(
            value = settings.fontSize,
            start = 0.8f,
            end = 2f,
            count = 25,
            label = "Font size",
            valueText = String.format(Locale.US, "%.2f×", settings.fontSize),
            onValueChange = { onSettingsChange(settings.copy(fontSize = it)) },
        )

        SettingsSlider(
            value = settings.fontWeight,
            start = 0f,
            end = 1.5f,
            count = 16,
            label = "Font weight",
            valueText = String.format(Locale.US, "%.2f", settings.fontWeight),
            onValueChange = { onSettingsChange(settings.copy(fontWeight = it)) },
        )

        SettingsSlider(
            value = settings.pageMargins,
            start = 0f,
            end = 2f,
            count = 21,
            label = "Page margins",
            valueText = String.format(Locale.US, "%.2f×", settings.pageMargins),
            onValueChange = { onSettingsChange(settings.copy(pageMargins = it)) },
        )

        SettingsSlider(
            value = settings.lineHeight,
            start = 1f,
            end = 2.2f,
            count = 25,
            label = "Line spacing",
            valueText = String.format(Locale.US, "%.2f×", settings.lineHeight),
            onValueChange = { onSettingsChange(settings.copy(lineHeight = it)) },
        )

        SettingsSlider(
            value = settings.paragraphSpacing,
            start = 0f,
            end = 2f,
            count = 21,
            label = "Paragraph spacing",
            valueText = String.format(Locale.US, "%.2f", settings.paragraphSpacing),
            onValueChange = { onSettingsChange(settings.copy(paragraphSpacing = it)) },
        )

        SettingsSlider(
            value = settings.wordSpacing,
            start = 0f,
            end = 1f,
            count = 21,
            label = "Word spacing",
            valueText = String.format(Locale.US, "%.2f", settings.wordSpacing),
            onValueChange = { onSettingsChange(settings.copy(wordSpacing = it)) },
        )

        SettingsSlider(
            value = settings.letterSpacing,
            start = 0f,
            end = 1f,
            count = 21,
            label = "Letter spacing",
            valueText = String.format(Locale.US, "%.2f", settings.letterSpacing),
            onValueChange = { onSettingsChange(settings.copy(letterSpacing = it)) },
        )

        SettingsLabel("Text alignment")
        AppButtonGroup {
            SettingsChoice(
                text = "Auto",
                selected = settings.textAlign.isEmpty(),
                onClick = { onSettingsChange(settings.copy(textAlign = "")) },
            )
            SettingsChoice(
                text = "Start",
                selected = settings.textAlign == "start",
                onClick = { onSettingsChange(settings.copy(textAlign = "start")) },
            )
            SettingsChoice(
                text = "Justify",
                selected = settings.textAlign == "justify",
                onClick = { onSettingsChange(settings.copy(textAlign = "justify")) },
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        AppOptionRow(
            text = "Normalise text styles",
            selected = settings.textNormalization,
            onClick = {
                onSettingsChange(settings.copy(textNormalization = !settings.textNormalization))
            },
        )
    }
}

@Composable
private fun SettingsLabel(text: String) {
    Spacer(modifier = Modifier.height(20.dp))
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

@Composable
private fun SettingsChoice(text: String, selected: Boolean, onClick: () -> Unit) {
    AppButton(
        onClick = onClick,
        variant = if (selected) AppButtonVariant.Primary else AppButtonVariant.Secondary,
    ) {
        Text(text)
    }
}

@Composable
private fun SettingsSlider(
    value: Float,
    start: Float,
    end: Float,
    count: Int,
    valueText: String,
    onValueChange: (Float) -> Unit,
    label: String? = null,
) {
    val index =
        (((value - start) / (end - start)) * (count - 1)).roundToInt().coerceIn(0, count - 1)

    Column(modifier = Modifier.padding(top = if (label != null) 20.dp else 12.dp)) {
        if (label != null) {
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = valueText,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
                Text(
                    text = valueText,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        SteppedSlider(
            index = index,
            count = count,
            onIndexChange = { newIndex ->
                onValueChange(start + (end - start) * newIndex / (count - 1))
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
