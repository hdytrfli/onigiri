package dev.jdtech.jellyfin.film.presentation.media

import dev.jdtech.jellyfin.film.presentation.library.ListDisplayField
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
)

data class MediaSuggestionsState(
    val sections: List<MediaSection> = emptyList(),
    val displayFields: Set<ListDisplayField> =
        setOf(
            ListDisplayField.Year,
            ListDisplayField.CommunityRating,
            ListDisplayField.AgeRating,
        ),
    val isLoading: Boolean = false,
    val error: Exception? = null,
)
