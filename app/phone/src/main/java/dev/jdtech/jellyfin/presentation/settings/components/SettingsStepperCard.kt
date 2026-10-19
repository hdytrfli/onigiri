package dev.jdtech.jellyfin.presentation.settings.components

import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.presentation.components.AppButtonSize
import dev.jdtech.jellyfin.presentation.components.AppButtonVariant
import dev.jdtech.jellyfin.presentation.components.AppIconButton
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.settings.R as SettingsR
import dev.jdtech.jellyfin.settings.domain.models.Preference
import dev.jdtech.jellyfin.settings.presentation.models.PreferenceStepper

/**
 * A settings row with the title/description on the left and the current value between minus and
 * plus steppers on the right. The value is clamped to [PreferenceStepper.valueRange]; minus is
 * disabled at the lower bound and plus at the upper bound.
 */
@Composable
fun SettingsStepperCard(
    preference: PreferenceStepper,
    onUpdate: (value: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val range = preference.valueRange
    val value = preference.value?.toIntOrNull()?.coerceIn(range) ?: range.first

    SettingsBaseCard(
        preference = preference,
        modifier = modifier,
        trailing = {
            AppIconButton(
                onClick = { onUpdate((value - 1).coerceAtLeast(range.first).toString()) },
                enabled = preference.enabled && value > range.first,
                size = AppButtonSize.Decorator,
                variant = AppButtonVariant.Ghost,
                content = {
                    Icon(
                        painter = painterResource(CoreR.drawable.ic_minus_fat),
                        contentDescription = null,
                    )
                },
            )
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(min = 24.dp),
            )
            AppIconButton(
                onClick = { onUpdate((value + 1).coerceAtMost(range.last).toString()) },
                enabled = preference.enabled && value < range.last,
                size = AppButtonSize.Decorator,
                variant = AppButtonVariant.Ghost,
                content = {
                    Icon(
                        painter = painterResource(CoreR.drawable.ic_plus),
                        contentDescription = null,
                    )
                },
            )
        },
    )
}

@Preview
@Composable
private fun SettingsStepperCardPreview() {
    FindroidTheme {
        SettingsStepperCard(
            preference =
                PreferenceStepper(
                    nameStringResource = SettingsR.string.grid_poster_portrait,
                    descriptionStringRes = SettingsR.string.grid_poster_portrait_summary,
                    backendPreference = Preference("", "2"),
                    value = "2",
                ),
            onUpdate = {},
        )
    }
}
