package dev.hdytrfli.onigiri.models

import dev.hdytrfli.onigiri.database.ServerDatabaseDao
import dev.hdytrfli.onigiri.repository.JellyfinRepository
import java.util.UUID
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.MediaType
import org.jellyfin.sdk.model.api.PlayAccess

/**
 * A Jellyfin book rendered in the in-app reader. Only EPUB is supported by the reader; other
 * publication formats are still listed so the detail page can explain that they cannot be opened.
 */
@Serializable
@SerialName("book")
data class FindroidBook(
    @Serializable(with = UuidSerializer::class) override val id: UUID,
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
    @Serializable(with = LocalDateTimeSerializer::class)
    val premiereDate: java.time.LocalDateTime? = null,
    val genres: List<String> = emptyList(),
    val studios: List<String> = emptyList(),
    val people: List<FindroidItemPerson> = emptyList(),
    /** Whether the in-app reader can open this book. */
    val isReadable: Boolean = true,
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
    val localUserData = serverDatabase?.getUserData(id, jellyfinRepository.getUserId())
    // Only pending local changes override the server; synced rows must not mask other clients.
    val pendingUserData = localUserData?.takeIf { it.toBeSynced }
    return FindroidBook(
        id = id,
        name = name.orEmpty(),
        originalTitle = originalTitle,
        overview = overview.orEmpty(),
        sources = sources,
        played = pendingUserData?.played ?: (userData?.played == true),
        favorite = pendingUserData?.favorite ?: (userData?.isFavorite == true),
        canPlay = playAccess != PlayAccess.NONE,
        canDownload = canDownload == true,
        runtimeTicks = 0,
        playbackPositionTicks =
            pendingUserData?.playbackPositionTicks ?: userData?.playbackPositionTicks ?: 0,
        images = toFindroidImages(jellyfinRepository),
        chapters = toFindroidChapters(),
        isAudiobook =
            type == org.jellyfin.sdk.model.api.BaseItemKind.AUDIO_BOOK || isAudiobookItem(),
        productionYear = productionYear,
        premiereDate = premiereDate,
        genres = genres ?: emptyList(),
        studios = studios?.mapNotNull { it.name } ?: emptyList(),
        people = people?.map { it.toFindroidPerson(jellyfinRepository) } ?: emptyList(),
        isReadable = isReadableBook(),
    )
}

/** Publication formats that should still appear in the books list. */
private val LISTABLE_BOOK_CONTAINERS = setOf("epub", "pdf", "cbz", "zip")

/** The only publication format the in-app reader supports. */
private val EPUB_BOOK_CONTAINERS = setOf("epub")

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
 * Whether this book is in a format the in-app reader can open. Jellyfin reports the container on
 * the item and on its media sources; the file extension is the last fallback. Some servers omit all
 * of them for books, in which case the item is kept and the detail page marks it unsupported.
 */
fun BaseItemDto.isReadableBook(): Boolean {
    val candidates = bookContainers()
    if (isAudiobookItem()) return false
    if (candidates.isEmpty()) return true
    return candidates.any { it in EPUB_BOOK_CONTAINERS }
}

/** Whether this is a book the app should list, even if the reader cannot open it. */
fun BaseItemDto.isListableBook(): Boolean {
    val candidates = bookContainers()
    if (isAudiobookItem()) return false
    if (candidates.isEmpty()) return true
    return candidates.any { it in LISTABLE_BOOK_CONTAINERS }
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
        isReadable = !isAudiobook,
    )
}
