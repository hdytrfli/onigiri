package dev.jdtech.jellyfin.film.presentation.downloads

import android.app.DownloadManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.film.presentation.collection.CollectionState
import dev.jdtech.jellyfin.film.presentation.library.ListDisplayField
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.models.FindroidSourceType
import dev.jdtech.jellyfin.models.isDownloading
import dev.jdtech.jellyfin.models.mediaSections
import dev.jdtech.jellyfin.repository.JellyfinRepository
import dev.jdtech.jellyfin.settings.domain.AppPreferences
import dev.jdtech.jellyfin.swr.QueryKeys
import dev.jdtech.jellyfin.swr.SwrStore
import dev.jdtech.jellyfin.utils.Downloader
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer

@HiltViewModel
class DownloadsViewModel
@Inject
constructor(
    private val repository: JellyfinRepository,
    private val downloader: Downloader,
    private val appPreferences: AppPreferences,
    private val swrStore: SwrStore,
) : ViewModel() {
    private val _state = MutableStateFlow(CollectionState())
    val state = _state.asStateFlow()

    private var progressJob: Job? = null
    private val key = QueryKeys.Downloads
    private val serializer = ListSerializer(FindroidItem.serializer())

    init {
        viewModelScope.launch {
            swrStore.observe(key, serializer).collect { entry ->
                val items = entry.data.orEmpty()
                _state.value =
                    _state.value.copy(
                        items = items,
                        sections = mediaSections(items),
                        displayFields =
                            ListDisplayField.fromKeys(
                                appPreferences.getValue(appPreferences.listDisplayFields)
                            ),
                        isLoading = entry.isRefreshing,
                        error = entry.error as? Exception,
                    )
                startProgressPolling(items)
            }
        }
    }

    /** Entry load: uses the cached list and refreshes only when it is stale or absent. */
    fun loadItemsIfNeeded() {
        swrStore.refresh(key, serializer) { repository.getDownloads() }
    }

    fun loadItems() {
        swrStore.refresh(key, serializer, force = true) { repository.getDownloads() }
    }

    /** Flips [field] in the shared list display preference every list page reads. */
    fun toggleDisplayField(field: ListDisplayField) {
        val fields = _state.value.displayFields.toMutableSet()
        if (!fields.add(field)) {
            fields.remove(field)
        }
        appPreferences.setValue(appPreferences.listDisplayFields, fields.map { it.key }.toSet())
        _state.value = _state.value.copy(displayFields = fields)
    }

    /** Polls the progress of in-progress downloads so the grid can overlay the percentage. */
    private fun startProgressPolling(items: List<FindroidItem>) {
        val downloading = items.filter { it.isDownloading() }
        progressJob?.cancel()
        if (downloading.isEmpty()) {
            _state.value = _state.value.copy(downloadProgress = emptyMap())
            return
        }
        progressJob = viewModelScope.launch {
            while (true) {
                val progress = mutableMapOf<UUID, Int>()
                var finished = false
                for (item in downloading) {
                    val source =
                        item.sources.firstOrNull { it.type == FindroidSourceType.LOCAL } ?: continue
                    val (status, value) = downloader.getProgress(source.downloadId)
                    if (status == DownloadManager.STATUS_SUCCESSFUL) {
                        finished = true
                        break
                    }
                    progress[item.id] = value.coerceIn(0, 100)
                }
                if (finished) {
                    loadItems()
                    return@launch
                }
                _state.value = _state.value.copy(downloadProgress = progress)
                delay(1000L)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        progressJob?.cancel()
    }
}
