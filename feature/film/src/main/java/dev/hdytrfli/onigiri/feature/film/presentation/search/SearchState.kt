package dev.hdytrfli.onigiri.feature.film.presentation.search

import dev.hdytrfli.onigiri.models.FindroidItem

data class SearchState(val items: List<FindroidItem> = emptyList(), val loading: Boolean = false)
