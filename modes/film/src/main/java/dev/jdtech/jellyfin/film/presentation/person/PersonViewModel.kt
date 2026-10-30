package dev.jdtech.jellyfin.film.presentation.person

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.models.FindroidBook
import dev.jdtech.jellyfin.models.FindroidMovie
import dev.jdtech.jellyfin.models.FindroidShow
import dev.jdtech.jellyfin.repository.JellyfinRepository
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jellyfin.sdk.model.api.BaseItemKind

@HiltViewModel
class PersonViewModel @Inject internal constructor(private val repository: JellyfinRepository) :
    ViewModel() {
    private val _state = MutableStateFlow(PersonState())
    val state = _state.asStateFlow()

    fun loadPerson(personId: UUID) {
        viewModelScope.launch {
            try {
                val (person, items) =
                    coroutineScope {
                        val personDeferred = async { repository.getPerson(personId) }
                        val itemsDeferred = async {
                            repository.getPersonItems(
                                personIds = listOf(personId),
                                includeTypes =
                                    listOf(
                                        BaseItemKind.MOVIE,
                                        BaseItemKind.SERIES,
                                        BaseItemKind.BOOK,
                                        BaseItemKind.AUDIO_BOOK,
                                    ),
                                recursive = true,
                            )
                        }
                        personDeferred.await() to itemsDeferred.await()
                    }

                val movies = items.filterIsInstance<FindroidMovie>()
                val shows = items.filterIsInstance<FindroidShow>()
                val books = items.filterIsInstance<FindroidBook>()

                _state.emit(
                    _state.value.copy(
                        person = person,
                        starredInMovies = movies,
                        starredInShows = shows,
                        starredInBooks = books,
                    )
                )
            } catch (e: Exception) {
                _state.emit(_state.value.copy(error = e))
            }
        }
    }
}
