package dev.jdtech.jellyfin.film.presentation.editimages

import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ImageInfo
import org.jellyfin.sdk.model.api.ImageType

/**
 * State for the "Edit images" screen: the images attached to an item and which mutation (if any) is
 * currently in flight. [kind] is used to decide which image types to offer; [baseUrl] is needed to
 * build the preview URLs.
 */
data class EditImagesState(
    val images: List<ImageInfo> = emptyList(),
    val isLoading: Boolean = true,
    val kind: BaseItemKind? = null,
    val baseUrl: String = "",
    val busyKey: String? = null,
    val downloadingType: ImageType? = null,
    val error: Exception? = null,
)

/**
 * Identifies a single image slot (type + index) so the UI can show a progress indicator only on the
 * image that is being changed. A null index (a single-image type, or a new image) maps to -1.
 */
fun imageBusyKey(type: ImageType, index: Int?): String = "${type.name}:${index ?: -1}"

/**
 * The preferred aspect ratio (width / height) for [type], per the Jellyfin spec. Used to size the
 * image tiles so each type is previewed at the shape it is actually displayed at.
 */
fun imageTypeAspectRatio(type: ImageType): Float =
    when (type) {
        ImageType.PRIMARY -> 2f / 3f
        ImageType.BACKDROP,
        ImageType.THUMB,
        ImageType.ART,
        ImageType.MENU,
        ImageType.SCREENSHOT -> 16f / 9f
        ImageType.LOGO -> 800f / 310f
        ImageType.BANNER -> 1000f / 185f
        ImageType.DISC,
        ImageType.BOX,
        ImageType.BOX_REAR,
        ImageType.PROFILE -> 1f
        else -> 16f / 9f
    }
