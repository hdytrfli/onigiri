package dev.hdytrfli.onigiri.models

import java.util.UUID
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jellyfin.sdk.model.api.BaseItemDto

@Serializable
@SerialName("musicAlbum")
data class FindroidMusicAlbum(
    @Serializable(with = UuidSerializer::class) val id: UUID,
    val name: String,
    val overview: String,
    @Serializable(with = UuidSerializer::class) val artistId: UUID?,
    val artistName: String?,
    val productionYear: Int?,
    val songCount: Int?,
    val runtimeTicks: Long,
    val favorite: Boolean,
    val images: FindroidImages,
)

fun BaseItemDto.toFindroidMusicAlbum(
    repository: dev.hdytrfli.onigiri.repository.JellyfinRepository
): FindroidMusicAlbum =
    FindroidMusicAlbum(
        id = id,
        name = name.orEmpty(),
        overview = overview.orEmpty(),
        artistId = albumArtists?.firstOrNull()?.id ?: artistItems?.firstOrNull()?.id,
        artistName =
            albumArtist
                ?: albumArtists?.firstOrNull()?.name
                ?: artistItems?.firstOrNull()?.name
                ?: artists?.joinToString(", "),
        productionYear = productionYear,
        songCount = songCount ?: childCount,
        runtimeTicks = runTimeTicks ?: 0,
        favorite = userData?.isFavorite == true,
        images = toFindroidImages(repository),
    )
