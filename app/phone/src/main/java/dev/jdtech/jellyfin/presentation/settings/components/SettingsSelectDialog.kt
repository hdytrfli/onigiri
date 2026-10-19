package dev.jdtech.jellyfin.presentation.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.presentation.components.BaseDialog
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.settings.R as SettingsR
import dev.jdtech.jellyfin.settings.domain.models.Preference
import dev.jdtech.jellyfin.settings.presentation.models.PreferenceSelect

@Composable
fun SettingsSelectDialog(
    preference: PreferenceSelect,
    options: List<Pair<String?, String>>,
    onUpdate: (value: String?) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val lazyListState = rememberLazyListState()

    BaseDialog(
        title = stringResource(preference.nameStringResource),
        onDismiss = onDismissRequest,
    ) {
        if (lazyListState.canScrollBackward) {
            HorizontalDivider()
        }

        LazyColumn(modifier = Modifier.fillMaxWidth(), state = lazyListState) {
            items(items = options, key = { it.first ?: "null" }) { option ->
                SettingsSelectDialogItem(
                    option = option,
                    isSelected = option.first == preference.value,
                    iconDrawableId = preference.iconDrawableId,
                    onSelect = onUpdate,
                )
            }
        }
    }
}

/**
 * One option row, styled like the player sheet's track list: a leading icon, the label, and a
 * trailing check mark that only shows for the current selection.
 */
@Composable
private fun SettingsSelectDialogItem(
    option: Pair<String?, String>,
    isSelected: Boolean,
    iconDrawableId: Int?,
    onSelect: (String?) -> Unit,
) {
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .selectable(
                    selected = isSelected,
                    role = Role.RadioButton,
                    onClick = { onSelect(option.first) },
                )
                .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (iconDrawableId != null) {
            Box(
                modifier =
                    Modifier.size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerLow),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(iconDrawableId),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.width(MaterialTheme.spacings.medium))
        }

        Text(
            text = option.second,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )

        Spacer(modifier = Modifier.width(MaterialTheme.spacings.medium))

        Icon(
            painter = painterResource(CoreR.drawable.ic_check),
            contentDescription = null,
            modifier = Modifier.size(24.dp).alpha(if (isSelected) 1f else 0f),
            tint = MaterialTheme.colorScheme.primary,
        )
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
                    iconDrawableId = CoreR.drawable.ic_speaker,
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
