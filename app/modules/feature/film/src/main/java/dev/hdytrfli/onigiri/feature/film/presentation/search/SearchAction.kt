package dev.hdytrfli.onigiri.feature.film.presentation.search

import dev.hdytrfli.onigiri.models.FindroidItem

sealed interface SearchAction {
    data class Search(val query: String) : SearchAction

    data class OnItemClick(val item: FindroidItem) : SearchAction
}
