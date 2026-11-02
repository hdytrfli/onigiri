package dev.jdtech.jellyfin.film.presentation.season

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.repository.JellyfinRepository
import dev.jdtech.jellyfin.swr.SwrStore
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jellyfin.sdk.model.api.ItemFields

@HiltViewModel
class SeasonViewModel
@Inject
constructor(
    private val repository: JellyfinRepository,
    private val swrStore: SwrStore,
) : ViewModel() {
    private val _state = MutableStateFlow(SeasonState())
    val state = _state.asStateFlow()

    lateinit var seasonId: UUID

    fun loadSeason(seasonId: UUID) {
        this.seasonId = seasonId
        viewModelScope.launch {
            _state.emit(_state.value.copy(error = null))
            try {
                val season = repository.getSeason(seasonId)
                val episodes =
                    repository.getEpisodes(
                        seriesId = season.seriesId,
                        seasonId = seasonId,
                        fields = listOf(ItemFields.OVERVIEW),
                    )
                _state.emit(
                    _state.value.copy(
                        season = season,
                        episodes = episodes,
                    )
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.emit(_state.value.copy(error = e))
            }
        }
    }

    fun onAction(action: SeasonAction) {
        when (action) {
            is SeasonAction.MarkAsPlayed -> mutate { repository.markAsPlayed(seasonId) }
            is SeasonAction.UnmarkAsPlayed -> mutate { repository.markAsUnplayed(seasonId) }
            is SeasonAction.MarkAsFavorite -> mutate { repository.markAsFavorite(seasonId) }
            is SeasonAction.UnmarkAsFavorite -> mutate { repository.unmarkAsFavorite(seasonId) }
            else -> Unit
        }
    }

    private fun mutate(block: suspend () -> Unit) {
        viewModelScope.launch {
            if (runCatching { block() }.isSuccess) {
                swrStore.invalidateMediaLists()
                loadSeason(seasonId)
            }
        }
    }
}
