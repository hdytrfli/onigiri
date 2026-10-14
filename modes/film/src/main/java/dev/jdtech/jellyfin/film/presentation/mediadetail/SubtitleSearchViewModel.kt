package dev.jdtech.jellyfin.film.presentation.mediadetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.repository.JellyfinRepository
import dev.jdtech.jellyfin.settings.domain.AppPreferences
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Backs the "search remote subtitles" flow on the detail screens. Gated to admins: the button is
 * only shown when [SubtitleSearchState.canManageSubtitles] is true.
 */
@HiltViewModel
class SubtitleSearchViewModel
@Inject
constructor(
    private val repository: JellyfinRepository,
    private val appPreferences: AppPreferences,
) : ViewModel() {
    private val _state = MutableStateFlow(SubtitleSearchState())
    val state = _state.asStateFlow()

    private var loaded = false

    fun load() {
        if (loaded) return
        loaded = true
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val language =
                appPreferences.getValue(appPreferences.preferredSubtitleLanguage) ?: "eng"
            val canManage =
                try {
                    repository.getUserPolicy()?.isAdministrator == true
                } catch (_: Exception) {
                    false
                }
            _state.update {
                it.copy(isLoading = false, canManageSubtitles = canManage, language = language)
            }
        }
    }

    fun search(itemId: UUID) {
        viewModelScope.launch {
            _state.update { it.copy(isSearching = true, results = emptyList(), error = null) }
            try {
                val results = repository.searchRemoteSubtitles(itemId, _state.value.language)
                _state.update { it.copy(isSearching = false, results = results) }
            } catch (e: Exception) {
                _state.update { it.copy(isSearching = false, error = e) }
            }
        }
    }

    fun download(itemId: UUID, subtitleId: String) {
        viewModelScope.launch {
            _state.update { it.copy(downloadingId = subtitleId, error = null) }
            try {
                repository.downloadRemoteSubtitle(itemId, subtitleId)
                _state.update { it.copy(downloadingId = null, downloadedId = subtitleId) }
            } catch (e: Exception) {
                _state.update { it.copy(downloadingId = null, error = e) }
            }
        }
    }

    fun consumeDownloaded() {
        _state.update { it.copy(downloadedId = null) }
    }
}
