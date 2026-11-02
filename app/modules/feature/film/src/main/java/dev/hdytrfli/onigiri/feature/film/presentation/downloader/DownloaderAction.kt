package dev.hdytrfli.onigiri.feature.film.presentation.downloader

import dev.hdytrfli.onigiri.models.FindroidItem

sealed interface DownloaderAction {
    data class Download(val item: FindroidItem, val storageIndex: Int = 0) : DownloaderAction

    data class DeleteDownload(val item: FindroidItem) : DownloaderAction

    data class CancelDownload(val item: FindroidItem) : DownloaderAction
}
