package dev.jdtech.jellyfin.presentation.setup.login

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.core.presentation.theme.Dimens
import dev.jdtech.jellyfin.presentation.components.AppButton
import dev.jdtech.jellyfin.presentation.components.AppButtonSize
import dev.jdtech.jellyfin.presentation.components.AppButtonVariant
import dev.jdtech.jellyfin.presentation.components.AppIconButton
import dev.jdtech.jellyfin.presentation.components.AppLoadingIndicator
import dev.jdtech.jellyfin.presentation.components.AppTextField
import dev.jdtech.jellyfin.presentation.setup.components.LoadingButton
import dev.jdtech.jellyfin.presentation.setup.components.RootLayout
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.setup.R as SetupR
import dev.jdtech.jellyfin.setup.presentation.login.LoginAction
import dev.jdtech.jellyfin.setup.presentation.login.LoginEvent
import dev.jdtech.jellyfin.setup.presentation.login.LoginState
import dev.jdtech.jellyfin.setup.presentation.login.LoginViewModel
import dev.jdtech.jellyfin.utils.ObserveAsEvents

@Composable
fun LoginScreen(
    onSuccess: () -> Unit,
    onChangeServerClick: () -> Unit,
    onBackClick: () -> Unit,
    prefilledUsername: String? = null,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(true) {
        viewModel.loadServer()
        viewModel.loadDisclaimer()
        viewModel.loadQuickConnectEnabled()
    }

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is LoginEvent.Success -> onSuccess()
        }
    }

    LoginScreenLayout(
        state = state,
        onAction = { action ->
            when (action) {
                is LoginAction.OnChangeServerClick -> onChangeServerClick()
                is LoginAction.OnBackClick -> onBackClick()
                else -> Unit
            }
            viewModel.onAction(action)
        },
        prefilledUsername = prefilledUsername,
    )
}

@Composable
private fun LoginScreenLayout(
    state: LoginState,
    onAction: (LoginAction) -> Unit,
    prefilledUsername: String? = null,
) {
    val scrollState = rememberScrollState()
    var username by rememberSaveable { mutableStateOf(prefilledUsername ?: "") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    val doLogin = { onAction(LoginAction.OnLoginClick(username, password)) }

    RootLayout {
        Column(
            verticalArrangement = Arrangement.Center,
            modifier =
                Modifier.fillMaxHeight()
                    .padding(horizontal = MaterialTheme.spacings.default)
                    .widthIn(max = Dimens.formMaxWidth)
                    .align(Alignment.Center)
                    .verticalScroll(scrollState),
        ) {
            Image(
                painter = painterResource(id = CoreR.drawable.ic_banner),
                contentDescription = null,
                modifier = Modifier.width(Dimens.logoWidth).align(Alignment.CenterHorizontally),
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacings.large))
            Text(
                text = stringResource(SetupR.string.login),
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacings.small))
            Text(
                text = stringResource(SetupR.string.server_subtitle, state.serverName ?: ""),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacings.default))
            AppTextField(
                value = username,
                onValueChange = { username = it },
                label = stringResource(SetupR.string.edit_text_username_hint),
                leadingContent = {
                    Icon(
                        painter = painterResource(CoreR.drawable.ic_user),
                        contentDescription = null,
                    )
                },
                keyboardOptions =
                    KeyboardOptions(autoCorrectEnabled = false, imeAction = ImeAction.Next),
                isError = state.error != null,
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacings.small))
            AppTextField(
                value = password,
                onValueChange = { password = it },
                label = stringResource(SetupR.string.edit_text_password_hint),
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
                        content = {
                            Icon(
                                painter =
                                    if (passwordVisible) painterResource(CoreR.drawable.ic_eye_off)
                                    else painterResource(CoreR.drawable.ic_eye),
                                contentDescription = null,
                            )
                        },
                    )
                },
                keyboardOptions =
                    KeyboardOptions(
                        autoCorrectEnabled = false,
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Go,
                    ),
                keyboardActions = KeyboardActions(onGo = { doLogin() }),
                visualTransformation =
                    if (passwordVisible) VisualTransformation.None
                    else PasswordVisualTransformation(),
                isError = state.error != null,
                enabled = !state.isLoading,
                supportingText = state.error?.asString(),
                modifier = Modifier.fillMaxWidth(),
            )
            LoadingButton(
                text = stringResource(SetupR.string.login_btn_login),
                onClick = { doLogin() },
                isLoading = state.isLoading,
                modifier = Modifier.fillMaxWidth(),
            )
            AnimatedVisibility(state.quickConnectEnabled) {
                Column {
                    Spacer(modifier = Modifier.height(MaterialTheme.spacings.small))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        HorizontalDivider(
                            modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
                        )
                        Text(
                            text = stringResource(SetupR.string.or),
                            color = DividerDefaults.color,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        HorizontalDivider(
                            modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(MaterialTheme.spacings.small))
                    Box {
                        if (state.quickConnectCode != null) {
                            AppLoadingIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                modifier =
                                    Modifier.size(24.dp)
                                        .align(Alignment.CenterStart)
                                        .offset(x = MaterialTheme.spacings.small),
                            )
                        }
                        AppButton(
                            onClick = { onAction(LoginAction.OnQuickConnectClick) },
                            modifier = Modifier.fillMaxWidth(),
                            variant = AppButtonVariant.Secondary,
                        ) {
                            Text(
                                text =
                                    if (state.quickConnectCode != null) state.quickConnectCode!!
                                    else stringResource(SetupR.string.login_btn_quick_connect)
                            )
                        }
                    }
                }
            }
            if (state.disclaimer != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = state.disclaimer!!)
            }
        }
        AppIconButton(
            onClick = { onAction(LoginAction.OnBackClick) },
            modifier = Modifier.padding(start = MaterialTheme.spacings.small),
            variant = AppButtonVariant.Ghost,
            content = {
                Icon(
                    painter = painterResource(CoreR.drawable.ic_arrow_left),
                    contentDescription = null,
                )
            },
        )
        AppIconButton(
            onClick = { onAction(LoginAction.OnChangeServerClick) },
            modifier = Modifier.align(Alignment.TopEnd).padding(end = MaterialTheme.spacings.small),
            variant = AppButtonVariant.Ghost,
            content = {
                Icon(painter = painterResource(CoreR.drawable.ic_server), contentDescription = null)
            },
        )
    }
}

@PreviewScreenSizes
@Composable
private fun AddServerScreenLayoutPreview() {
    FindroidTheme {
        LoginScreenLayout(
            state =
                LoginState(
                    serverName = "Demo Server",
                    quickConnectEnabled = true,
                    disclaimer = "Sample disclaimer",
                ),
            onAction = {},
        )
    }
}
