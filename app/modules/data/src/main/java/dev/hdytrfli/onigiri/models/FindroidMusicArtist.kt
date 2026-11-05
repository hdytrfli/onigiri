package dev.hdytrfli.onigiri.models

import java.util.UUID
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jellyfin.sdk.model.api.BaseItemDto

@Serializable
@SerialName("musicArtist")
data class FindroidMusicArtist(
    @Serializable(with = UuidSerializer::class) val id: UUID,
    val name: String,
    val overview: String,
    val albumCount: Int?,
    val favorite: Boolean,
    val images: FindroidImages,
)

fun BaseItemDto.toFindroidMusicArtist(
    repository: dev.hdytrfli.onigiri.repository.JellyfinRepository
): FindroidMusicArtist =
    FindroidMusicArtist(
        id = id,
        name = name.orEmpty(),
        overview = overview.orEmpty(),
        albumCount = albumCount ?: childCount,
        favorite = userData?.isFavorite == true,
        images = toFindroidImages(repository),
    )
