package dev.jdtech.jellyfin.models

import dev.jdtech.jellyfin.repository.JellyfinRepository
import java.util.UUID
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.PlayAccess

/**
 * A Jellyfin book rendered in the in-app reader. Only publication formats Readium can open are
 * mapped (EPUB, PDF, CBZ); audiobooks are a different item kind and are never mapped here.
 */
data class FindroidBook(
    override val id: UUID,
    override val name: String,
    override val originalTitle: String?,
    override val overview: String,
    override val sources: List<FindroidSource>,
    override val played: Boolean,
    override val favorite: Boolean,
    override val canPlay: Boolean,
    override val canDownload: Boolean,
    override val runtimeTicks: Long,
    override val playbackPositionTicks: Long,
    override val unplayedItemCount: Int? = null,
    override val images: FindroidImages,
    override val chapters: List<FindroidChapter>,
) : FindroidItem

suspend fun BaseItemDto.toFindroidBook(jellyfinRepository: JellyfinRepository): FindroidBook {
    return FindroidBook(
        id = id,
        name = name.orEmpty(),
        originalTitle = originalTitle,
        overview = overview.orEmpty(),
        sources = mediaSources?.map { it.toFindroidSource(jellyfinRepository, id) } ?: emptyList(),
        played = userData?.played == true,
        favorite = userData?.isFavorite == true,
        canPlay = playAccess != PlayAccess.NONE,
        canDownload = canDownload == true,
        runtimeTicks = 0,
        playbackPositionTicks = userData?.playbackPositionTicks ?: 0,
        images = toFindroidImages(jellyfinRepository),
        chapters = toFindroidChapters(),
    )
}

/** The publication formats the in-app reader supports. */
private val READABLE_BOOK_CONTAINERS = setOf("epub", "pdf", "cbz")

/**
 * Whether this book is in a format Readium can open. Jellyfin reports the container on the item and
 * on its media sources; the file extension is the last fallback.
 */
fun BaseItemDto.isReadableBook(): Boolean {
    val candidates = buildList {
        container?.let(::add)
        mediaSources?.forEach { source -> source.container?.let(::add) }
        path?.let { add(it.substringAfterLast('.', missingDelimiterValue = "")) }
    }
    return candidates.any { it.lowercase() in READABLE_BOOK_CONTAINERS }
}
