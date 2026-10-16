package dev.jdtech.jellyfin.models

import android.net.Uri
import dev.jdtech.jellyfin.repository.JellyfinRepository
import java.util.UUID
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.ImageType

data class FindroidImages(
    val primary: Uri? = null,
    val backdrop: Uri? = null,
    val logo: Uri? = null,
    val showPrimary: Uri? = null,
    val showBackdrop: Uri? = null,
    val showLogo: Uri? = null,
)

fun BaseItemDto.toFindroidImages(jellyfinRepository: JellyfinRepository): FindroidImages {
    val baseUrl = Uri.parse(jellyfinRepository.getBaseUrl())
    val primary =
        imageTags?.get(ImageType.PRIMARY)?.let { tag ->
            baseUrl
                .buildUpon()
                .appendEncodedPath("items/$id/Images/${ImageType.PRIMARY}")
                .appendQueryParameter("tag", tag)
                .build()
        }
    val backdrop =
        backdropImageTags?.firstOrNull()?.let { tag ->
            baseUrl
                .buildUpon()
                .appendEncodedPath("items/$id/Images/${ImageType.BACKDROP}/0")
                .appendQueryParameter("tag", tag)
                .build()
        }
    val logo =
        imageTags?.get(ImageType.LOGO)?.let { tag ->
            baseUrl
                .buildUpon()
                .appendEncodedPath("items/$id/Images/${ImageType.LOGO}")
                .appendQueryParameter("tag", tag)
                .build()
        }
    val showPrimary = seriesPrimaryImageTag?.let { tag ->
        baseUrl
            .buildUpon()
            .appendEncodedPath("items/$seriesId/Images/${ImageType.PRIMARY}")
            .appendQueryParameter("tag", tag)
            .build()
    }
    val showBackdrop = seriesPrimaryImageTag?.let { tag ->
        baseUrl
            .buildUpon()
            .appendEncodedPath("items/$seriesId/Images/${ImageType.BACKDROP}/0")
            .appendQueryParameter("tag", tag)
            .build()
    }
    val showLogo = seriesPrimaryImageTag?.let { tag ->
        baseUrl
            .buildUpon()
            .appendEncodedPath("items/$seriesId/Images/${ImageType.LOGO}")
            .appendQueryParameter("tag", tag)
            .build()
    }

    return FindroidImages(
        primary = primary,
        backdrop = backdrop,
        logo = logo,
        showPrimary = showPrimary,
        showBackdrop = showBackdrop,
        showLogo = showLogo,
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
