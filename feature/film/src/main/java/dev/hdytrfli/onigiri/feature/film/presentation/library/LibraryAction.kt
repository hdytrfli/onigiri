package dev.hdytrfli.onigiri.feature.film.presentation.library

import dev.hdytrfli.onigiri.models.FindroidItem
import dev.hdytrfli.onigiri.models.SortBy
import dev.hdytrfli.onigiri.models.SortOrder

sealed interface LibraryAction {
    data class OnItemClick(val item: FindroidItem) : LibraryAction

    data object OnBackClick : LibraryAction

    data class ChangeSorting(val sortBy: SortBy, val sortOrder: SortOrder) : LibraryAction

    data class ToggleDisplayField(val field: ListDisplayField) : LibraryAction

    data class SetSearchQuery(val query: String) : LibraryAction

    data object ToggleSearch : LibraryAction

    /** Re-fetches the item whose detail page was last opened, if any. */
    data object RefreshOpenedItem : LibraryAction

    /** Re-fetches the whole list; the cached items stay visible while it runs. */
    data object RefreshItems : LibraryAction
}
