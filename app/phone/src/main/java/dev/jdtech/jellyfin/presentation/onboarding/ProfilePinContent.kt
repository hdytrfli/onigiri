package dev.jdtech.jellyfin.presentation.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.jdtech.jellyfin.R
import dev.jdtech.jellyfin.core.presentation.theme.OnScrimSecondary
import dev.jdtech.jellyfin.models.UiText
import dev.jdtech.jellyfin.presentation.components.AppButton
import dev.jdtech.jellyfin.presentation.components.AppButtonVariant
import dev.jdtech.jellyfin.presentation.setup.components.LoadingButton
import dev.jdtech.jellyfin.presentation.theme.spacings

@Composable
fun ProfilePinContent(
    pin: String,
    isSavingPin: Boolean,
    pinError: UiText?,
    onPinChange: (String) -> Unit,
    onLock: () -> Unit,
    onSkip: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.medium)) {
        Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small)) {
            Text(
                text = stringResource(R.string.onboarding_pin_label),
                style = MaterialTheme.typography.labelLarge,
                color = OnScrimSecondary,
            )
            PinCodeField(
                value = pin,
                onValueChange = onPinChange,
                length = 4,
                isError = pinError != null,
            )
            pinError?.let {
                Text(
                    text = it.asString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small)) {
            LoadingButton(
                text = stringResource(R.string.onboarding_pin_lock),
                onClick = onLock,
                isLoading = isSavingPin,
                modifier = Modifier.fillMaxWidth(),
            )
            AppButton(
                onClick = onSkip,
                enabled = !isSavingPin,
                variant = AppButtonVariant.Secondary,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = stringResource(R.string.onboarding_pin_skip))
            }
            Text(
                text = stringResource(R.string.onboarding_pin_info),
                style = MaterialTheme.typography.bodySmall,
                color = OnScrimSecondary,
            )
        }
    }
}
