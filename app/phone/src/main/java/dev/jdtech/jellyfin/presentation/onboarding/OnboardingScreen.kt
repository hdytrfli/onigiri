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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.jdtech.jellyfin.R
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.core.presentation.theme.OnScrimSecondary
import dev.jdtech.jellyfin.presentation.components.AppButton
import dev.jdtech.jellyfin.presentation.components.AppTextField
import dev.jdtech.jellyfin.presentation.film.components.ItemTopBar
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

    val showBack = state.step != OnboardingStep.OVERVIEW || state.quickConnectVisible
    OnboardingScaffold(
        stepIndex = state.step.ordinal,
        onBack = if (showBack) ({ viewModel.handleBack() }) else null,
    ) {
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
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.medium)) {
        OnboardingHeader(
            title = stringResource(R.string.onboarding_account_title),
            description = stringResource(R.string.onboarding_account_description),
        )
        AccountForm(
            username = state.username,
            password = state.password,
            isLoggingIn = state.isLoggingIn,
            loginError = state.loginError,
            onUsernameChange = onUsernameChange,
            onPasswordChange = onPasswordChange,
            onLogin = onLogin,
            onQuickConnect = onQuickConnect,
        )
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
        QuickConnectContent(
            code = state.quickConnectCode,
            error = state.quickConnectError,
            onCancel = onCancel,
        )
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
        ProfilePinContent(
            pin = state.pin,
            isSavingPin = state.isSavingPin,
            pinError = state.pinError,
            onPinChange = onPinChange,
            onLock = onLock,
            onSkip = onSkip,
        )
    }
}

@Composable
fun OnboardingHeader(title: String, description: String) {
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small)) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            color = Color.White,
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = OnScrimSecondary,
        )
    }
}

@Composable
fun OnboardingScaffold(
    stepIndex: Int?,
    onBack: (() -> Unit)? = null,
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
                brush = Brush.verticalGradient(colors = listOf(Color(0x33000000), Color.Black))
            )
        }

        CompositionLocalProvider(LocalContentColor provides Color.White) {
            Column(modifier = Modifier.fillMaxSize()) {
                ItemTopBar(
                    hasBackButton = onBack != null,
                    onBackClick = { onBack?.invoke() },
                )
                Column(
                    modifier =
                        Modifier.weight(1f)
                            .fillMaxWidth()
                            .imePadding()
                            .navigationBarsPadding()
                            .padding(horizontal = MaterialTheme.spacings.default)
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    Column(
                        modifier =
                            Modifier.widthIn(max = 420.dp)
                                .fillMaxWidth()
                                .align(Alignment.CenterHorizontally)
                                .heightIn(max = contentMaxHeight)
                                .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.medium),
                        content = content,
                    )
                    if (stepIndex != null) {
                        OnboardingDots(
                            stepIndex = stepIndex,
                            modifier =
                                Modifier.fillMaxWidth()
                                    .padding(
                                        top = MaterialTheme.spacings.medium,
                                        bottom = MaterialTheme.spacings.small,
                                    ),
                        )
                    }
                }
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
