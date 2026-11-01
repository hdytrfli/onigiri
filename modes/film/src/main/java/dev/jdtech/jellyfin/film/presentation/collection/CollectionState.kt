package dev.jdtech.jellyfin.film.presentation.collection

import dev.jdtech.jellyfin.film.presentation.library.ListDisplayField
import dev.jdtech.jellyfin.models.CollectionSection
import dev.jdtech.jellyfin.models.FindroidBoxSet
import dev.jdtech.jellyfin.models.FindroidItem
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
