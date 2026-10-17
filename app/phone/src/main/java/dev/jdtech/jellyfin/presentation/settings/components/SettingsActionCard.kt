package dev.jdtech.jellyfin.presentation.settings.components

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.settings.R as SettingsR
import dev.jdtech.jellyfin.settings.domain.models.Preference
import dev.jdtech.jellyfin.settings.presentation.models.PreferenceAction

/**
 * A settings row with the title/description on the left and the current value followed by a chevron
 * on the right. Clicking it runs the preference's own action instead of opening a dialog.
 */
@Composable
fun SettingsActionCard(preference: PreferenceAction, modifier: Modifier = Modifier) {
    SettingsBaseCard(
        preference = preference,
        modifier = modifier,
        onClick = { preference.onClick(preference) },
        value = preference.value,
        trailing = {
            Icon(
                painter = painterResource(CoreR.drawable.ic_chevron_down),
                contentDescription = null,
                modifier = Modifier.rotate(-90f),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
    )
}

@Preview
@Composable
private fun SettingsActionCardPreview() {
    FindroidTheme {
        SettingsActionCard(
            preference =
                PreferenceAction(
                    nameStringResource = SettingsR.string.pref_player_mpv_sub_font,
                    descriptionStringRes = SettingsR.string.mpv_sub_group_text,
                    backendPreference = Preference("", "sans-serif"),
                    value = "Default (Roboto)",
                )
        )
    }
}
