package dev.jdtech.jellyfin.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.api.JellyfinApi
import dev.jdtech.jellyfin.database.ServerDatabaseDao
import dev.jdtech.jellyfin.settings.domain.AppPreferences
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

sealed interface ServerResetEvent {
    data object Reset : ServerResetEvent
}

@HiltViewModel
class ServerResetViewModel
@Inject
constructor(
    private val database: ServerDatabaseDao,
    private val appPreferences: AppPreferences,
    private val jellyfinApi: JellyfinApi,
) : ViewModel() {
    private val eventsChannel = Channel<ServerResetEvent>()
    val events = eventsChannel.receiveAsFlow()

    fun reset() {
        viewModelScope.launch {
            database.deleteAllUsers()
            database.deleteAllServers()
            appPreferences.setValue(appPreferences.currentServer, null)
            appPreferences.setValue(appPreferences.onboardingComplete, false)
            appPreferences.setValue(appPreferences.profileLoggedOut, false)
            // The data has already been cleared here; do not run the upgrade purge again when the
            // restarted process starts the onboarding flow.
            appPreferences.setValue(appPreferences.onboardingSessionCleared, true)
            appPreferences.clearReaderState()
            appPreferences.clearAllProfileMetadata()
            jellyfinApi.api.update(accessToken = null)
            jellyfinApi.userId = null
            eventsChannel.send(ServerResetEvent.Reset)
        }
    }
}
