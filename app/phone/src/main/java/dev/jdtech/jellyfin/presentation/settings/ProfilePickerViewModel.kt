package dev.jdtech.jellyfin.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.R
import dev.jdtech.jellyfin.api.JellyfinApi
import dev.jdtech.jellyfin.models.UiText
import dev.jdtech.jellyfin.settings.domain.AppPreferences
import dev.jdtech.jellyfin.setup.domain.SetupRepository
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileItem(
    val id: UUID,
    val name: String,
    val isLocked: Boolean,
    val isCurrent: Boolean,
    val isAdmin: Boolean,
    val accessToken: String?,
)

data class ProfilePickerState(
    val profiles: List<ProfileItem> = emptyList(),
    val serverId: String? = null,
    val baseUrl: String? = null,
    val viewerIsAdmin: Boolean = false,
    val isLoading: Boolean = true,
    val pinPrompt: ProfileItem? = null,
    val pinError: UiText? = null,
    val forgotPrompt: ProfileItem? = null,
    val forgotError: UiText? = null,
    val removePrompt: ProfileItem? = null,
    val pinEditorPrompt: ProfileItem? = null,
    val pinEditorError: UiText? = null,
    val isBusy: Boolean = false,
)

sealed interface ProfilePickerEvent {
    data object Switched : ProfilePickerEvent
}

@HiltViewModel
class ProfilePickerViewModel
@Inject
constructor(
    private val repository: SetupRepository,
    private val appPreferences: AppPreferences,
    private val jellyfinApi: JellyfinApi,
) : ViewModel() {
    private val _state = MutableStateFlow(ProfilePickerState())
    val state = _state.asStateFlow()

    private val eventsChannel = Channel<ProfilePickerEvent>()
    val events = eventsChannel.receiveAsFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            try {
                val server = repository.getCurrentServer()
                if (server == null) {
                    _state.update { it.copy(isLoading = false) }
                    return@launch
                }
                val users = repository.getUsers(server.id)
                val profiles = users.map { user ->
                    ProfileItem(
                        id = user.id,
                        name = user.name,
                        isLocked = appPreferences.hasProfilePin(server.id, user.id),
                        isCurrent = user.id == server.currentUserId,
                        isAdmin = appPreferences.isProfileAdmin(server.id, user.id),
                        accessToken = user.accessToken,
                    )
                }
                _state.update {
                    it.copy(
                        profiles = profiles,
                        serverId = server.id,
                        baseUrl = jellyfinApi.api.baseUrl?.trimEnd('/'),
                        viewerIsAdmin =
                            profiles.firstOrNull { profile -> profile.isCurrent }?.isAdmin == true,
                        isLoading = false,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onProfileClick(profile: ProfileItem) {
        if (_state.value.isBusy) return
        if (profile.isLocked) {
            _state.update { it.copy(pinPrompt = profile, pinError = null) }
        } else {
            switchTo(profile)
        }
    }

    fun dismissPin() {
        _state.update { it.copy(pinPrompt = null, pinError = null) }
    }

    fun submitPin(pin: String) {
        val profile = _state.value.pinPrompt ?: return
        val serverId = _state.value.serverId ?: return
        if (appPreferences.verifyProfilePin(serverId, profile.id, pin)) {
            _state.update { it.copy(pinPrompt = null, pinError = null) }
            switchTo(profile)
        } else {
            _state.update { it.copy(pinError = UiText.StringResource(R.string.profile_pin_wrong)) }
        }
    }

    fun showForgotPin(profile: ProfileItem) {
        _state.update {
            it.copy(
                pinPrompt = null,
                pinError = null,
                forgotPrompt = profile,
                forgotError = null,
            )
        }
    }

    fun dismissForgotPin() {
        _state.update { it.copy(forgotPrompt = null, forgotError = null) }
    }

    fun submitPassword(password: String) {
        val profile = _state.value.forgotPrompt ?: return
        if (password.isBlank() || _state.value.isBusy) return
        viewModelScope.launch {
            _state.update { it.copy(isBusy = true, forgotError = null) }
            try {
                repository.login(profile.name, password)
                _state.update { it.copy(forgotPrompt = null, isBusy = false) }
                switchTo(profile)
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isBusy = false,
                        forgotError =
                            if (e.message?.contains("401") == true) {
                                UiText.StringResource(R.string.profile_password_wrong)
                            } else {
                                UiText.StringResource(R.string.profile_switch_error)
                            },
                    )
                }
            }
        }
    }

    fun showPinEditor(profile: ProfileItem) {
        _state.update { it.copy(pinEditorPrompt = profile, pinEditorError = null) }
    }

    fun dismissPinEditor() {
        _state.update { it.copy(pinEditorPrompt = null, pinEditorError = null) }
    }

    fun saveProfilePin(pin: String) {
        val profile = _state.value.pinEditorPrompt ?: return
        val serverId = _state.value.serverId ?: return
        if (pin.length != 4) {
            _state.update {
                it.copy(pinEditorError = UiText.StringResource(R.string.onboarding_pin_incomplete))
            }
            return
        }
        if (_state.value.isBusy) return
        viewModelScope.launch {
            _state.update { it.copy(isBusy = true, pinEditorError = null) }
            try {
                appPreferences.setProfilePin(serverId, profile.id, pin)
                _state.update { it.copy(pinEditorPrompt = null) }
                load()
            } finally {
                _state.update { it.copy(isBusy = false) }
            }
        }
    }

    fun unlockProfile(profile: ProfileItem) {
        val serverId = _state.value.serverId ?: return
        appPreferences.clearProfilePin(serverId, profile.id)
        viewModelScope.launch { load() }
    }

    fun showRemoveProfile(profile: ProfileItem) {
        _state.update { it.copy(removePrompt = profile) }
    }

    fun dismissRemoveProfile() {
        _state.update { it.copy(removePrompt = null) }
    }

    fun confirmRemoveProfile() {
        val profile = _state.value.removePrompt ?: return
        val serverId = _state.value.serverId ?: return
        if (_state.value.isBusy) return
        viewModelScope.launch {
            _state.update { it.copy(isBusy = true) }
            try {
                repository.deleteUser(profile.id)
                appPreferences.clearProfileMetadata(serverId, profile.id)
                if (profile.isCurrent) {
                    repository.logout()
                    appPreferences.setValue(appPreferences.profileLoggedOut, true)
                }
                _state.update { it.copy(removePrompt = null) }
                load()
            } finally {
                _state.update { it.copy(isBusy = false) }
            }
        }
    }

    private fun switchTo(profile: ProfileItem) {
        if (_state.value.isBusy) return
        viewModelScope.launch {
            _state.update { it.copy(isBusy = true) }
            try {
                repository.setCurrentUser(profile.id)
                appPreferences.setValue(appPreferences.profileLoggedOut, false)
                appPreferences.clearReaderState()
                eventsChannel.send(ProfilePickerEvent.Switched)
            } finally {
                _state.update { it.copy(isBusy = false) }
            }
        }
    }
}
