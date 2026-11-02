package dev.hdytrfli.onigiri.feature.film.presentation.person

import dev.hdytrfli.onigiri.models.FindroidBook
import dev.hdytrfli.onigiri.models.FindroidMovie
import dev.hdytrfli.onigiri.models.FindroidPerson
import dev.hdytrfli.onigiri.models.FindroidShow

data class PersonState(
    val person: FindroidPerson? = null,
    val starredInMovies: List<FindroidMovie> = emptyList(),
    val starredInShows: List<FindroidShow> = emptyList(),
    val starredInBooks: List<FindroidBook> = emptyList(),
    val error: Exception? = null,
)
