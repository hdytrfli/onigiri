package dev.hdytrfli.onigiri.feature.film.presentation.media

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.hdytrfli.onigiri.models.FindroidItem
import dev.hdytrfli.onigiri.models.GenreSection
import dev.hdytrfli.onigiri.models.SortBy
import dev.hdytrfli.onigiri.repository.JellyfinRepository
import dev.hdytrfli.onigiri.swr.QueryKeys
import dev.hdytrfli.onigiri.swr.SwrEntry
import dev.hdytrfli.onigiri.swr.SwrStore
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer
import org.jellyfin.sdk.model.api.BaseItemKind

data class GenresState(
    val sections: List<GenreSection> = emptyList(),
    val isLoading: Boolean = false,
    val error: Exception? = null,
)

@HiltViewModel
class GenresViewModel
@Inject
constructor(
    private val repository: JellyfinRepository,
    private val swrStore: SwrStore,
) : ViewModel() {
    private val _state = MutableStateFlow(GenresState())
    val state = _state.asStateFlow()

    private val serializer = ListSerializer(GenreSection.serializer())
    private var currentKind: MediaKind? = null
    private var observeJob: Job? = null

    /** Switches to [kind]'s cached row set and refreshes it only when it is stale. */
    fun load(kind: MediaKind, force: Boolean = false) {
        observe(kind)
        swrStore.refresh(QueryKeys.genres(kind.name), serializer, force = force) {
            fetchGenres(kind)
        }
    }

    fun retry(kind: MediaKind) {
        load(kind, force = true)
    }

    private fun observe(kind: MediaKind) {
        if (currentKind == kind) return
        currentKind = kind
        _state.value = GenresState(isLoading = true)
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            swrStore.observe(QueryKeys.genres(kind.name), serializer).collect(::applyEntry)
        }
    }

    private fun applyEntry(entry: SwrEntry<List<GenreSection>>) {
        _state.value =
            _state.value.copy(
                sections =
                    entry.data
                        ?: if (entry.isRefreshing || entry.error != null) {
                            _state.value.sections
                        } else {
                            emptyList()
                        },
                isLoading = entry.isRefreshing,
                error = entry.error as? Exception,
            )
    }

    private suspend fun fetchGenres(kind: MediaKind): List<GenreSection> {
        val includeType = if (kind == MediaKind.Movies) BaseItemKind.MOVIE else BaseItemKind.SERIES
        val genres = repository.getGenres(includeTypes = listOf(includeType))
        return genres.mapNotNull { genre ->
            val items =
                try {
                    repository.getItems(
                        includeTypes = listOf(includeType),
                        recursive = true,
                        sortBy = SortBy.NAME,
                        genreIds = listOf(genre.id),
                        limit = SECTION_LIMIT,
                    )
                } catch (_: Exception) {
                    emptyList<FindroidItem>()
                }
            if (items.isEmpty()) {
                null
            } else {
                GenreSection(id = genre.id, name = genre.name, items = items)
            }
        }
    }

    private companion object {
        const val SECTION_LIMIT = 18
    }
}
