package dev.hdytrfli.onigiri.feature.film.presentation.library

import dev.hdytrfli.onigiri.models.FindroidItem
import dev.hdytrfli.onigiri.models.SortBy
import dev.hdytrfli.onigiri.models.SortOrder

data class LibraryState(
    /**
     * The cached list for the current query. It is kept in the ViewModel, so revisiting the page
     * (or coming back from a detail) renders the same data at once instead of reloading, and it
     * stays on screen while an explicit refresh runs.
     */
    val items: List<FindroidItem> = emptyList(),
    val sortBy: SortBy = SortBy.NAME,
    val sortOrder: SortOrder = SortOrder.ASCENDING,
    val displayFields: Set<ListDisplayField> =
        setOf(
            ListDisplayField.Year,
            ListDisplayField.CommunityRating,
            ListDisplayField.AgeRating,
        ),
    val searchQuery: String = "",
    val searchExpanded: Boolean = false,
    val isLoading: Boolean = false,
    val error: Exception? = null,
)
