package dev.hdytrfli.onigiri.feature.film.presentation.person

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.hdytrfli.onigiri.models.FindroidBook
import dev.hdytrfli.onigiri.models.FindroidMovie
import dev.hdytrfli.onigiri.models.FindroidShow
import dev.hdytrfli.onigiri.repository.JellyfinRepository
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
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
            _state.emit(_state.value.copy(error = null))
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
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.emit(_state.value.copy(error = e))
            }
        }
    }
}
