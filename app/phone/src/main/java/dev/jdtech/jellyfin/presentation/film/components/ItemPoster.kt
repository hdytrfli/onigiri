package dev.jdtech.jellyfin.presentation.film.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import dev.jdtech.jellyfin.models.FindroidBook
import dev.jdtech.jellyfin.models.FindroidEpisode
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.models.FindroidMovie

enum class Direction {
    HORIZONTAL,
    VERTICAL,
}

@Composable
fun ItemPoster(item: FindroidItem, direction: Direction, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var imageUri = item.images.primary
    var blurHash = item.images.primaryBlurHash
    // Only Primary images are worth a heap slot; backdrops are served from the disk cache.
    var useBackdrop = false

    when (direction) {
        Direction.HORIZONTAL -> {
            if (item is FindroidMovie) {
                imageUri = item.images.backdrop
                blurHash = item.images.backdropBlurHash
                useBackdrop = true
            }
        }
        Direction.VERTICAL -> {
            when (item) {
                is FindroidEpisode -> {
                    imageUri = item.images.showPrimary
                    // Jellyfin does not attach a blur hash for the series primary image.
                    blurHash = null
                }
            }
        }
    }

    // Ugly workaround to append the files directory when loading local images
    if (imageUri?.scheme == null) {
        imageUri =
            Uri.Builder()
                .appendEncodedPath("${context.filesDir}")
                .appendEncodedPath(imageUri?.path)
                .build()
    }

    val isHorizontal = direction == Direction.HORIZONTAL
    val isAudiobook = item is FindroidBook && item.isAudiobook
    val aspectRatio =
        when {
            isAudiobook -> 1f
            isHorizontal -> 1.77f
            else -> 0.66f
        }
    val placeholder =
        rememberBlurHashPainter(
            hash = blurHash,
            width = if (isHorizontal) BLUR_HASH_LANDSCAPE_WIDTH else BLUR_HASH_POSTER_WIDTH,
            height = if (isHorizontal) BLUR_HASH_LANDSCAPE_HEIGHT else BLUR_HASH_POSTER_HEIGHT,
        )
    val model =
        remember(context, imageUri, useBackdrop) {
            ImageRequest.Builder(context)
                .data(imageUri)
                // Only Primary images take a heap slot; the fade duration comes from the loader.
                .memoryCachePolicy(if (useBackdrop) CachePolicy.DISABLED else CachePolicy.ENABLED)
                .build()
        }

    AsyncImage(
        model = model,
        placeholder = placeholder,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier =
            modifier
                .aspectRatio(aspectRatio)
                .background(MaterialTheme.colorScheme.surfaceContainer),
    )
}
