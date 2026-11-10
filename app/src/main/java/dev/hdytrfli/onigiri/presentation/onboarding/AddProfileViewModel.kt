package dev.hdytrfli.onigiri.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.hdytrfli.onigiri.R
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

enum class AddProfileStep {
    ACCOUNT,
    PIN,
    LANGUAGES,
}

data class AddProfileState(
    val step: AddProfileStep = AddProfileStep.ACCOUNT,
    val username: String = "",
    val password: String = "",
    val pin: String = "",
    val isLoggingIn: Boolean = false,
    val isSavingPin: Boolean = false,
    val loginError: UiText? = null,
    val pinError: UiText? = null,
    val quickConnectVisible: Boolean = false,
    val quickConnectCode: String? = null,
    val quickConnectError: UiText? = null,
    val audioLanguage: String? = null,
    val subtitleLanguage: String? = null,
    val alwaysPlaySubtitles: Boolean = false,
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
        _state.update {
            it.copy(
                audioLanguage = appPreferences.defaultLanguage,
                subtitleLanguage = appPreferences.defaultLanguage,
            )
        }
    }

    fun updateUsername(value: String) {
        _state.update { it.copy(username = value, loginError = null) }
    }

    fun updatePassword(value: String) {
        _state.update { it.copy(password = value, loginError = null) }
    }

    fun login() {
        val current = _state.value
        if (current.username.isBlank()) {
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
                _state.update {
                    it.copy(
                        step = AddProfileStep.PIN,
                        isLoggingIn = false,
                        audioLanguage =
                            appPreferences.getValue(appPreferences.preferredAudioLanguage),
                        subtitleLanguage =
                            appPreferences.getValue(appPreferences.preferredSubtitleLanguage),
                        alwaysPlaySubtitles =
                            appPreferences.getValue(appPreferences.playerAlwaysPlaySubtitles),
                    )
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
                appPreferences.clearReaderState()
                _state.update {
                    it.copy(
                        step = AddProfileStep.PIN,
                        quickConnectVisible = false,
                        quickConnectCode = null,
                        quickConnectError = null,
                        audioLanguage =
                            appPreferences.getValue(appPreferences.preferredAudioLanguage),
                        subtitleLanguage =
                            appPreferences.getValue(appPreferences.preferredSubtitleLanguage),
                        alwaysPlaySubtitles =
                            appPreferences.getValue(appPreferences.playerAlwaysPlaySubtitles),
                    )
                }
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

    fun updatePin(value: String) {
        _state.update { it.copy(pin = value.filter(Char::isDigit).take(4), pinError = null) }
    }

    fun lockProfile() {
        val current = _state.value
        if (current.pin.length != 4) {
            _state.update {
                it.copy(pinError = UiText.StringResource(R.string.onboarding_pin_incomplete))
            }
            return
        }
        if (current.isSavingPin) return

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
            appPreferences.setProfilePin(server.id, user.id, current.pin)
            _state.update { it.copy(isSavingPin = false, step = AddProfileStep.LANGUAGES) }
        }
    }

    fun skipPin() {
        _state.update { it.copy(step = AddProfileStep.LANGUAGES) }
    }

    fun updateAudioLanguage(language: String?) {
        _state.update { it.copy(audioLanguage = language) }
    }

    fun updateSubtitleLanguage(language: String?) {
        _state.update { it.copy(subtitleLanguage = language) }
    }

    fun updateAlwaysPlaySubtitles(enabled: Boolean) {
        _state.update { it.copy(alwaysPlaySubtitles = enabled) }
    }

    fun finishLanguages() {
        val state = _state.value
        appPreferences.setValue(appPreferences.preferredAudioLanguage, state.audioLanguage)
        appPreferences.setValue(appPreferences.preferredSubtitleLanguage, state.subtitleLanguage)
        appPreferences.setValue(
            appPreferences.playerAlwaysPlaySubtitles,
            state.alwaysPlaySubtitles,
        )
        complete()
    }

    fun handleBack(): Boolean {
        if (_state.value.quickConnectVisible) {
            cancelQuickConnect()
            return true
        }
        if (_state.value.step == AddProfileStep.LANGUAGES) {
            _state.update { it.copy(step = AddProfileStep.PIN) }
            return true
        }
        // Login has already switched the active user; don't let the user step back into the
        // account form and leave it in a half-configured state.
        if (_state.value.step == AddProfileStep.PIN) return true
        return false
    }

    private fun complete() {
        appPreferences.setValue(appPreferences.profileLoggedOut, false)
        appPreferences.clearReaderState()
        _state.update { it.copy(isSavingPin = false) }
        viewModelScope.launch { eventsChannel.send(AddProfileEvent.Completed) }
    }

    private fun Throwable.toUiText(): UiText =
        message?.let { UiText.DynamicString(it) }
            ?: UiText.StringResource(R.string.onboarding_unknown_error)

    override fun onCleared() {
        quickConnectJob?.cancel()
        super.onCleared()
    }
}
