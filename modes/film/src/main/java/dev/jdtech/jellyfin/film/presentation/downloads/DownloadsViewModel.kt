package dev.jdtech.jellyfin.film.presentation.downloads

import android.app.DownloadManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.core.Constants
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.film.presentation.collection.CollectionState
import dev.jdtech.jellyfin.models.CollectionSection
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.models.FindroidSourceType
import dev.jdtech.jellyfin.models.UiText
import dev.jdtech.jellyfin.models.isDownloading
import dev.jdtech.jellyfin.repository.JellyfinRepository
import dev.jdtech.jellyfin.utils.Downloader
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class DownloadsViewModel
@Inject
constructor(
    private val repository: JellyfinRepository,
    private val downloader: Downloader,
) : ViewModel() {
    private val _state = MutableStateFlow(CollectionState())
    val state = _state.asStateFlow()

    private var progressJob: Job? = null

    fun loadItems() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            try {
                val items = repository.getDownloads()

                // No Movies/Shows grouping: the downloads page shows one flat list.
                val sections =
                    if (items.isEmpty()) {
                        emptyList()
                    } else {
                        listOf(
                            CollectionSection(
                                Constants.FAVORITE_TYPE_MOVIES,
                                UiText.StringResource(CoreR.string.movies_label),
                                items,
                            )
                        )
                    }

                _state.value = _state.value.copy(isLoading = false, sections = sections)
                startProgressPolling(items)
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = e)
            }
        }
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
