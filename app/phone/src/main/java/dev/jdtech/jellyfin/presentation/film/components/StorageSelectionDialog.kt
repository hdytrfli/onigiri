package dev.jdtech.jellyfin.presentation.film.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.presentation.components.AppOptionRow
import dev.jdtech.jellyfin.presentation.components.BaseDialog
import dev.jdtech.jellyfin.presentation.components.drawableRes
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.settings.presentation.models.OptionIcon

@Composable
fun StorageSelectionDialog(
    storageLocations: List<String>,
    onSelect: (storageIndex: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    BaseDialog(
        title = stringResource(CoreR.string.select_storage_location),
        description = stringResource(CoreR.string.select_storage_location_summary),
        onDismiss = onDismiss,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            storageLocations.forEachIndexed { index, storageLocation ->
                AppOptionRow(
                    text = storageLocation,
                    onClick = { onSelect(index) },
                    showCheck = false,
                    iconDrawableId = OptionIcon.Storage.drawableRes(),
                )
            }
        }
    }
}

@Composable
@Preview
private fun StorageSelectionDialogPreview() {
    FindroidTheme {
        StorageSelectionDialog(
            storageLocations = listOf("Internal", "External"),
            onSelect = {},
            onDismiss = {},
        )
    }
}
