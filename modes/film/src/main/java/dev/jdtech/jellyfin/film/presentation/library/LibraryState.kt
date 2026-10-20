package dev.jdtech.jellyfin.film.presentation.library

import androidx.paging.PagingData
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.models.SortBy
import dev.jdtech.jellyfin.models.SortOrder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

data class LibraryState(
    val items: Flow<PagingData<FindroidItem>> = emptyFlow(),
    /** The server's total for the current query, reported by the first page; null until then. */
    val itemCount: Int? = null,
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
