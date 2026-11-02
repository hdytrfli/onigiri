package dev.hdytrfli.onigiri.feature.film.presentation.collection

import dev.hdytrfli.onigiri.feature.film.presentation.library.ListDisplayField
import dev.hdytrfli.onigiri.models.CollectionSection
import dev.hdytrfli.onigiri.models.FindroidBoxSet
import dev.hdytrfli.onigiri.models.FindroidItem
import java.util.UUID

data class CollectionState(
    val collection: FindroidBoxSet? = null,
    val items: List<FindroidItem> = emptyList(),
    val displayFields: Set<ListDisplayField> = emptySet(),
    val sections: List<CollectionSection> = emptyList(),
    val isLoading: Boolean = false,
    val error: Exception? = null,
    /** Download progress (0..100) per item id, for items currently downloading. */
    val downloadProgress: Map<UUID, Int> = emptyMap(),
)
