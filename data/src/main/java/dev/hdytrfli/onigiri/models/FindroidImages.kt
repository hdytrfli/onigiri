package dev.hdytrfli.onigiri.models

import android.net.Uri
import dev.hdytrfli.onigiri.repository.JellyfinRepository
import java.util.UUID
import kotlinx.serialization.Serializable
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.ImageType

@Serializable
data class FindroidImages(
    @Serializable(with = UriSerializer::class) val primary: Uri? = null,
    @Serializable(with = UriSerializer::class) val backdrop: Uri? = null,
    @Serializable(with = UriSerializer::class) val logo: Uri? = null,
    @Serializable(with = UriSerializer::class) val showPrimary: Uri? = null,
    @Serializable(with = UriSerializer::class) val showBackdrop: Uri? = null,
    @Serializable(with = UriSerializer::class) val showLogo: Uri? = null,
    val primaryBlurHash: String? = null,
    val backdropBlurHash: String? = null,
)

/**
 * Card-sized sources: the server renders at most this wide, so grids stay cheap to fetch/decode.
 */
private const val POSTER_IMAGE_WIDTH = 800
private const val BACKDROP_IMAGE_WIDTH = 1600
private const val LOGO_IMAGE_WIDTH = 600

fun BaseItemDto.toFindroidImages(jellyfinRepository: JellyfinRepository): FindroidImages {
    val baseUrl = Uri.parse(jellyfinRepository.getBaseUrl())
    val primaryTag = imageTags?.get(ImageType.PRIMARY)
    val primary = primaryTag?.let { tag ->
        baseUrl
            .buildUpon()
            .appendEncodedPath("items/$id/Images/${ImageType.PRIMARY}")
            .appendQueryParameter("tag", tag)
            .appendQueryParameter("maxWidth", POSTER_IMAGE_WIDTH.toString())
            .build()
    }
    val backdropTag = backdropImageTags?.firstOrNull()
    val backdrop = backdropTag?.let { tag ->
        baseUrl
            .buildUpon()
            .appendEncodedPath("items/$id/Images/${ImageType.BACKDROP}/0")
            .appendQueryParameter("tag", tag)
            .appendQueryParameter("maxWidth", BACKDROP_IMAGE_WIDTH.toString())
            .build()
    }
    val logo =
        imageTags?.get(ImageType.LOGO)?.let { tag ->
            baseUrl
                .buildUpon()
                .appendEncodedPath("items/$id/Images/${ImageType.LOGO}")
                .appendQueryParameter("tag", tag)
                .appendQueryParameter("maxWidth", LOGO_IMAGE_WIDTH.toString())
                .build()
        }
    val showPrimary = seriesPrimaryImageTag?.let { tag ->
        baseUrl
            .buildUpon()
            .appendEncodedPath("items/$seriesId/Images/${ImageType.PRIMARY}")
            .appendQueryParameter("tag", tag)
            .appendQueryParameter("maxWidth", POSTER_IMAGE_WIDTH.toString())
            .build()
    }
    val showBackdrop = seriesPrimaryImageTag?.let { tag ->
        baseUrl
            .buildUpon()
            .appendEncodedPath("items/$seriesId/Images/${ImageType.BACKDROP}/0")
            .appendQueryParameter("tag", tag)
            .appendQueryParameter("maxWidth", BACKDROP_IMAGE_WIDTH.toString())
            .build()
    }
    val showLogo = seriesPrimaryImageTag?.let { tag ->
        baseUrl
            .buildUpon()
            .appendEncodedPath("items/$seriesId/Images/${ImageType.LOGO}")
            .appendQueryParameter("tag", tag)
            .appendQueryParameter("maxWidth", LOGO_IMAGE_WIDTH.toString())
            .build()
    }
    val primaryBlurHash = primaryTag?.let { imageBlurHashes?.get(ImageType.PRIMARY)?.get(it) }
    val backdropBlurHash = backdropTag?.let { imageBlurHashes?.get(ImageType.BACKDROP)?.get(it) }

    return FindroidImages(
        primary = primary,
        backdrop = backdrop,
        logo = logo,
        showPrimary = showPrimary,
        showBackdrop = showBackdrop,
        showLogo = showLogo,
        primaryBlurHash = primaryBlurHash,
        backdropBlurHash = backdropBlurHash,
    )
}

fun FindroidMovieDto.toLocalFindroidImages(itemId: UUID): FindroidImages {
    return FindroidImages(
        primary = Uri.Builder().appendEncodedPath("images/$itemId/primary").build(),
        backdrop = Uri.Builder().appendEncodedPath("images/$itemId/backdrop").build(),
    )
}

fun FindroidShowDto.toLocalFindroidImages(itemId: UUID): FindroidImages {
    return FindroidImages(
        primary = Uri.Builder().appendEncodedPath("images/$itemId/primary").build(),
        backdrop = Uri.Builder().appendEncodedPath("images/$itemId/backdrop").build(),
    )
}

fun FindroidSeasonDto.toLocalFindroidImages(itemId: UUID): FindroidImages {
    return FindroidImages(
        primary = Uri.Builder().appendEncodedPath("images/$itemId/primary").build(),
        backdrop = Uri.Builder().appendEncodedPath("images/$itemId/backdrop").build(),
        showPrimary = Uri.Builder().appendEncodedPath("images/$seriesId/primary").build(),
        showBackdrop = Uri.Builder().appendEncodedPath("images/$seriesId/backdrop").build(),
    )
}

fun FindroidEpisodeDto.toLocalFindroidImages(itemId: UUID): FindroidImages {
    return FindroidImages(
        primary = Uri.Builder().appendEncodedPath("images/$itemId/primary").build(),
        backdrop = Uri.Builder().appendEncodedPath("images/$itemId/backdrop").build(),
        showPrimary = Uri.Builder().appendEncodedPath("images/$seriesId/primary").build(),
        showBackdrop = Uri.Builder().appendEncodedPath("images/$seriesId/backdrop").build(),
    )
}

fun FindroidBookDto.toLocalFindroidImages(itemId: UUID): FindroidImages {
    return FindroidImages(
        primary = Uri.Builder().appendEncodedPath("images/$itemId/primary").build(),
        backdrop = Uri.Builder().appendEncodedPath("images/$itemId/backdrop").build(),
    )
}

/**
 * Builds the URL for an item image of [type], optionally at [index], with the cache-busting [tag]
 * from [org.jellyfin.sdk.model.api.ImageInfo]. Mirrors the URLs built by [toFindroidImages] so the
 * same Coil ImageLoader (and its authentication) can load them.
 */
fun buildItemImageUrl(
    baseUrl: String,
    itemId: UUID,
    type: ImageType,
    index: Int? = null,
    tag: String? = null,
): Uri {
    val builder = Uri.parse(baseUrl).buildUpon().appendEncodedPath("items/$itemId/Images/$type")
    if (index != null) {
        builder.appendEncodedPath(index.toString())
    }
    if (!tag.isNullOrBlank()) {
        builder.appendQueryParameter("tag", tag)
    }
    return builder.build()
}
