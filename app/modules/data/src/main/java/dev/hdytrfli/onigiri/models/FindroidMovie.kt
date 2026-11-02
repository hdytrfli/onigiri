package dev.hdytrfli.onigiri.models

import dev.hdytrfli.onigiri.database.ServerDatabaseDao
import dev.hdytrfli.onigiri.repository.JellyfinRepository
import java.time.LocalDateTime
import java.util.UUID
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.PlayAccess

@Serializable
@SerialName("movie")
data class FindroidMovie(
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
    @Serializable(with = LocalDateTimeSerializer::class) val premiereDate: LocalDateTime?,
    val people: List<FindroidItemPerson>,
    val genres: List<String>,
    val communityRating: Float?,
    val officialRating: String?,
    val status: String,
    val productionYear: Int?,
    @Serializable(with = LocalDateTimeSerializer::class) val endDate: LocalDateTime?,
    val trailer: String?,
    override val unplayedItemCount: Int? = null,
    override val images: FindroidImages,
    override val chapters: List<FindroidChapter>,
    override val trickplayInfo: Map<String, FindroidTrickplayInfo>?,
) : FindroidItem, FindroidSources

suspend fun BaseItemDto.toFindroidMovie(
    jellyfinRepository: JellyfinRepository,
    serverDatabase: ServerDatabaseDao? = null,
): FindroidMovie {
    val sources = mutableListOf<FindroidSource>()
    sources.addAll(mediaSources?.map { it.toFindroidSource(jellyfinRepository, id) } ?: emptyList())
    if (serverDatabase != null) {
        sources.addAll(serverDatabase.getSources(id).map { it.toFindroidSource(serverDatabase) })
    }
    val localUserData = serverDatabase?.getUserData(id, jellyfinRepository.getUserId())
    // Local user data is only authoritative while it still has changes waiting for the server.
    // Once synced, the server wins so changes made by another client are not hidden by a stale row.
    val pendingUserData = localUserData?.takeIf { it.toBeSynced }
    return FindroidMovie(
        id = id,
        name = name.orEmpty(),
        originalTitle = originalTitle,
        overview = overview.orEmpty(),
        sources = sources,
        played = pendingUserData?.played ?: (userData?.played == true),
        favorite = pendingUserData?.favorite ?: (userData?.isFavorite == true),
        canPlay = playAccess != PlayAccess.NONE,
        canDownload = canDownload == true,
        runtimeTicks = runTimeTicks ?: 0,
        playbackPositionTicks =
            pendingUserData?.playbackPositionTicks ?: userData?.playbackPositionTicks ?: 0,
        premiereDate = premiereDate,
        communityRating = communityRating,
        genres = genres ?: emptyList(),
        people = people?.map { it.toFindroidPerson(jellyfinRepository) } ?: emptyList(),
        officialRating = officialRating,
        status = status ?: "Ended",
        productionYear = productionYear,
        endDate = endDate,
        trailer = remoteTrailers?.getOrNull(0)?.url,
        images = toFindroidImages(jellyfinRepository),
        chapters = toFindroidChapters(),
        trickplayInfo =
            trickplay?.mapValues { it.value[it.value.keys.max()]!!.toFindroidTrickplayInfo() },
    )
}

suspend fun FindroidMovieDto.toFindroidMovie(
    database: ServerDatabaseDao,
    userId: UUID,
): FindroidMovie {
    val userData = database.getUserDataOrCreateNew(id, userId)
    val sources = database.getSources(id).map { it.toFindroidSource(database) }
    val trickplayInfos = mutableMapOf<String, FindroidTrickplayInfo>()
    for (source in sources) {
        database.getTrickplayInfo(source.id)?.toFindroidTrickplayInfo()?.let {
            trickplayInfos[source.id] = it
        }
    }
    return FindroidMovie(
        id = id,
        name = name,
        originalTitle = originalTitle,
        overview = overview,
        played = userData.played,
        favorite = userData.favorite,
        runtimeTicks = runtimeTicks,
        playbackPositionTicks = userData.playbackPositionTicks,
        premiereDate = premiereDate,
        genres = emptyList(),
        people = emptyList(),
        communityRating = communityRating,
        officialRating = officialRating,
        status = status,
        productionYear = productionYear,
        endDate = endDate,
        canDownload = false,
        canPlay = true,
        sources = database.getSources(id).map { it.toFindroidSource(database) },
        trailer = null,
        images = toLocalFindroidImages(itemId = id),
        chapters = chapters ?: emptyList(),
        trickplayInfo = trickplayInfos,
    )
}
