package dev.jdtech.jellyfin.film.presentation.mediadetail

import dev.jdtech.jellyfin.models.FindroidItemPerson
import dev.jdtech.jellyfin.models.VideoMetadata

data class MediaDetailState(
    val isLoading: Boolean = false,
    val title: String? = null,
    val originalTitle: String? = null,
    val overview: String? = null,
    val year: Int? = null,
    val runtimeMinutes: Long? = null,
    val officialRating: String? = null,
    val communityRating: Float? = null,
    val genres: List<String> = emptyList(),
    val people: List<FindroidItemPerson> = emptyList(),
    val videoMetadata: VideoMetadata? = null,
    val error: Exception? = null,
)
