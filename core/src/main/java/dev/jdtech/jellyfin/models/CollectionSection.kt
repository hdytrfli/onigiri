package dev.jdtech.jellyfin.models

import dev.jdtech.jellyfin.core.Constants
import dev.jdtech.jellyfin.core.R as CoreR

data class CollectionSection(val id: Int, val name: UiText, var items: List<FindroidItem>)

/**
 * Splits a flat item list into the Movies and Shows sections shared by the Favorites and Downloads
 * pages, so both group content the same way: shows and episodes go to Shows, everything else
 * (movies, box sets, collections) to Movies, and empty sections are dropped so a page that holds
 * only one kind of item shows a single header.
 */
fun mediaSections(items: List<FindroidItem>): List<CollectionSection> {
    if (items.isEmpty()) return emptyList()
    val shows = items.filter { it is FindroidShow || it is FindroidEpisode }
    val movies = items.filterNot { it is FindroidShow || it is FindroidEpisode }
    return buildList {
        if (movies.isNotEmpty()) {
            add(
                CollectionSection(
                    id = Constants.FAVORITE_TYPE_MOVIES,
                    name = UiText.StringResource(CoreR.string.movies_label),
                    items = movies,
                )
            )
        }
        if (shows.isNotEmpty()) {
            add(
                CollectionSection(
                    id = Constants.FAVORITE_TYPE_SHOWS,
                    name = UiText.StringResource(CoreR.string.shows_label),
                    items = shows,
                )
            )
        }
    }
}
