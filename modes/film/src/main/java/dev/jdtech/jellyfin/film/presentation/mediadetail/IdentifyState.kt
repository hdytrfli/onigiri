package dev.jdtech.jellyfin.film.presentation.mediadetail

import org.jellyfin.sdk.model.api.RemoteSearchResult

data class IdentifyState(
    val query: String = "",
    val year: String = "",
    val imdbId: String = "",
    val tmdbId: String = "",
    val tvdbId: String = "",
    val isSearching: Boolean = false,
    val results: List<RemoteSearchResult> = emptyList(),
    val error: Exception? = null,
    val info: RemoteSearchResult? = null,
    val isApplying: Boolean = false,
    val canManage: Boolean = false,
)
