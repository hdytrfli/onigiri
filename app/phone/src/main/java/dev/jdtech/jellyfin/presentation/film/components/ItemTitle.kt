package dev.jdtech.jellyfin.presentation.film.components

import android.content.res.Configuration
import android.net.Uri
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.jdtech.jellyfin.presentation.utils.LocalNaiveTitleOverlay
import dev.jdtech.jellyfin.presentation.utils.LocalTitleImageConfig

/**
 * Shows the item's title logo image when available, falling back to plain text otherwise.
 *
 * The logo uses its intrinsic (auto) width, capped to a reserved area of 70% of the screen width in
 * portrait and 35% in landscape, with a 2:1 (width:height) maximum ratio. [logoHeight] can tighten
 * the height further for inline uses.
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
        val ratioHeight = if (titleConfig.ratio > 0f) maxWidth / titleConfig.ratio else maxWidth
        val maxHeight =
            if (logoHeight == Dp.Unspecified) ratioHeight else minOf(logoHeight, ratioHeight)
        val naiveOverlay = LocalNaiveTitleOverlay.current
        AsyncImage(
            model = logoUri,
            contentDescription = title,
            contentScale = ContentScale.Fit,
            alignment = Alignment.CenterStart,
            colorFilter = if (naiveOverlay) ColorFilter.tint(Color.White) else null,
            modifier = modifier.widthIn(max = maxWidth).heightIn(max = maxHeight),
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
