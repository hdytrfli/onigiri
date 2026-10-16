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
class IdentifyViewModel
@Inject
constructor(
    private val repository: JellyfinRepository,
    private val applyHolder: IdentifyApplyHolder,
) : ViewModel() {
    private val _state = MutableStateFlow(IdentifyState())
    val state = _state.asStateFlow()

    private val eventsChannel = Channel<IdentifyEvent>()
    val events = eventsChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            applyHolder.isApplying.collect { applying ->
                _state.update { it.copy(isApplying = applying) }
            }
        }
    }

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

    fun setImdbId(value: String) {
        _state.update { it.copy(imdbId = value) }
    }

    fun setTmdbId(value: String) {
        _state.update { it.copy(tmdbId = value) }
    }

    fun setTvdbId(value: String) {
        _state.update { it.copy(tvdbId = value) }
    }

    /** Runs a search with explicit criteria (used by the results screen). */
    fun search(
        itemId: UUID,
        query: String,
        year: Int?,
        providerIds: Map<String, String>,
    ) {
        viewModelScope.launch {
            _state.update { it.copy(isSearching = true, results = emptyList(), error = null) }
            try {
                val results =
                    repository.searchRemoteMovies(
                        itemId = itemId,
                        name = query,
                        year = year,
                        providerIds = providerIds,
                    )
                _state.update { it.copy(isSearching = false, results = results) }
            } catch (e: Exception) {
                _state.update { it.copy(isSearching = false, error = e) }
            }
        }
    }

    fun apply(itemId: UUID, result: RemoteSearchResult) {
        // Fire and forget so the caller can return to the media detail page immediately; the
        // loading state is observed via the shared holder.
        applyHolder.apply(itemId, result)
        viewModelScope.launch { eventsChannel.send(IdentifyEvent.Applied) }
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
