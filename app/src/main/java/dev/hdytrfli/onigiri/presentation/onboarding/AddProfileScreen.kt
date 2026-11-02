package dev.hdytrfli.onigiri.presentation.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.hdytrfli.onigiri.R
import dev.hdytrfli.onigiri.presentation.settings.clearProfileCachesAndRestart
import dev.hdytrfli.onigiri.ui.theme.spacings
import dev.hdytrfli.onigiri.utils.ObserveAsEvents

@Composable
fun AddProfileScreen(
    onBack: () -> Unit,
    viewModel: AddProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    ObserveAsEvents(viewModel.events) { event ->
        if (event is AddProfileEvent.Completed) {
            context.clearProfileCachesAndRestart()
        }
    }

    val back = { if (!viewModel.handleBack()) onBack() }
    BackHandler(onBack = back)

    OnboardingScaffold(stepIndex = null, onBack = back) {
        Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.medium)) {
            when {
                state.quickConnectVisible -> {
                    OnboardingHeader(
                        title = stringResource(R.string.onboarding_quick_connect_title),
                        description = stringResource(R.string.onboarding_quick_connect_description),
                    )
                    QuickConnectContent(
                        code = state.quickConnectCode,
                        error = state.quickConnectError,
                        onCancel = viewModel::cancelQuickConnect,
                    )
                }

                state.step == AddProfileStep.PIN -> {
                    OnboardingHeader(
                        title = stringResource(R.string.onboarding_pin_title),
                        description = stringResource(R.string.onboarding_pin_description),
                    )
                    ProfilePinContent(
                        pin = state.pin,
                        isSavingPin = state.isSavingPin,
                        pinError = state.pinError,
                        onPinChange = viewModel::updatePin,
                        onLock = viewModel::lockProfile,
                        onSkip = viewModel::skipPin,
                    )
                }

                else -> {
                    OnboardingHeader(
                        title = stringResource(R.string.profile_add_title),
                        description = stringResource(R.string.profile_add_description),
                    )
                    AccountForm(
                        username = state.username,
                        password = state.password,
                        isLoggingIn = state.isLoggingIn,
                        loginError = state.loginError,
                        onUsernameChange = viewModel::updateUsername,
                        onPasswordChange = viewModel::updatePassword,
                        onLogin = viewModel::login,
                        onQuickConnect = viewModel::startQuickConnect,
                    )
                }
            }
        }
    }
}
