package dev.jdtech.jellyfin.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.R
import dev.jdtech.jellyfin.models.UiText
import dev.jdtech.jellyfin.settings.domain.AppPreferences
import dev.jdtech.jellyfin.setup.domain.SetupRepository
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

data class AddProfileState(
    val username: String = "",
    val password: String = "",
    val isLoggingIn: Boolean = false,
    val loginError: UiText? = null,
    val quickConnectEnabled: Boolean = false,
    val quickConnectVisible: Boolean = false,
    val quickConnectCode: String? = null,
    val quickConnectError: UiText? = null,
)

sealed interface AddProfileEvent {
    data object Completed : AddProfileEvent
}

@HiltViewModel
class AddProfileViewModel
@Inject
constructor(
    private val repository: SetupRepository,
    private val appPreferences: AppPreferences,
) : ViewModel() {
    private val _state = MutableStateFlow(AddProfileState())
    val state = _state.asStateFlow()

    private val eventsChannel = Channel<AddProfileEvent>()
    val events = eventsChannel.receiveAsFlow()

    private var quickConnectJob: Job? = null

    init {
        loadQuickConnectEnabled()
    }

    fun updateUsername(value: String) {
        _state.update { it.copy(username = value, loginError = null) }
    }

    fun updatePassword(value: String) {
        _state.update { it.copy(password = value, loginError = null) }
    }

    fun login() {
        val current = _state.value
        if (current.username.isBlank() || current.password.isBlank()) {
            _state.update {
                it.copy(loginError = UiText.StringResource(R.string.onboarding_login_required))
            }
            return
        }
        if (current.isLoggingIn) return

        viewModelScope.launch {
            _state.update { it.copy(isLoggingIn = true, loginError = null) }
            try {
                repository.login(current.username.trim(), current.password)
                appPreferences.clearReaderState()
                _state.update { it.copy(isLoggingIn = false) }
                eventsChannel.send(AddProfileEvent.Completed)
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
                var quickConnectState = repository.initiateQuickConnect()
                _state.update { it.copy(quickConnectCode = quickConnectState.code) }
                while (!quickConnectState.authenticated) {
                    delay(5_000L)
                    quickConnectState = repository.getQuickConnectState(quickConnectState.secret)
                }
                repository.loginWithSecret(quickConnectState.secret)
                appPreferences.clearReaderState()
                _state.update {
                    it.copy(
                        quickConnectVisible = false,
                        quickConnectCode = null,
                        quickConnectError = null,
                    )
                }
                eventsChannel.send(AddProfileEvent.Completed)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
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

    fun handleBack(): Boolean {
        if (_state.value.quickConnectVisible) {
            cancelQuickConnect()
            return true
        }
        return false
    }

    private fun loadQuickConnectEnabled() {
        viewModelScope.launch {
            runCatching { repository.getIsQuickConnectEnabled() }
                .onSuccess { enabled -> _state.update { it.copy(quickConnectEnabled = enabled) } }
        }
    }

    private fun Throwable.toUiText(): UiText =
        message?.let { UiText.DynamicString(it) }
            ?: UiText.StringResource(R.string.onboarding_unknown_error)

    override fun onCleared() {
        quickConnectJob?.cancel()
        super.onCleared()
    }
}
