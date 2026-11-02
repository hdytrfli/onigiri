package dev.hdytrfli.onigiri.feature.film.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import dev.hdytrfli.onigiri.core.R as CoreR
import dev.hdytrfli.onigiri.settings.presentation.models.OptionIcon
import dev.hdytrfli.onigiri.ui.components.AppOptionRow
import dev.hdytrfli.onigiri.ui.components.BaseDialog
import dev.hdytrfli.onigiri.ui.components.drawableRes
import dev.hdytrfli.onigiri.ui.theme.FindroidTheme

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
