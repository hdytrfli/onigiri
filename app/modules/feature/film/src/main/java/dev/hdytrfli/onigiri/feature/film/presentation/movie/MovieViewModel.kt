package dev.hdytrfli.onigiri.feature.film.presentation.movie

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.hdytrfli.onigiri.feature.film.domain.VideoMetadataParser
import dev.hdytrfli.onigiri.models.FindroidItem
import dev.hdytrfli.onigiri.models.FindroidItemPerson
import dev.hdytrfli.onigiri.models.FindroidMovie
import dev.hdytrfli.onigiri.repository.JellyfinRepository
import dev.hdytrfli.onigiri.settings.domain.AppPreferences
import dev.hdytrfli.onigiri.swr.SwrStore
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.sdk.model.api.PersonKind

@HiltViewModel
class MovieViewModel
@Inject
constructor(
    private val repository: JellyfinRepository,
    private val appPreferences: AppPreferences,
    private val videoMetadataParser: VideoMetadataParser,
    private val swrStore: SwrStore,
) : ViewModel() {
    private val _state = MutableStateFlow(MovieState())
    val state = _state.asStateFlow()

    lateinit var movieId: UUID

    fun loadMovie(movieId: UUID) {
        this.movieId = movieId
        viewModelScope.launch {
            _state.emit(_state.value.copy(error = null))
            try {
                val movie = repository.getMovie(movieId)
                val source = movie.sources.firstOrNull()
                val videoMetadata = if (source != null) videoMetadataParser.parse(source) else null
                val actors = getActors(movie)
                val director = getDirector(movie)
                val writers = getWriters(movie)
                val displayExtraInfo = appPreferences.getValue(appPreferences.displayExtraInfo)
                val showRecommendations =
                    appPreferences.getValue(appPreferences.showRecommendations)
                val (recommendations, collections) =
                    if (showRecommendations) {
                        coroutineScope {
                            val recommendations = async {
                                runCatching { repository.getSimilarItems(movieId) }
                                    .getOrDefault(emptyList())
                            }
                            val collections = async {
                                runCatching { repository.getItemCollections(movieId) }
                                    .getOrDefault(emptyList())
                            }
                            recommendations.await() to collections.await()
                        }
                    } else {
                        emptyList<FindroidItem>() to emptyList()
                    }
                _state.emit(
                    _state.value.copy(
                        movie = movie,
                        videoMetadata = videoMetadata,
                        actors = actors,
                        director = director,
                        writers = writers,
                        recommendations = recommendations,
                        collections = collections,
                        showRecommendations = showRecommendations,
                        displayExtraInfo = displayExtraInfo,
                    )
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.emit(_state.value.copy(error = e))
            }
        }
    }

    private suspend fun getActors(item: FindroidMovie): List<FindroidItemPerson> {
        return withContext(Dispatchers.Default) {
            item.people.filter { it.type == PersonKind.ACTOR }
        }
    }

    private suspend fun getDirector(item: FindroidMovie): FindroidItemPerson? {
        return withContext(Dispatchers.Default) {
            item.people.firstOrNull { it.type == PersonKind.DIRECTOR }
        }
    }

    private suspend fun getWriters(item: FindroidMovie): List<FindroidItemPerson> {
        return withContext(Dispatchers.Default) {
            item.people.filter { it.type == PersonKind.WRITER }
        }
    }

    fun onAction(action: MovieAction) {
        when (action) {
            is MovieAction.MarkAsPlayed -> mutate { repository.markAsPlayed(movieId) }
            is MovieAction.UnmarkAsPlayed -> mutate { repository.markAsUnplayed(movieId) }
            is MovieAction.MarkAsFavorite -> mutate { repository.markAsFavorite(movieId) }
            is MovieAction.UnmarkAsFavorite -> mutate { repository.unmarkAsFavorite(movieId) }
            else -> Unit
        }
    }

    private fun mutate(block: suspend () -> Unit) {
        viewModelScope.launch {
            if (runCatching { block() }.isSuccess) {
                swrStore.invalidateMediaLists()
                loadMovie(movieId)
            }
        }
    }
}
