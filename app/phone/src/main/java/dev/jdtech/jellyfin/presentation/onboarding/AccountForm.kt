package dev.jdtech.jellyfin.presentation.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import dev.jdtech.jellyfin.R
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.core.presentation.theme.OnScrimSecondary
import dev.jdtech.jellyfin.models.UiText
import dev.jdtech.jellyfin.presentation.components.AppButton
import dev.jdtech.jellyfin.presentation.components.AppButtonSize
import dev.jdtech.jellyfin.presentation.components.AppButtonVariant
import dev.jdtech.jellyfin.presentation.components.AppIconButton
import dev.jdtech.jellyfin.presentation.components.AppTextField
import dev.jdtech.jellyfin.presentation.setup.components.LoadingButton
import dev.jdtech.jellyfin.presentation.theme.spacings

@Composable
fun AccountForm(
    username: String,
    password: String,
    isLoggingIn: Boolean,
    loginError: UiText?,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLogin: () -> Unit,
    onQuickConnect: () -> Unit,
) {
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.medium)) {
        Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small)) {
            AppTextField(
                value = username,
                onValueChange = onUsernameChange,
                label = stringResource(R.string.onboarding_username),
                hint = stringResource(R.string.onboarding_username_hint),
                leadingContent = {
                    Icon(
                        painter = painterResource(CoreR.drawable.ic_user),
                        contentDescription = null,
                    )
                },
                enabled = !isLoggingIn,
                keyboardOptions =
                    KeyboardOptions(
                        autoCorrectEnabled = false,
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next,
                    ),
                modifier = Modifier.fillMaxWidth(),
            )
            AppTextField(
                value = password,
                onValueChange = onPasswordChange,
                label = stringResource(R.string.onboarding_password),
                hint = stringResource(R.string.onboarding_password_hint),
                leadingContent = {
                    Icon(
                        painter = painterResource(CoreR.drawable.ic_lock),
                        contentDescription = null,
                    )
                },
                trailingContent = {
                    AppIconButton(
                        onClick = { passwordVisible = !passwordVisible },
                        size = AppButtonSize.Decorator,
                        variant = AppButtonVariant.Ghost,
                    ) {
                        Icon(
                            painter =
                                painterResource(
                                    if (passwordVisible) CoreR.drawable.ic_eye_off
                                    else CoreR.drawable.ic_eye
                                ),
                            contentDescription = null,
                        )
                    }
                },
                enabled = !isLoggingIn,
                isError = loginError != null,
                supportingText = loginError?.asString(),
                visualTransformation =
                    if (passwordVisible) VisualTransformation.None
                    else PasswordVisualTransformation(),
                keyboardOptions =
                    KeyboardOptions(
                        autoCorrectEnabled = false,
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Go,
                    ),
                keyboardActions = KeyboardActions(onGo = { onLogin() }),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small)) {
            LoadingButton(
                text = stringResource(R.string.onboarding_sign_in),
                onClick = onLogin,
                isLoading = isLoggingIn,
                modifier = Modifier.fillMaxWidth(),
            )
            AppButton(
                onClick = onQuickConnect,
                enabled = !isLoggingIn,
                variant = AppButtonVariant.Secondary,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = stringResource(R.string.onboarding_quick_connect_action))
            }
            Text(
                text = stringResource(R.string.onboarding_quick_connect_hint),
                style = MaterialTheme.typography.bodySmall,
                color = OnScrimSecondary,
            )
        }
    }
}

@Composable
fun QuickConnectContent(
    code: String?,
    error: UiText?,
    onCancel: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.medium)) {
        Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small)) {
            PinCodeField(
                value = code.orEmpty(),
                onValueChange = {},
                length = 6,
                masked = false,
                readOnly = true,
                loading = code == null && error == null,
            )
            Text(
                text =
                    when {
                        error != null -> error.asString()
                        code == null -> stringResource(R.string.onboarding_quick_connect_starting)
                        else -> stringResource(R.string.onboarding_quick_connect_waiting)
                    },
                style = MaterialTheme.typography.bodySmall,
                color = OnScrimSecondary,
            )
        }
        AppButton(
            onClick = onCancel,
            variant = AppButtonVariant.Secondary,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(R.string.onboarding_back))
        }
    }
}
