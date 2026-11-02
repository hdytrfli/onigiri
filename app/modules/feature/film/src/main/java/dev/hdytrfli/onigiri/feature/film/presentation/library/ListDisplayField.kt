package dev.hdytrfli.onigiri.feature.film.presentation.library

/** The metadata a movie/show list card can show. [key] is what the preference stores. */
enum class ListDisplayField(val key: String) {
    Year("year"),
    CommunityRating("rating"),
    AgeRating("age");

    companion object {
        fun fromKeys(keys: Set<String>): Set<ListDisplayField> =
            entries.filterTo(mutableSetOf()) { it.key in keys }
    }
}
