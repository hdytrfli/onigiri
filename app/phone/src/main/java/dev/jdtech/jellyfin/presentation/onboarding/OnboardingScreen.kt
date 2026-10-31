package dev.jdtech.jellyfin.presentation.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.jdtech.jellyfin.R
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.core.presentation.theme.OnScrimSecondary
import dev.jdtech.jellyfin.presentation.components.AppButton
import dev.jdtech.jellyfin.presentation.components.AppButtonSize
import dev.jdtech.jellyfin.presentation.components.AppButtonVariant
import dev.jdtech.jellyfin.presentation.components.AppIconButton
import dev.jdtech.jellyfin.presentation.components.AppTextField
import dev.jdtech.jellyfin.presentation.setup.components.LoadingButton
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.utils.ObserveAsEvents

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        if (event is OnboardingEvent.Completed) onComplete()
    }

    BackHandler(enabled = state.step != OnboardingStep.OVERVIEW || state.quickConnectVisible) {
        viewModel.handleBack()
    }

    OnboardingScaffold(stepIndex = state.step.ordinal) {
        AnimatedContent(
            targetState = if (state.quickConnectVisible) "quick-connect" else state.step.name,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "onboarding-page",
        ) { page ->
            when (page) {
                OnboardingStep.OVERVIEW.name ->
                    OverviewPage(onContinue = viewModel::continueFromOverview)
                OnboardingStep.SERVER.name ->
                    ServerPage(
                        state = state,
                        onUrlChange = viewModel::updateServerUrl,
                        onConnect = viewModel::connectServer,
                    )

                OnboardingStep.ACCOUNT.name ->
                    AccountPage(
                        state = state,
                        onUsernameChange = viewModel::updateUsername,
                        onPasswordChange = viewModel::updatePassword,
                        onLogin = viewModel::login,
                        onQuickConnect = viewModel::startQuickConnect,
                    )

                "quick-connect" ->
                    QuickConnectPage(
                        state = state,
                        onCancel = viewModel::cancelQuickConnect,
                    )

                OnboardingStep.PIN.name ->
                    PinPage(
                        state = state,
                        onPinChange = viewModel::updatePin,
                        onLock = viewModel::lockProfile,
                        onSkip = viewModel::skipPin,
                    )
            }
        }
    }
}

@Composable
private fun OverviewPage(onContinue: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.medium)) {
        OnboardingHeader(
            title = stringResource(R.string.onboarding_welcome_title),
            description = stringResource(R.string.onboarding_welcome_description),
        )
        AppButton(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(R.string.onboarding_get_started))
        }
    }
}

@Composable
private fun ServerPage(
    state: OnboardingState,
    onUrlChange: (String) -> Unit,
    onConnect: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.medium)) {
        OnboardingHeader(
            title = stringResource(R.string.onboarding_server_title),
            description = stringResource(R.string.onboarding_server_description),
        )
        Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small)) {
            AppTextField(
                value = state.serverUrl,
                onValueChange = onUrlChange,
                label = stringResource(R.string.onboarding_server_url),
                hint = stringResource(R.string.onboarding_server_url_hint),
                leadingContent = {
                    Icon(
                        painter = painterResource(CoreR.drawable.ic_server),
                        contentDescription = null,
                    )
                },
                keyboardOptions =
                    KeyboardOptions(
                        autoCorrectEnabled = false,
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Go,
                    ),
                keyboardActions = KeyboardActions(onGo = { onConnect() }),
                enabled = !state.isConnecting,
                isError = state.serverError != null,
                supportingText = state.serverError?.asString(),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small)) {
            LoadingButton(
                text = stringResource(R.string.onboarding_connect),
                onClick = onConnect,
                isLoading = state.isConnecting,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = stringResource(R.string.onboarding_server_info),
                style = MaterialTheme.typography.bodySmall,
                color = OnScrimSecondary,
            )
        }
    }
}

