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
    val accessToken: String?,
)

data class ProfilePickerState(
    val profiles: List<ProfileItem> = emptyList(),
    val serverId: String? = null,
    val baseUrl: String? = null,
    val isLoading: Boolean = true,
    val pinPrompt: ProfileItem? = null,
    val pinError: UiText? = null,
    val forgotPrompt: ProfileItem? = null,
    val forgotError: UiText? = null,
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
            val server = repository.getCurrentServer()
            if (server == null) {
                _state.update { it.copy(isLoading = false) }
                return@launch
            }
            val users = repository.getUsers(server.id)
            _state.update {
                it.copy(
                    profiles =
                        users.map { user ->
                            ProfileItem(
                                id = user.id,
                                name = user.name,
                                isLocked = appPreferences.hasProfilePin(server.id, user.id),
                                isCurrent = user.id == server.currentUserId,
                                accessToken = user.accessToken,
                            )
                        },
                    serverId = server.id,
                    baseUrl = jellyfinApi.api.baseUrl?.trimEnd('/'),
                    isLoading = false,
                )
            }
        }
    }

    fun onProfileClick(profile: ProfileItem) {
        if (profile.isCurrent || _state.value.isBusy) return
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
                val server = repository.getCurrentServer()
                repository.login(profile.name, password)
                if (server != null) {
                    appPreferences.clearProfilePin(server.id, profile.id)
                }
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

    private fun switchTo(profile: ProfileItem) {
        if (_state.value.isBusy) return
        viewModelScope.launch {
            _state.update { it.copy(isBusy = true) }
            repository.setCurrentUser(profile.id)
            appPreferences.clearReaderState()
            _state.update { it.copy(isBusy = false) }
            eventsChannel.send(ProfilePickerEvent.Switched)
        }
    }
}
