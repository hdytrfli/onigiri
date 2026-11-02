package dev.hdytrfli.onigiri.utils

import dev.hdytrfli.onigiri.models.FindroidItem
import dev.hdytrfli.onigiri.models.FindroidSource
import dev.hdytrfli.onigiri.models.UiText

interface Downloader {
    suspend fun downloadItem(
        item: FindroidItem,
        sourceId: String,
        storageIndex: Int = 0,
    ): Pair<Long, UiText?>

    suspend fun cancelDownload(item: FindroidItem, downloadId: Long)

    suspend fun deleteItem(item: FindroidItem, source: FindroidSource)

    suspend fun getProgress(downloadId: Long?): Pair<Int, Int>
}
