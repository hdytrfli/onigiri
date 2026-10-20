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
import coil3.request.ImageRequest
import coil3.request.crossfade
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

    when (direction) {
        Direction.HORIZONTAL -> {
            if (item is FindroidMovie) {
                imageUri = item.images.backdrop
                blurHash = item.images.backdropBlurHash
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
    val placeholder =
        rememberBlurHashPainter(
            hash = blurHash,
            width = if (isHorizontal) BLUR_HASH_LANDSCAPE_WIDTH else BLUR_HASH_POSTER_WIDTH,
            height = if (isHorizontal) BLUR_HASH_LANDSCAPE_HEIGHT else BLUR_HASH_POSTER_HEIGHT,
        )
    val model =
        remember(context, imageUri) {
            // No crossfade here: a card scrolling back into view is a cache hit, and fading it in
            // again would read as a reload.
            ImageRequest.Builder(context).data(imageUri).crossfade(false).build()
        }

    AsyncImage(
        model = model,
        placeholder = placeholder,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier =
            modifier
                .aspectRatio(if (isHorizontal) 1.77f else 0.66f)
                .background(MaterialTheme.colorScheme.surfaceContainer),
    )
}
