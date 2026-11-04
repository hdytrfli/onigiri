package dev.hdytrfli.onigiri.models

import java.util.UUID
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind

@Serializable
enum class MusicFavoriteKind {
    Song,
    Album,
    Artist,
}

/**
 * A music favorite rendered by the shared favorites page. Music uses its own detail screens, so
 * this keeps the favorite's target ids instead of pretending it is a video item.
 */
@Serializable
@SerialName("musicFavorite")
data class FindroidMusicFavorite(
    @Serializable(with = UuidSerializer::class) override val id: UUID,
    override val name: String,
    val kind: MusicFavoriteKind,
    val subtitle: String? = null,
    @Serializable(with = UuidSerializer::class) val albumId: UUID? = null,
    @Serializable(with = UuidSerializer::class) val artistId: UUID? = null,
    override val originalTitle: String? = null,
    override val overview: String = "",
    override val played: Boolean = false,
    override val favorite: Boolean = true,
    override val canPlay: Boolean = kind == MusicFavoriteKind.Song,
    override val canDownload: Boolean = false,
    override val sources: List<FindroidSource> = emptyList(),
    override val runtimeTicks: Long = 0,
    override val playbackPositionTicks: Long = 0,
    override val unplayedItemCount: Int? = null,
    override val images: FindroidImages,
    override val chapters: List<FindroidChapter> = emptyList(),
) : FindroidItem

fun BaseItemDto.toFindroidMusicFavorite(
    repository: dev.hdytrfli.onigiri.repository.JellyfinRepository
): FindroidMusicFavorite? =
    when (type) {
        BaseItemKind.AUDIO -> {
            val song = toFindroidMusicSong(repository)
            FindroidMusicFavorite(
                id = song.id,
                name = song.name,
                kind = MusicFavoriteKind.Song,
                subtitle = listOfNotNull(song.artistName, song.albumName).joinToString(" · "),
                albumId = song.albumId,
                artistId = song.artistId,
                overview = "",
                runtimeTicks = song.runtimeTicks,
                images = song.images,
            )
        }
        BaseItemKind.MUSIC_ALBUM -> {
            val album = toFindroidMusicAlbum(repository)
            FindroidMusicFavorite(
                id = album.id,
                name = album.name,
                kind = MusicFavoriteKind.Album,
                subtitle =
                    listOfNotNull(album.artistName, album.productionYear?.toString())
                        .joinToString(" · ")
                        .ifBlank { null },
                albumId = album.id,
                artistId = album.artistId,
                overview = album.overview,
                runtimeTicks = album.runtimeTicks,
                images = album.images,
            )
        }
        BaseItemKind.MUSIC_ARTIST -> {
            val artist = toFindroidMusicArtist(repository)
            FindroidMusicFavorite(
                id = artist.id,
                name = artist.name,
                kind = MusicFavoriteKind.Artist,
                artistId = artist.id,
                overview = artist.overview,
                images = artist.images.copy(primary = artist.images.primary ?: artist.images.logo),
            )
        }
        else -> null
    }
