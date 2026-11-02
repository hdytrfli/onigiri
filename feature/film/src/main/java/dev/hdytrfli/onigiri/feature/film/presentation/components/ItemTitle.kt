package dev.hdytrfli.onigiri.feature.film.presentation.components

import android.content.res.Configuration
import android.net.Uri
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import dev.hdytrfli.onigiri.ui.utils.LocalTitleImageConfig

/**
 * Shows the item's title logo image when available, falling back to plain text otherwise.
 *
 * The logo takes the configured fraction of the screen width (70% in portrait, 35% in landscape by
 * default), inside a fixed 16:9 box. [logoHeight] can tighten the height further for inline uses.
 */
@Composable
fun ItemTitle(
    title: String,
    logoUri: Uri?,
    modifier: Modifier = Modifier,
    maxLines: Int = 3,
    style: TextStyle = MaterialTheme.typography.headlineMedium,
    logoHeight: Dp = Dp.Unspecified,
) {
    if (logoUri != null) {
        val configuration = LocalConfiguration.current
        val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val titleConfig = LocalTitleImageConfig.current
        val widthFraction =
            if (isLandscape) titleConfig.landscapeWidth else titleConfig.portraitWidth
        val maxWidth = (configuration.screenWidthDp * widthFraction).dp
        // Logos are capped at a fixed 16:9 box.
        val ratioHeight = maxWidth / (16f / 9f)
        val maxHeight =
            if (logoHeight == Dp.Unspecified) ratioHeight else minOf(logoHeight, ratioHeight)
        val context = LocalContext.current
        val model =
            remember(context, logoUri) {
                ImageRequest.Builder(context)
                    .data(logoUri)
                    // Logos are one-off images: served from disk, not the bitmap pool.
                    .memoryCachePolicy(CachePolicy.DISABLED)
                    .build()
            }
        AsyncImage(
            model = model,
            contentDescription = title,
            contentScale = ContentScale.Fit,
            alignment = Alignment.CenterStart,
            // The configured fraction is the actual width, not just a cap: a logo smaller than the
            // cap still grows/shrinks with the setting.
            modifier = modifier.width(maxWidth).height(maxHeight),
        )
    } else {
        Text(
            text = title,
            overflow = TextOverflow.Ellipsis,
            maxLines = maxLines,
            style = style,
            modifier = modifier,
        )
    }
}
