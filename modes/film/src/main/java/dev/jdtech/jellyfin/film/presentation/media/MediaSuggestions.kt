package dev.jdtech.jellyfin.film.presentation.media

import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.models.UiText

enum class MediaKind {
    Movies,
    Shows,
}

/** One horizontally scrolling row on the Movies/Shows page. */
data class MediaSection(
    val id: String,
    val title: UiText,
    val items: List<FindroidItem>,
    /** The "newly added" row links to the sortable list page. */
    val showsListLink: Boolean = false,
    /** Continue Watching / Next Up use the wide thumbnail cards instead of posters. */
    val useThumbnails: Boolean = false,
    /** Continue Watching shows how much of the item is left. */
    val showRemainingTime: Boolean = false,
)

data class MediaSuggestionsState(
    val sections: List<MediaSection> = emptyList(),
    val isLoading: Boolean = false,
    val error: Exception? = null,
)
