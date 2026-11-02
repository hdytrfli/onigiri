package dev.hdytrfli.onigiri.swr

import dev.hdytrfli.onigiri.models.CollectionType
import dev.hdytrfli.onigiri.models.SortBy
import dev.hdytrfli.onigiri.models.SortOrder
import java.util.UUID

/** Stable identity of one cached query. Parts are joined into a single persisted string. */
@JvmInline
value class SwrKey(val value: String) {
    override fun toString(): String = value

    companion object {
        fun of(vararg parts: Any?): SwrKey =
            SwrKey(parts.joinToString("|") { it?.toString() ?: "-" })
    }
}

/**
 * Every query key the app uses. Keeping the factories in one place avoids string drift between
 * readers and invalidators.
 */
object QueryKeys {
    val Home = SwrKey("home")
    val Favorites = SwrKey("favorites")
    val Downloads = SwrKey("downloads")

    const val LibraryPrefix = "library|"
    const val GenresPrefix = "genres|"
    const val SuggestionsPrefix = "suggestions|"
    const val CollectionsPrefix = "collections|"
    const val CollectionPrefix = "collection|"
    const val BooksPrefix = "books|"
    const val ItemPrefix = "item|"

    fun genres(kind: String): SwrKey = SwrKey("$GenresPrefix$kind")

    fun suggestions(kind: String): SwrKey = SwrKey("$SuggestionsPrefix$kind")

    fun collections(kind: String): SwrKey = SwrKey("$CollectionsPrefix$kind")

    fun collection(id: UUID): SwrKey = SwrKey("$CollectionPrefix$id")

    fun books(sortBy: SortBy, sortOrder: SortOrder): SwrKey =
        SwrKey("$BooksPrefix${sortBy.name}|${sortOrder.name}")

    fun library(
        parentId: UUID?,
        libraryType: CollectionType,
        sortBy: SortBy,
        sortOrder: SortOrder,
        query: String?,
        genreIds: List<UUID>,
    ): SwrKey =
        SwrKey(
            "$LibraryPrefix${parentId ?: "root"}|${libraryType.name}|${sortBy.name}|" +
                "${sortOrder.name}|${query.orEmpty()}|${genreIds.sorted().joinToString(",")}"
        )

    fun item(id: UUID): SwrKey = SwrKey("$ItemPrefix$id")

    fun person(id: UUID): SwrKey = SwrKey("person|$id")
}
