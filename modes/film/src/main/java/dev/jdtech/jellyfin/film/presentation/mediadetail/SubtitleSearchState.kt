package dev.jdtech.jellyfin.film.presentation.mediadetail

import dev.jdtech.jellyfin.models.FindroidMediaStream
import org.jellyfin.sdk.model.api.RemoteSubtitleInfo

data class SubtitleSearchState(
    val isLoading: Boolean = false,
    /** Whether the current user may manage subtitles (admins only, per the app's gating). */
    val canManageSubtitles: Boolean = false,
    /** 3-letter ISO language code used for the search. */
    val language: String = "eng",
    /** Subtitles already attached to the item (deletable when they are external). */
    val existing: List<FindroidMediaStream> = emptyList(),
    val isSearching: Boolean = false,
    val results: List<RemoteSubtitleInfo> = emptyList(),
    val downloadingId: String? = null,
    val downloadedIds: Set<String> = emptySet(),
    val deletingIndex: Int? = null,
    val error: Exception? = null,
    val info: RemoteSubtitleInfo? = null,
    val isRefreshing: Boolean = false,
)
