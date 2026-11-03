package dev.hdytrfli.onigiri.feature.film.presentation.downloads

import android.app.DownloadManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.hdytrfli.onigiri.feature.film.presentation.collection.CollectionState
import dev.hdytrfli.onigiri.feature.film.presentation.library.ListDisplayField
import dev.hdytrfli.onigiri.models.FindroidItem
import dev.hdytrfli.onigiri.models.FindroidSourceType
import dev.hdytrfli.onigiri.models.isDownloading
import dev.hdytrfli.onigiri.models.mediaSections
import dev.hdytrfli.onigiri.repository.JellyfinRepository
import dev.hdytrfli.onigiri.settings.domain.AppPreferences
import dev.hdytrfli.onigiri.swr.QueryKeys
import dev.hdytrfli.onigiri.swr.SwrStore
import dev.hdytrfli.onigiri.utils.Downloader
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
                val items =
                    entry.data
                        ?: if (entry.isRefreshing || entry.error != null) {
                            _state.value.items
                        } else {
                            emptyList()
                        }
                _state.value =
                    _state.value.copy(
                        items = items,
                        sections = mediaSections(items),
                        displayFields =
                            ListDisplayField.fromKeys(
                                appPreferences.getValue(appPreferences.listDisplayFields)
                            ),
                        isLoading =
                            entry.isRefreshing || (entry.data == null && entry.error == null),
                        error = entry.error as? Exception,
                    )
                startProgressPolling(items)
            }
        }
    }

    /** Entry load: uses the cached list and refreshes only when it is stale or absent. */
    fun loadItemsIfNeeded() {
        loadItems()
    }

    fun loadItems(force: Boolean = false) {
        swrStore.refresh(key, serializer, force = force) { repository.getDownloads() }
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
                    loadItems(force = true)
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