@Composable
private fun AccountPage(
    state: OnboardingState,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLogin: () -> Unit,
    onQuickConnect: () -> Unit,
) {
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.medium)) {
        OnboardingHeader(
            title = stringResource(R.string.onboarding_account_title),
            description = stringResource(R.string.onboarding_account_description),
        )
        Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small)) {
            AppTextField(
                value = state.username,
                onValueChange = onUsernameChange,
                label = stringResource(R.string.onboarding_username),
                hint = stringResource(R.string.onboarding_username_hint),
                leadingContent = {
                    Icon(
                        painter = painterResource(CoreR.drawable.ic_user),
                        contentDescription = null,
                    )
                },
                enabled = !state.isLoggingIn,
                keyboardOptions =
                    KeyboardOptions(
                        autoCorrectEnabled = false,
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next,
                    ),
                modifier = Modifier.fillMaxWidth(),
            )
            AppTextField(
                value = state.password,
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
                enabled = !state.isLoggingIn,
                isError = state.loginError != null,
                supportingText = state.loginError?.asString(),
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
                isLoading = state.isLoggingIn,
                modifier = Modifier.fillMaxWidth(),
            )
            if (state.quickConnectEnabled) {
                AppButton(
                    onClick = onQuickConnect,
                    enabled = !state.isLoggingIn,
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
}

@Composable
private fun QuickConnectPage(
    state: OnboardingState,
    onCancel: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.medium)) {
        OnboardingHeader(
            title = stringResource(R.string.onboarding_quick_connect_title),
            description = stringResource(R.string.onboarding_quick_connect_description),
        )
        Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small)) {
            if (state.quickConnectCode == null) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            } else {
                PinCodeField(
                    value = state.quickConnectCode,
                    onValueChange = {},
                    length = 6,
                    masked = false,
                    readOnly = true,
                )
            }
            state.quickConnectError?.let {
                Text(
                    text = it.asString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Text(
                text =
                    if (state.quickConnectCode == null) {
                        stringResource(R.string.onboarding_quick_connect_starting)
                    } else {
                        stringResource(R.string.onboarding_quick_connect_waiting)
                    },
                style = MaterialTheme.typography.bodySmall,
                color = OnScrimSecondary,
            )
        }
        AppButton(
            onClick = onCancel,
            variant = AppButtonVariant.Ghost,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(R.string.onboarding_back))
        }
    }
}

@Composable
private fun PinPage(
    state: OnboardingState,
    onPinChange: (String) -> Unit,
    onLock: () -> Unit,
    onSkip: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.medium)) {
        OnboardingHeader(
            title = stringResource(R.string.onboarding_pin_title),
            description = stringResource(R.string.onboarding_pin_description),
        )
        Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small)) {
            Text(
                text = stringResource(R.string.onboarding_pin_label),
                style = MaterialTheme.typography.labelLarge,
                color = OnScrimSecondary,
            )
            PinCodeField(
                value = state.pin,
                onValueChange = onPinChange,
                length = 4,
                isError = state.pinError != null,
            )
            state.pinError?.let {
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
                isLoading = state.isSavingPin,
                modifier = Modifier.fillMaxWidth(),
            )
            AppButton(
                onClick = onSkip,
                enabled = !state.isSavingPin,
                variant = AppButtonVariant.Ghost,
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

@Composable
private fun OnboardingHeader(title: String, description: String) {
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small)) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            color = OnScrimSecondary,
        )
    }
}

@Composable
private fun OnboardingScaffold(
    stepIndex: Int,
    content: @Composable ColumnScope.() -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val contentMaxHeight = maxHeight * 0.76f

        Image(
            painter = painterResource(CoreR.drawable.onboarding_background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush =
                    Brush.verticalGradient(
                        colorStops =
                            arrayOf(
                                0f to Color.Transparent,
                                0.42f to Color.Black.copy(alpha = 0.15f),
                                1f to Color.Black,
                            )
                    )
            )
        }

        CompositionLocalProvider(LocalContentColor provides Color.White) {
            Column(
                modifier =
                    Modifier.fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .imePadding()
                        .padding(horizontal = MaterialTheme.spacings.default)
            ) {
                OnboardingDots(
                    stepIndex = stepIndex,
                    modifier = Modifier.fillMaxWidth().padding(top = MaterialTheme.spacings.small),
                )
                Spacer(modifier = Modifier.weight(1f))
                Column(
                    modifier =
                        Modifier.fillMaxWidth()
                            .widthIn(max = 420.dp)
                            .align(Alignment.CenterHorizontally)
                            .heightIn(max = contentMaxHeight)
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = MaterialTheme.spacings.medium),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.medium),
                    content = content,
                )
            }
        }
    }
}

@Composable
private fun OnboardingDots(stepIndex: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(4) { index ->
            Box(
                modifier =
                    Modifier.height(6.dp)
                        .width(if (index == stepIndex) 18.dp else 6.dp)
                        .clip(CircleShape)
                        .background(
                            if (index == stepIndex) {
                                Color.White
                            } else {
                                Color.White.copy(alpha = 0.4f)
                            }
                        )
            )
        }
    }
}
