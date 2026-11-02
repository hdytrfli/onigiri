package dev.hdytrfli.onigiri.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.hdytrfli.onigiri.R
import dev.hdytrfli.onigiri.models.ExceptionUiText
import dev.hdytrfli.onigiri.models.ExceptionUiTexts
import dev.hdytrfli.onigiri.models.UiText
import dev.hdytrfli.onigiri.settings.domain.AppPreferences
import dev.hdytrfli.onigiri.setup.domain.SetupRepository
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class OnboardingStep {
    OVERVIEW,
    SERVER,
    ACCOUNT,
    PIN,
}

data class OnboardingState(
    val step: OnboardingStep = OnboardingStep.OVERVIEW,
    val serverUrl: String = "",
    val serverName: String? = null,
    val username: String = "",
    val password: String = "",
    val pin: String = "",
    val isConnecting: Boolean = false,
    val isLoggingIn: Boolean = false,
    val isSavingPin: Boolean = false,
    val quickConnectVisible: Boolean = false,
    val quickConnectCode: String? = null,
    val serverError: UiText? = null,
    val loginError: UiText? = null,
    val quickConnectError: UiText? = null,
    val pinError: UiText? = null,
)

sealed interface OnboardingEvent {
    data object Completed : OnboardingEvent
}

@HiltViewModel
class OnboardingViewModel
@Inject
constructor(
    private val repository: SetupRepository,
    private val appPreferences: AppPreferences,
) : ViewModel() {
    private val _state = MutableStateFlow(OnboardingState())
    val state = _state.asStateFlow()

    private val eventsChannel = Channel<OnboardingEvent>()
    val events = eventsChannel.receiveAsFlow()

    private var quickConnectJob: Job? = null

    fun continueFromOverview() {
        _state.update { it.copy(step = OnboardingStep.SERVER) }
    }

    fun updateServerUrl(value: String) {
        _state.update { it.copy(serverUrl = value, serverError = null) }
    }

    fun connectServer() {
        val url = _state.value.serverUrl.trim()
        if (url.isBlank()) {
            _state.update {
                it.copy(serverError = UiText.StringResource(R.string.onboarding_server_required))
            }
            return
        }
        if (_state.value.isConnecting) return

        viewModelScope.launch {
            _state.update { it.copy(isConnecting = true, serverError = null) }
            try {
                val server = repository.addServer(url)
                appPreferences.setValue(appPreferences.currentServer, server.id)
                _state.update {
                    it.copy(
                        step = OnboardingStep.ACCOUNT,
                        serverName = server.name,
                        isConnecting = false,
                        serverError = null,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: ExceptionUiText) {
                _state.update { it.copy(isConnecting = false, serverError = e.uiText) }
            } catch (e: ExceptionUiTexts) {
                _state.update {
                    it.copy(
                        isConnecting = false,
                        serverError = e.uiTexts.firstOrNull(),
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isConnecting = false,
                        serverError = e.toUiText(),
                    )
                }
            }
        }
    }

    fun updateUsername(value: String) {
        _state.update { it.copy(username = value, loginError = null) }
    }

    fun updatePassword(value: String) {
        _state.update { it.copy(password = value, loginError = null) }
    }

    fun login() {
        val state = _state.value
        if (state.username.isBlank() || state.password.isBlank()) {
            _state.update {
                it.copy(loginError = UiText.StringResource(R.string.onboarding_login_required))
            }
            return
        }
        if (state.isLoggingIn) return

        viewModelScope.launch {
            _state.update { it.copy(isLoggingIn = true, loginError = null) }
            try {
                repository.login(state.username.trim(), state.password)
                _state.update {
                    it.copy(step = OnboardingStep.PIN, isLoggingIn = false, loginError = null)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoggingIn = false,
                        loginError =
                            if (e.message?.contains("401") == true) {
                                UiText.StringResource(R.string.onboarding_login_error)
                            } else {
                                e.toUiText()
                            },
                    )
                }
            }
        }
    }

    fun startQuickConnect() {
        quickConnectJob?.cancel()
        _state.update {
            it.copy(
                quickConnectVisible = true,
                quickConnectCode = null,
                quickConnectError = null,
            )
        }
        quickConnectJob = viewModelScope.launch {
            try {
                if (!repository.getIsQuickConnectEnabled()) {
                    _state.update {
                        it.copy(
                            quickConnectError =
                                UiText.StringResource(R.string.onboarding_quick_connect_unavailable)
                        )
                    }
                    return@launch
                }
                var quickConnectState = repository.initiateQuickConnect()
                _state.update { it.copy(quickConnectCode = quickConnectState.code) }

                while (!quickConnectState.authenticated) {
                    delay(5_000L)
                    quickConnectState = repository.getQuickConnectState(quickConnectState.secret)
                }

                repository.loginWithSecret(quickConnectState.secret)
                _state.update {
                    it.copy(
                        step = OnboardingStep.PIN,
                        quickConnectVisible = false,
                        quickConnectCode = null,
                        quickConnectError = null,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        quickConnectCode = null,
                        quickConnectError =
                            UiText.StringResource(R.string.onboarding_quick_connect_error),
                    )
                }
            }
        }
    }

    fun cancelQuickConnect() {
        quickConnectJob?.cancel()
        quickConnectJob = null
        _state.update {
            it.copy(
                quickConnectVisible = false,
                quickConnectCode = null,
                quickConnectError = null,
            )
        }
    }

    fun updatePin(value: String) {
        val digits = value.filter(Char::isDigit).take(4)
        _state.update { it.copy(pin = digits, pinError = null) }
    }

    fun lockProfile() {
        val state = _state.value
        if (state.pin.length != 4) {
            _state.update {
                it.copy(pinError = UiText.StringResource(R.string.onboarding_pin_incomplete))
            }
            return
        }
        if (state.isSavingPin) return

        viewModelScope.launch {
            _state.update { it.copy(isSavingPin = true, pinError = null) }
            val server = repository.getCurrentServer()
            val user = repository.getCurrentUser()
            if (server == null || user == null) {
                _state.update {
                    it.copy(
                        isSavingPin = false,
                        pinError = UiText.StringResource(R.string.onboarding_pin_save_error),
                    )
                }
                return@launch
            }

            appPreferences.setProfilePin(server.id, user.id, state.pin)
            completeOnboarding()
        }
    }

    fun skipPin() {
        completeOnboarding()
    }

    /** Returns true when the back gesture was handled by an earlier onboarding step. */
    fun handleBack(): Boolean {
        if (_state.value.quickConnectVisible) {
            cancelQuickConnect()
            return true
        }
        return when (_state.value.step) {
            OnboardingStep.OVERVIEW -> false
            OnboardingStep.SERVER -> {
                _state.update { it.copy(step = OnboardingStep.OVERVIEW) }
                true
            }

            OnboardingStep.ACCOUNT -> {
                _state.update { it.copy(step = OnboardingStep.SERVER) }
                true
            }

            OnboardingStep.PIN -> {
                _state.update { it.copy(step = OnboardingStep.ACCOUNT) }
                true
            }
        }
    }

    private fun completeOnboarding() {
        appPreferences.setValue(appPreferences.onboardingComplete, true)
        appPreferences.setValue(appPreferences.profileLoggedOut, false)
        viewModelScope.launch { eventsChannel.send(OnboardingEvent.Completed) }
    }

    private fun Throwable.toUiText(): UiText =
        message?.let { UiText.DynamicString(it) }
            ?: UiText.StringResource(R.string.onboarding_unknown_error)

    override fun onCleared() {
        quickConnectJob?.cancel()
        super.onCleared()
    }
}
