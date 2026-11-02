package dev.hdytrfli.onigiri.feature.film.presentation.media

import dev.hdytrfli.onigiri.models.FindroidCollection

data class MediaState(
    val libraries: List<FindroidCollection> = emptyList(),
    val serverName: String? = null,
    val isLoading: Boolean = false,
    val error: Exception? = null,
)
