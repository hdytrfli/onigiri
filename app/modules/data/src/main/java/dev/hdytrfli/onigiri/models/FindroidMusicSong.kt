package dev.hdytrfli.onigiri.models

import java.util.UUID
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.ImageType

@Serializable
@SerialName("musicSong")
data class FindroidMusicSong(
    @Serializable(with = UuidSerializer::class) val id: UUID,
    val name: String,
    @Serializable(with = UuidSerializer::class) val albumId: UUID?,
    val albumName: String?,
    val artistName: String?,
    val indexNumber: Int?,
    val productionYear: Int?,
    val runtimeTicks: Long,
    val favorite: Boolean,
    val images: FindroidImages,
)

fun BaseItemDto.toFindroidMusicSong(
    repository: dev.hdytrfli.onigiri.repository.JellyfinRepository
): FindroidMusicSong {
    val baseImages = toFindroidImages(repository)
    val albumImage =
        if (baseImages.primary == null && albumId != null) {
            albumPrimaryImageTag?.let {
                buildItemImageUrl(
                    repository.getBaseUrl(),
                    albumId!!,
                    ImageType.PRIMARY,
                    tag = it,
                )
            }
        } else {
            null
        }

    return FindroidMusicSong(
        id = id,
        name = name.orEmpty(),
        albumId = albumId,
        albumName = album,
        artistName =
            albumArtist
                ?: albumArtists?.firstOrNull()?.name
                ?: artistItems?.firstOrNull()?.name
                ?: artists?.joinToString(", "),
        indexNumber = indexNumber,
        productionYear = productionYear,
        runtimeTicks = runTimeTicks ?: 0,
        favorite = userData?.isFavorite == true,
        images = baseImages.copy(primary = baseImages.primary ?: albumImage),
    )
}
