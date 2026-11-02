package dev.hdytrfli.onigiri.feature.film.presentation.home

import dev.hdytrfli.onigiri.feature.film.presentation.library.ListDisplayField
import dev.hdytrfli.onigiri.models.HomeItem
import dev.hdytrfli.onigiri.models.Server

data class HomeState(
    val server: Server? = null,
    val displayFields: Set<ListDisplayField> = emptySet(),
    val suggestionsSection: HomeItem.Suggestions? = null,
    val resumeSection: HomeItem.Section? = null,
    val nextUpSection: HomeItem.Section? = null,
    val latestBooksSection: HomeItem.Section? = null,
    val latestAudiobooksSection: HomeItem.Section? = null,
    val views: List<HomeItem.ViewItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: Exception? = null,
)
