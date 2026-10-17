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
 * The preferred aspect ratio (width / height) for [type] in the editor. The poster (Primary) and
 * square (Disc, Box) shapes are kept; everything else is shown at 16:9. An episode's Primary image
 * is a 16:9 still rather than a poster, so [isEpisode] flips that one case.
 */
fun imageTypeAspectRatio(type: ImageType, isEpisode: Boolean = false): Float =
    if (type == ImageType.PRIMARY) {
        if (isEpisode) 16f / 9f else 2f / 3f
    } else {
        when (type) {
            ImageType.DISC,
            ImageType.BOX -> 1f
            else -> 16f / 9f
        }
    }
