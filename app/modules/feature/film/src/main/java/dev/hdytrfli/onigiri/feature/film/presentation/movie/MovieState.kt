package dev.hdytrfli.onigiri.feature.film.presentation.movie

import dev.hdytrfli.onigiri.models.FindroidItem
import dev.hdytrfli.onigiri.models.FindroidItemPerson
import dev.hdytrfli.onigiri.models.FindroidMovie
import dev.hdytrfli.onigiri.models.VideoMetadata

data class MovieState(
    val movie: FindroidMovie? = null,
    val videoMetadata: VideoMetadata? = null,
    val actors: List<FindroidItemPerson> = emptyList(),
    val director: FindroidItemPerson? = null,
    val writers: List<FindroidItemPerson> = emptyList(),
    val recommendations: List<FindroidItem> = emptyList(),
    val collections: List<FindroidItem> = emptyList(),
    val showRecommendations: Boolean = true,
    val displayExtraInfo: Boolean = false,
    val error: Exception? = null,
)
