package dev.jdtech.jellyfin.film.presentation.mediadetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.repository.JellyfinRepository
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jellyfin.sdk.model.api.RemoteSearchResult

/**
 * Backs the "Identify" screen: search remote metadata providers for a movie and apply the chosen
 * result to replace its metadata.
 */
@HiltViewModel
class IdentifyViewModel @Inject constructor(private val repository: JellyfinRepository) :
    ViewModel() {
    private val _state = MutableStateFlow(IdentifyState())
    val state = _state.asStateFlow()

    private val eventsChannel = Channel<IdentifyEvent>()
    val events = eventsChannel.receiveAsFlow()

    private var adminLoaded = false

    /** Loads whether the user may identify items (admins only); the policy is cached upstream. */
    fun loadAdmin() {
        if (adminLoaded) return
        adminLoaded = true
        viewModelScope.launch {
            val canManage =
                try {
                    repository.getUserPolicy()?.isAdministrator == true
                } catch (_: Exception) {
                    false
                }
            _state.update { it.copy(canManage = canManage) }
        }
    }

    fun setQuery(query: String) {
        _state.update { it.copy(query = query) }
    }

    fun setYear(year: String) {
        _state.update { it.copy(year = year) }
    }

    fun search(itemId: UUID) {
        val query = _state.value.query.trim()
        if (query.isEmpty()) return
        viewModelScope.launch {
            _state.update { it.copy(isSearching = true, results = emptyList(), error = null) }
            try {
                val results =
                    repository.searchRemoteMovies(
                        itemId = itemId,
                        name = query,
                        year = _state.value.year.trim().toIntOrNull(),
                    )
                _state.update { it.copy(isSearching = false, results = results) }
            } catch (e: Exception) {
                _state.update { it.copy(isSearching = false, error = e) }
            }
        }
    }

    fun apply(itemId: UUID, result: RemoteSearchResult) {
        viewModelScope.launch {
            _state.update { it.copy(isApplying = true, error = null) }
            try {
                repository.applyRemoteSearchResult(itemId = itemId, result = result)
                _state.update { it.copy(isApplying = false) }
                eventsChannel.send(IdentifyEvent.Applied)
            } catch (e: Exception) {
                _state.update { it.copy(isApplying = false, error = e) }
            }
        }
    }

    fun showInfo(result: RemoteSearchResult) {
        _state.update { it.copy(info = result) }
    }

    fun closeInfo() {
        _state.update { it.copy(info = null) }
    }
}

sealed interface IdentifyEvent {
    data object Applied : IdentifyEvent
}
