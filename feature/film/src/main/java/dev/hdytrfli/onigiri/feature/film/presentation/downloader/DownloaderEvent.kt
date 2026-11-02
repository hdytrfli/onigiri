package dev.hdytrfli.onigiri.feature.film.presentation.downloader

sealed interface DownloaderEvent {
    data object Successful : DownloaderEvent

    data object Deleted : DownloaderEvent
}
