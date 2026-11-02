package dev.hdytrfli.onigiri.models

import dev.hdytrfli.onigiri.core.Constants
import dev.hdytrfli.onigiri.core.R as CoreR

data class CollectionSection(val id: Int, val name: UiText, var items: List<FindroidItem>)

/**
 * Splits a flat item list into the Movies, Shows, Seasons and Episodes sections shared by the
 * Favorites and Downloads pages, so both group content the same way: every kind gets its own header
 * and the grid can give episodes their 16:9 thumbnail density while the poster kinds keep the
 * poster grid, and empty sections are dropped so a page that holds only one kind of item shows a
 * single header.
 */
fun mediaSections(items: List<FindroidItem>): List<CollectionSection> {
    if (items.isEmpty()) return emptyList()
    val episodes = items.filter { it is FindroidEpisode }
    val seasons = items.filter { it is FindroidSeason }
    val shows = items.filter { it is FindroidShow }
    val books = items.filter { it is FindroidBook }
    val movies = items.filterNot {
        it is FindroidShow || it is FindroidSeason || it is FindroidEpisode || it is FindroidBook
    }
    return buildList {
        if (books.isNotEmpty()) {
            add(
                CollectionSection(
                    id = Constants.FAVORITE_TYPE_BOOKS,
                    name = UiText.StringResource(CoreR.string.books_label),
                    items = books,
                )
            )
        }
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
        if (seasons.isNotEmpty()) {
            add(
                CollectionSection(
                    id = Constants.FAVORITE_TYPE_SEASONS,
                    name = UiText.StringResource(CoreR.string.seasons),
                    items = seasons,
                )
            )
        }
        if (episodes.isNotEmpty()) {
            add(
                CollectionSection(
                    id = Constants.FAVORITE_TYPE_EPISODES,
                    name = UiText.StringResource(CoreR.string.episodes_label),
                    items = episodes,
                )
            )
        }
    }
}
