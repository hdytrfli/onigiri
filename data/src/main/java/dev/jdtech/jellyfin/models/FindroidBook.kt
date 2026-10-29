package dev.jdtech.jellyfin.models

import dev.jdtech.jellyfin.database.ServerDatabaseDao
import dev.jdtech.jellyfin.repository.JellyfinRepository
import java.util.UUID
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.MediaType
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
    /** Audiobooks are listed with the books but the reader cannot play them yet. */
    val isAudiobook: Boolean = false,
    val productionYear: Int? = null,
    val premiereDate: java.time.LocalDateTime? = null,
    val genres: List<String> = emptyList(),
    val studios: List<String> = emptyList(),
    val people: List<FindroidItemPerson> = emptyList(),
) : FindroidItem

suspend fun BaseItemDto.toFindroidBook(
    jellyfinRepository: JellyfinRepository,
    serverDatabase: ServerDatabaseDao? = null,
): FindroidBook {
    val sources = mutableListOf<FindroidSource>()
    sources.addAll(mediaSources?.map { it.toFindroidSource(jellyfinRepository, id) } ?: emptyList())
    if (serverDatabase != null) {
        sources.addAll(serverDatabase.getSources(id).map { it.toFindroidSource(serverDatabase) })
    }
    // Books often arrive without mediaSources on the item payload; fetch them explicitly.
    if (sources.isEmpty()) {
        sources.addAll(
            runCatching { jellyfinRepository.getMediaSources(id) }.getOrDefault(emptyList())
        )
    }
    return FindroidBook(
        id = id,
        name = name.orEmpty(),
        originalTitle = originalTitle,
        overview = overview.orEmpty(),
        sources = sources,
        played = userData?.played == true,
        favorite = userData?.isFavorite == true,
        canPlay = playAccess != PlayAccess.NONE,
        canDownload = canDownload == true && sources.isNotEmpty(),
        runtimeTicks = 0,
        playbackPositionTicks = userData?.playbackPositionTicks ?: 0,
        images = toFindroidImages(jellyfinRepository),
        chapters = toFindroidChapters(),
        isAudiobook =
            type == org.jellyfin.sdk.model.api.BaseItemKind.AUDIO_BOOK || isAudiobookItem(),
        productionYear = productionYear,
        premiereDate = premiereDate,
        genres = genres ?: emptyList(),
        studios = studios?.mapNotNull { it.name } ?: emptyList(),
        people = people?.map { it.toFindroidPerson(jellyfinRepository) } ?: emptyList(),
    )
}

/** The publication formats the in-app reader supports. */
private val READABLE_BOOK_CONTAINERS = setOf("epub", "pdf", "cbz", "zip")

/** Containers of audiobooks that a server might still report as a Book item. */
private val AUDIOBOOK_CONTAINERS = setOf("m4b", "m4a", "mp3", "flac", "ogg", "opus", "wav", "aac")

private fun BaseItemDto.bookContainers(): List<String> = buildList {
    container?.let(::add)
    mediaSources?.forEach { source -> source.container?.let(::add) }
    path?.let { add(it.substringAfterLast('.', missingDelimiterValue = "")) }
}
    .map { it.trim().lowercase() }
    .filter { it.isNotEmpty() }

/** Whether this is an audiobook, which is listed but cannot be read by the reader. */
fun BaseItemDto.isAudiobookItem(): Boolean =
    mediaType == MediaType.AUDIO || bookContainers().any { it in AUDIOBOOK_CONTAINERS }

/**
 * Whether this book is in a format Readium can open. Jellyfin reports the container on the item and
 * on its media sources; the file extension is the last fallback. Some servers omit all of them for
 * books, in which case the item is kept and the reader reports an unsupported file.
 */
fun BaseItemDto.isReadableBook(): Boolean {
    val candidates = bookContainers()
    if (isAudiobookItem()) return false
    if (candidates.isEmpty()) return true
    return candidates.any { it in READABLE_BOOK_CONTAINERS }
}

suspend fun FindroidBookDto.toFindroidBook(
    database: ServerDatabaseDao,
    userId: UUID,
): FindroidBook {
    val userData = database.getUserDataOrCreateNew(id, userId)
    return FindroidBook(
        id = id,
        name = name,
        originalTitle = originalTitle,
        overview = overview,
        sources = database.getSources(id).map { it.toFindroidSource(database) },
        played = userData.played,
        favorite = userData.favorite,
        canPlay = true,
        canDownload = false,
        runtimeTicks = 0,
        playbackPositionTicks = userData.playbackPositionTicks,
        images = toLocalFindroidImages(itemId = id),
        chapters = emptyList(),
        isAudiobook = isAudiobook,
        productionYear = productionYear,
        premiereDate = premiereDate,
        genres = emptyList(),
        studios = studios.split("\u0001").filter { it.isNotEmpty() },
        people = emptyList(),
    )
}
