package dev.hdytrfli.onigiri.feature.film.presentation.show

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.hdytrfli.onigiri.models.FindroidEpisode
import dev.hdytrfli.onigiri.models.FindroidItem
import dev.hdytrfli.onigiri.models.FindroidItemPerson
import dev.hdytrfli.onigiri.models.FindroidShow
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
class ShowViewModel
@Inject
constructor(
    private val repository: JellyfinRepository,
    private val appPreferences: AppPreferences,
    private val swrStore: SwrStore,
) : ViewModel() {
    private val _state = MutableStateFlow(ShowState())
    val state = _state.asStateFlow()

    lateinit var showId: UUID

    fun loadShow(showId: UUID) {
        this.showId = showId
        viewModelScope.launch {
            _state.emit(_state.value.copy(error = null))
            try {
                val show = repository.getShow(showId)
                val nextUp = getNextUp(showId)
                val seasons = repository.getSeasons(showId)
                val actors = getActors(show)
                val director = getDirector(show)
                val writers = getWriters(show)
                val showRecommendations =
                    appPreferences.getValue(appPreferences.showRecommendations)
                val (recommendations, collections) =
                    if (showRecommendations) {
                        coroutineScope {
                            val recommendations = async {
                                runCatching { repository.getSimilarItems(showId) }
                                    .getOrDefault(emptyList())
                            }
                            val collections = async {
                                runCatching { repository.getItemCollections(showId) }
                                    .getOrDefault(emptyList())
                            }
                            recommendations.await() to collections.await()
                        }
                    } else {
                        emptyList<FindroidItem>() to emptyList()
                    }
                _state.emit(
                    _state.value.copy(
                        show = show,
                        nextUp = nextUp,
                        seasons = seasons,
                        actors = actors,
                        director = director,
                        writers = writers,
                        recommendations = recommendations,
                        collections = collections,
                        showRecommendations = showRecommendations,
                    )
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.emit(_state.value.copy(error = e))
            }
        }
    }

    private suspend fun getNextUp(showId: UUID): FindroidEpisode? {
        val nextUpItems = repository.getNextUp(showId)
        return nextUpItems.getOrNull(0)
    }

    private suspend fun getActors(item: FindroidShow): List<FindroidItemPerson> {
        return withContext(Dispatchers.Default) {
            item.people.filter { it.type == PersonKind.ACTOR }
        }
    }

    private suspend fun getDirector(item: FindroidShow): FindroidItemPerson? {
        return withContext(Dispatchers.Default) {
            item.people.firstOrNull { it.type == PersonKind.DIRECTOR }
        }
    }

    private suspend fun getWriters(item: FindroidShow): List<FindroidItemPerson> {
        return withContext(Dispatchers.Default) {
            item.people.filter { it.type == PersonKind.WRITER }
        }
    }

    fun onAction(action: ShowAction) {
        when (action) {
            is ShowAction.MarkAsPlayed -> mutate { repository.markAsPlayed(showId) }
            is ShowAction.UnmarkAsPlayed -> mutate { repository.markAsUnplayed(showId) }
            is ShowAction.MarkAsFavorite -> mutate { repository.markAsFavorite(showId) }
            is ShowAction.UnmarkAsFavorite -> mutate { repository.unmarkAsFavorite(showId) }
            else -> Unit
        }
    }

    private fun mutate(block: suspend () -> Unit) {
        viewModelScope.launch {
            if (runCatching { block() }.isSuccess) {
                swrStore.invalidateMediaLists()
                loadShow(showId)
            }
        }
    }
}
