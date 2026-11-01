package dev.jdtech.jellyfin

import dev.jdtech.jellyfin.models.CollectionType
import dev.jdtech.jellyfin.models.FindroidItem
import java.util.UUID

/**
 * In-memory item cache for genre pages. Genre routes are popped when the user goes back to the
 * genres list, which clears their ViewModel; keeping the last list here lets a reopened genre show
 * its previous contents immediately while it revalidates. Scoped to the main navigation
 * composition, so it is discarded on restart/profile switch.
 */
class GenreItemsCache(private val maxEntries: Int = 8) {
    data class Key(val genreId: UUID, val libraryType: CollectionType)

    private val entries =
        object : LinkedHashMap<Key, List<FindroidItem>>(maxEntries, 0.75f, true) {
            override fun removeEldestEntry(
                eldest: MutableMap.MutableEntry<Key, List<FindroidItem>>?
            ): Boolean = size > maxEntries
        }

    @Synchronized fun get(key: Key): List<FindroidItem>? = entries[key]

    @Synchronized
    fun put(key: Key, items: List<FindroidItem>) {
        if (items.isNotEmpty()) {
            entries[key] = items
        }
    }
}
