package dev.jdtech.jellyfin.presentation.film.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.presentation.components.AppButton
import dev.jdtech.jellyfin.presentation.components.AppButtonVariant
import dev.jdtech.jellyfin.presentation.components.BaseDialog
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme

@Composable
fun DeleteDownloadDialog(onDelete: () -> Unit, onDismiss: () -> Unit) {
    BaseDialog(
        title = stringResource(CoreR.string.delete_download),
        onDismiss = onDismiss,
        negativeButton = {
            AppButton(onClick = onDismiss, variant = AppButtonVariant.Secondary) {
                Text(text = stringResource(CoreR.string.cancel))
            }
        },
        positiveButton = {
            AppButton(onClick = onDelete, variant = AppButtonVariant.Destructive) {
                Text(text = stringResource(CoreR.string.delete_download))
            }
        },
    ) { contentPadding ->
        Text(
            text = stringResource(CoreR.string.delete_download_message),
            modifier = Modifier.padding(contentPadding),
        )
    }
}

@Composable
@Preview
private fun CancelDownloadDialogPreview() {
    FindroidTheme { DeleteDownloadDialog(onDelete = {}, onDismiss = {}) }
}
