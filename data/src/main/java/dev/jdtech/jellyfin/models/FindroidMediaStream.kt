package dev.jdtech.jellyfin.models

import dev.jdtech.jellyfin.repository.JellyfinRepository
import org.jellyfin.sdk.model.api.MediaStream
import org.jellyfin.sdk.model.api.MediaStreamType
import org.jellyfin.sdk.model.api.VideoRangeType

data class FindroidMediaStream(
    val index: Int,
    val mediaSourceId: String,
    val title: String,
    val displayTitle: String?,
    val language: String,
    val type: MediaStreamType,
    val codec: String,
    val isExternal: Boolean,
    val path: String?,
    val channelLayout: String?,
    val videoRangeType: VideoRangeType?,
    val height: Int?,
    val width: Int?,
    val videoDoViTitle: String?,
)

fun MediaStream.toFindroidMediaStream(
    jellyfinRepository: JellyfinRepository,
    mediaSourceId: String,
): FindroidMediaStream {
    return FindroidMediaStream(
        index = index,
        mediaSourceId = mediaSourceId,
        title = title.orEmpty(),
        displayTitle = displayTitle,
        language = language.orEmpty(),
        type = type,
        codec = codec.orEmpty(),
        isExternal = isExternal,
        path = jellyfinRepository.getBaseUrl() + deliveryUrl,
        channelLayout = channelLayout,
        videoRangeType = videoRangeType,
        height = height,
        width = width,
        videoDoViTitle = videoDoViTitle,
    )
}

fun FindroidMediaStreamDto.toFindroidMediaStream(): FindroidMediaStream {
    return FindroidMediaStream(
        // The stream index is not persisted locally; deleting only happens while online.
        index = -1,
        mediaSourceId = sourceId,
        title = title,
        displayTitle = displayTitle,
        language = language,
        type = type,
        codec = codec,
        isExternal = isExternal,
        path = path,
        channelLayout = channelLayout,
        videoRangeType = VideoRangeType.fromNameOrNull(videoRangeType ?: ""),
        height = height,
        width = width,
        videoDoViTitle = videoDoViTitle,
    )
}
