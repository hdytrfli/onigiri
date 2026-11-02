package dev.hdytrfli.onigiri.feature.film.presentation.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.hdytrfli.onigiri.feature.film.presentation.collection.CollectionState
import dev.hdytrfli.onigiri.feature.film.presentation.library.ListDisplayField
import dev.hdytrfli.onigiri.models.FindroidItem
import dev.hdytrfli.onigiri.models.mediaSections
import dev.hdytrfli.onigiri.repository.JellyfinRepository
import dev.hdytrfli.onigiri.settings.domain.AppPreferences
import dev.hdytrfli.onigiri.swr.QueryKeys
import dev.hdytrfli.onigiri.swr.SwrStore
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer

@HiltViewModel
class FavoritesViewModel
@Inject
constructor(
    private val repository: JellyfinRepository,
    private val appPreferences: AppPreferences,
    private val swrStore: SwrStore,
) : ViewModel() {
    private val _state = MutableStateFlow(CollectionState())
    val state = _state.asStateFlow()

    private val key = QueryKeys.Favorites
    private val serializer = ListSerializer(FindroidItem.serializer())

    init {
        viewModelScope.launch {
            swrStore.observe(key, serializer).collect { entry ->
                val items =
                    entry.data
                        ?: if (entry.isRefreshing || entry.error != null) {
                            _state.value.items
                        } else {
                            emptyList()
                        }
                _state.value =
                    CollectionState(
                        items = items,
                        sections = mediaSections(items),
                        displayFields =
                            ListDisplayField.fromKeys(
                                appPreferences.getValue(appPreferences.listDisplayFields)
                            ),
                        isLoading = entry.isRefreshing,
                        error = entry.error as? Exception,
                    )
            }
        }
    }

    /** Entry load: uses the cached list and refreshes only when it is stale or absent. */
    fun loadItemsIfNeeded() {
        loadItems()
    }

    fun loadItems(force: Boolean = false) {
        swrStore.refresh(key, serializer, force = force) { repository.getFavoriteItems() }
    }

    /** Flips [field] in the shared list display preference every list page reads. */
    fun toggleDisplayField(field: ListDisplayField) {
        val fields = _state.value.displayFields.toMutableSet()
        if (!fields.add(field)) {
            fields.remove(field)
        }
        appPreferences.setValue(appPreferences.listDisplayFields, fields.map { it.key }.toSet())
        _state.value = _state.value.copy(displayFields = fields)
    }
}
