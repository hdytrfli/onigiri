package dev.hdytrfli.onigiri.feature.film.presentation.mediadetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.hdytrfli.onigiri.models.FindroidMediaStream
import dev.hdytrfli.onigiri.repository.JellyfinRepository
import dev.hdytrfli.onigiri.settings.domain.AppPreferences
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jellyfin.sdk.model.api.MediaStreamType
import org.jellyfin.sdk.model.api.RemoteSubtitleInfo

/**
 * Backs the "remote subtitles" screen on the detail pages. Gated to admins: the entry button is
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

    private var adminLoaded = false
    private var itemLoaded = false

    /** Loads just the admin policy, for gating the entry button. The policy is cached upstream. */
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
            _state.update { it.copy(canManageSubtitles = canManage) }
        }
    }

    /** Loads the search language and the item's existing subtitles for the search screen. */
    fun load(itemId: UUID) {
        loadAdmin()
        if (itemLoaded) return
        itemLoaded = true
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val language =
                appPreferences.getValue(appPreferences.preferredSubtitleLanguage) ?: "eng"
            val recentLanguages =
                appPreferences
                    .getValue(appPreferences.subtitleSearchRecentLanguages)
                    .split(',')
                    .map(String::trim)
                    .filter(String::isNotEmpty)
                    .distinct()
                    .take(5)
            val existing = loadExistingSubtitles(itemId)
            _state.update {
                it.copy(
                    isLoading = false,
                    language = language,
                    recentLanguages = recentLanguages,
                    existing = existing,
                )
            }
        }
    }

    fun setLanguage(language: String) {
        val recent =
            (listOf(language) + _state.value.recentLanguages)
                .filter(String::isNotBlank)
                .distinct()
                .take(5)
        appPreferences.setValue(
            appPreferences.subtitleSearchRecentLanguages,
            recent.joinToString(","),
        )
        _state.update { it.copy(language = language, recentLanguages = recent) }
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
                val existing = loadExistingSubtitles(itemId)
                _state.update {
                    it.copy(
                        downloadingId = null,
                        downloadedIds = it.downloadedIds + subtitleId,
                        existing = existing,
                        results = it.results.filterNot { result -> result.id == subtitleId },
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(downloadingId = null, error = e) }
            }
        }
    }

    fun delete(itemId: UUID, index: Int) {
        viewModelScope.launch {
            _state.update { it.copy(deletingIndex = index, error = null) }
            try {
                repository.deleteSubtitle(itemId, index)
                val existing = loadExistingSubtitles(itemId)
                _state.update { it.copy(deletingIndex = null, existing = existing) }
            } catch (e: Exception) {
                _state.update { it.copy(deletingIndex = null, error = e) }
            }
        }
    }

    fun showInfo(info: RemoteSubtitleInfo) {
        _state.update { it.copy(info = info) }
    }

    fun closeInfo() {
        _state.update { it.copy(info = null) }
    }

    fun refresh(itemId: UUID) {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }
            val existing = loadExistingSubtitles(itemId)
            _state.update { it.copy(isRefreshing = false, existing = existing) }
        }
    }

    private suspend fun loadExistingSubtitles(itemId: UUID): List<FindroidMediaStream> =
        try {
            repository
                .getMediaSources(itemId)
                .flatMap { it.mediaStreams }
                .filter { it.type == MediaStreamType.SUBTITLE && it.index >= 0 }
        } catch (_: Exception) {
            emptyList()
        }
}
