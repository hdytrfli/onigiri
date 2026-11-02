package dev.hdytrfli.onigiri.feature.film.presentation.downloader

import android.app.DownloadManager
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.hdytrfli.onigiri.models.FindroidBook
import dev.hdytrfli.onigiri.models.FindroidItem
import dev.hdytrfli.onigiri.models.FindroidSourceType
import dev.hdytrfli.onigiri.models.isDownloading
import dev.hdytrfli.onigiri.utils.Downloader
import javax.inject.Inject
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

@HiltViewModel
class DownloaderViewModel @Inject constructor(private val downloader: Downloader) : ViewModel() {
    private val _state = MutableStateFlow(DownloaderState())
    val state = _state.asStateFlow()

    private val eventsChannel = Channel<DownloaderEvent>()
    val events = eventsChannel.receiveAsFlow()

    var downloadId: Long? = null

    private val handler = Handler(Looper.getMainLooper())

    fun update(item: FindroidItem) {
        viewModelScope.launch {
            if (item.isDownloading()) {
                val source =
                    item.sources.firstOrNull { it.type == FindroidSourceType.LOCAL }
                        ?: return@launch
                this@DownloaderViewModel.downloadId = source.downloadId
                pollDownloadProgress(source.downloadId)
            }
        }
    }

    private fun download(item: FindroidItem, storageIndex: Int = 0) {
        viewModelScope.launch {
            _state.emit(DownloaderState(status = DownloadManager.STATUS_PENDING))
            val source = item.sources.firstOrNull()
            if (source == null && item !is FindroidBook) {
                _state.emit(DownloaderState(status = DownloadManager.STATUS_FAILED))
                return@launch
            }
            val (downloadId, uiText) =
                downloader.downloadItem(
                    item = item,
                    sourceId = source?.id.orEmpty(),
                    storageIndex = storageIndex,
                )
            if (downloadId == 0L) {
                _state.emit(DownloaderState(status = DownloadManager.STATUS_SUCCESSFUL))
                eventsChannel.trySend(DownloaderEvent.Successful)
            } else if (downloadId != -1L) {
                this@DownloaderViewModel.downloadId = downloadId
                pollDownloadProgress(downloadId)
            } else {
                _state.emit(
                    DownloaderState(status = DownloadManager.STATUS_FAILED, errorText = uiText)
                )
            }
        }
    }

    private fun cancelDownload(item: FindroidItem) {
        viewModelScope.launch {
            // Stop progress polling
            handler.removeCallbacksAndMessages(null)

            // Cancel the download
            downloadId?.let { downloader.cancelDownload(item = item, downloadId = it) }

            // Emit empty DownloadState
            _state.emit(DownloaderState())
        }
    }

    private fun deleteDownload(item: FindroidItem) {
        viewModelScope.launch {
            val source =
                item.sources.firstOrNull { it.type == FindroidSourceType.LOCAL } ?: return@launch
            downloader.deleteItem(item = item, source = source)
            eventsChannel.send(DownloaderEvent.Deleted)
        }
    }

    private fun pollDownloadProgress(downloadId: Long?) {
        handler.removeCallbacksAndMessages(null)
        lateinit var downloadProgressRunnable: Runnable
        downloadProgressRunnable = Runnable {
            viewModelScope.launch {
                val (status, progress) = downloader.getProgress(downloadId)
                val state =
                    DownloaderState(
                        status = status,
                        progress = progress.coerceAtLeast(0) / 100f,
                    )
                _state.emit(state)

                if (status == DownloadManager.STATUS_SUCCESSFUL) {
                    eventsChannel.send(DownloaderEvent.Successful)
                }
                if (state.isDownloading) {
                    handler.postDelayed(downloadProgressRunnable, 1000L)
                }
            }
        }
        handler.post(downloadProgressRunnable)
    }

    fun onAction(action: DownloaderAction) {
        when (action) {
            is DownloaderAction.Download -> download(action.item, action.storageIndex)
            is DownloaderAction.DeleteDownload -> deleteDownload(action.item)
            is DownloaderAction.CancelDownload -> cancelDownload(action.item)
        }
    }

    override fun onCleared() {
        super.onCleared()
        handler.removeCallbacksAndMessages(null)
    }
}
