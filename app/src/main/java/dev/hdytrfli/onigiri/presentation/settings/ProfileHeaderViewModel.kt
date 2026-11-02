package dev.hdytrfli.onigiri.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.hdytrfli.onigiri.api.JellyfinApi
import dev.hdytrfli.onigiri.settings.domain.AppPreferences
import dev.hdytrfli.onigiri.setup.domain.SetupRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileHeaderState(
    val userName: String? = null,
    val serverName: String? = null,
    val avatarUrl: String? = null,
    val accessToken: String? = null,
    val isAdmin: Boolean = false,
    val loggedOut: Boolean = false,
)

@HiltViewModel
class ProfileHeaderViewModel
@Inject
constructor(
    private val repository: SetupRepository,
    private val jellyfinApi: JellyfinApi,
    private val appPreferences: AppPreferences,
) : ViewModel() {
    private val _state = MutableStateFlow(ProfileHeaderState())
    val state = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val server = repository.getCurrentServer()
            val user = repository.getCurrentUser()
            val baseUrl = jellyfinApi.api.baseUrl?.trimEnd('/')
            _state.emit(
                ProfileHeaderState(
                    userName = user?.name,
                    serverName = server?.name,
                    avatarUrl =
                        if (user != null && baseUrl != null) {
                            "$baseUrl/Users/${user.id}/Images/Primary?maxWidth=200"
                        } else {
                            null
                        },
                    accessToken = user?.accessToken,
                    isAdmin =
                        user != null &&
                            server != null &&
                            appPreferences.isProfileAdmin(server.id, user.id),
                )
            )
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            appPreferences.setValue(appPreferences.profileLoggedOut, true)
            _state.update { it.copy(loggedOut = true) }
        }
    }
}
