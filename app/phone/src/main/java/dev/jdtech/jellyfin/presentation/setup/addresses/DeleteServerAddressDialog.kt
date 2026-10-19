package dev.jdtech.jellyfin.presentation.setup.addresses

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.jdtech.jellyfin.presentation.components.AppButton
import dev.jdtech.jellyfin.presentation.components.AppButtonVariant
import dev.jdtech.jellyfin.presentation.components.BaseDialog
import dev.jdtech.jellyfin.setup.R as SetupR

@Composable
fun DeleteServerAddressDialog(address: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    BaseDialog(
        title = stringResource(SetupR.string.remove_server_address),
        onDismiss = onDismiss,
        negativeButton = {
            AppButton(onClick = onDismiss, variant = AppButtonVariant.Ghost) {
                Text(text = stringResource(SetupR.string.cancel))
            }
        },
        positiveButton = {
            AppButton(onClick = onConfirm, variant = AppButtonVariant.Destructive) {
                Text(text = stringResource(SetupR.string.confirm))
            }
        },
    ) { contentPadding ->
        Text(
            text = stringResource(SetupR.string.remove_server_address_dialog_text, address),
            modifier = Modifier.padding(contentPadding),
        )
    }
}
