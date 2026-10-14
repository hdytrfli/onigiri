package dev.jdtech.jellyfin.film.presentation.mediadetail

import org.jellyfin.sdk.model.api.RemoteSubtitleInfo

data class SubtitleSearchState(
    val isLoading: Boolean = false,
    /** Whether the current user may manage subtitles (admins only, per the app's gating). */
    val canManageSubtitles: Boolean = false,
    /** 3-letter ISO language code used for the search. */
    val language: String = "eng",
    val isSearching: Boolean = false,
    val results: List<RemoteSubtitleInfo> = emptyList(),
    val downloadingId: String? = null,
    val downloadedId: String? = null,
    val error: Exception? = null,
)
