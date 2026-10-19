package dev.jdtech.jellyfin.presentation.film.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.presentation.components.BaseDialog
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme

@Composable
fun CancelDownloadDialog(onCancel: () -> Unit, onDismiss: () -> Unit) {
    BaseDialog(
        title = stringResource(CoreR.string.cancel_download),
        onDismiss = onDismiss,
        negativeButton = {
            TextButton(onClick = onDismiss) { Text(text = stringResource(CoreR.string.cancel)) }
        },
        positiveButton = {
            TextButton(onClick = onCancel) {
                Text(text = stringResource(CoreR.string.stop_download))
            }
        },
    ) { contentPadding ->
        Text(
            text = stringResource(CoreR.string.cancel_download_message),
            modifier = Modifier.padding(contentPadding),
        )
    }
}

@Composable
@Preview
private fun CancelDownloadDialogPreview() {
    FindroidTheme { CancelDownloadDialog(onCancel = {}, onDismiss = {}) }
}
