package dev.jdtech.jellyfin.presentation.onboarding

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
import dev.jdtech.jellyfin.R
import dev.jdtech.jellyfin.presentation.settings.clearProfileCachesAndRestart
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.utils.ObserveAsEvents

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

    BackHandler { if (!viewModel.handleBack()) onBack() }

    OnboardingScaffold(stepIndex = null, onBack = onBack) {
        Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.medium)) {
            if (state.quickConnectVisible) {
                OnboardingHeader(
                    title = stringResource(R.string.onboarding_quick_connect_title),
                    description = stringResource(R.string.onboarding_quick_connect_description),
                )
                QuickConnectContent(
                    code = state.quickConnectCode,
                    error = state.quickConnectError,
                    onCancel = viewModel::cancelQuickConnect,
                )
            } else {
                OnboardingHeader(
                    title = stringResource(R.string.profile_add_title),
                    description = stringResource(R.string.profile_add_description),
                )
                AccountForm(
                    username = state.username,
                    password = state.password,
                    isLoggingIn = state.isLoggingIn,
                    loginError = state.loginError,
                    quickConnectEnabled = state.quickConnectEnabled,
                    onUsernameChange = viewModel::updateUsername,
                    onPasswordChange = viewModel::updatePassword,
                    onLogin = viewModel::login,
                    onQuickConnect = viewModel::startQuickConnect,
                )
            }
        }
    }
}
