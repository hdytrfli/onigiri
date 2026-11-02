package dev.hdytrfli.onigiri.feature.film.presentation.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.hdytrfli.onigiri.feature.film.presentation.library.ListDisplayField
import dev.hdytrfli.onigiri.models.FindroidBoxSet
import dev.hdytrfli.onigiri.models.FindroidItem
import dev.hdytrfli.onigiri.models.SortBy
import dev.hdytrfli.onigiri.repository.JellyfinRepository
import dev.hdytrfli.onigiri.settings.domain.AppPreferences
import dev.hdytrfli.onigiri.swr.QueryKeys
import dev.hdytrfli.onigiri.swr.SwrEntry
import dev.hdytrfli.onigiri.swr.SwrStore
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data class CollectionDetailCache(
    val collection: FindroidBoxSet? = null,
    val items: List<FindroidItem> = emptyList(),
)

@HiltViewModel
class CollectionViewModel
@Inject
constructor(
    private val repository: JellyfinRepository,
    private val appPreferences: AppPreferences,
    private val swrStore: SwrStore,
) : ViewModel() {
    private val _state = MutableStateFlow(CollectionState())
    val state = _state.asStateFlow()

    private val serializer = CollectionDetailCache.serializer()
    private var currentId: UUID? = null
    private var observeJob: Job? = null

    fun loadItems(parentId: UUID, force: Boolean = false) {
        _state.value =
            _state.value.copy(
                displayFields =
                    ListDisplayField.fromKeys(
                        appPreferences.getValue(appPreferences.listDisplayFields)
                    )
            )
        observe(parentId)
        swrStore.refresh(QueryKeys.collection(parentId), serializer, force = force) {
            fetchCollection(parentId)
        }
    }

    fun retry(parentId: UUID) {
        loadItems(parentId, force = true)
    }

    private fun observe(parentId: UUID) {
        if (currentId == parentId) return
        currentId = parentId
        _state.value =
            _state.value.copy(
                collection = null,
                items = emptyList(),
                sections = emptyList(),
                isLoading = true,
                error = null,
            )
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            swrStore.observe(QueryKeys.collection(parentId), serializer).collect(::applyEntry)
        }
    }

    private fun applyEntry(entry: SwrEntry<CollectionDetailCache>) {
        val cache = entry.data
        if (cache == null) {
            if (entry.isRefreshing || entry.error != null) {
                _state.value =
                    _state.value.copy(
                        isLoading = entry.isRefreshing,
                        error = entry.error as? Exception,
                    )
            } else {
                _state.value =
                    _state.value.copy(
                        collection = null,
                        items = emptyList(),
                        sections = emptyList(),
                        isLoading = entry.isRefreshing,
                        error = null,
                    )
            }
            return
        }
        _state.value =
            _state.value.copy(
                collection = cache.collection,
                items = cache.items,
                isLoading = entry.isRefreshing,
                error = entry.error as? Exception,
            )
    }

    private suspend fun fetchCollection(parentId: UUID): CollectionDetailCache {
        val collection = repository.getItem(parentId) as? FindroidBoxSet
        val items = repository.getItems(parentId = parentId, sortBy = SortBy.RELEASE_DATE)
        return CollectionDetailCache(collection = collection, items = items)
    }
}
