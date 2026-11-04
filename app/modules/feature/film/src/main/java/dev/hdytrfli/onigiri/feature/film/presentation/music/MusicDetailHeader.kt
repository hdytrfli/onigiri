package dev.hdytrfli.onigiri.feature.film.presentation.music

import android.content.res.Configuration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import coil3.compose.AsyncImage
import dev.hdytrfli.onigiri.models.FindroidImages
import dev.hdytrfli.onigiri.ui.theme.ScrimLight
import dev.hdytrfli.onigiri.ui.theme.spacings
import dev.hdytrfli.onigiri.ui.utils.LocalBackdropRatios
import dev.hdytrfli.onigiri.ui.utils.parallaxLayoutModifier
import dev.hdytrfli.onigiri.ui.utils.rememberSafePadding

/**
 * The music detail hero, sharing the same aspect-ratio preference, scrim and safe-area treatment as
 * the movie/show headers. Music items are not [dev.hdytrfli.onigiri.models.FindroidItem]s, so this
 * is the small analogue of ItemHeader for their two detail pages.
 */
@Composable
fun MusicDetailHeader(
    images: FindroidImages,
    lazyListState: LazyListState,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val safePadding = rememberSafePadding()
    val backdropRatios = LocalBackdropRatios.current
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val backgroundColor = MaterialTheme.colorScheme.background
    val image = images.backdrop ?: images.primary

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                // Aspect ratio (width / height), configured in Interface settings.
                .aspectRatio(if (isLandscape) backdropRatios.landscape else backdropRatios.portrait)
                .clipToBounds()
    ) {
        if (image != null) {
            AsyncImage(
                model = image,
                contentDescription = null,
                modifier =
                    Modifier.fillMaxSize()
                        .parallaxLayoutModifier(lazyListState = lazyListState, rate = 2),
                placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceContainer),
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(
                modifier =
                    Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainer)
            )
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(ScrimLight)
            drawRect(
                brush =
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, backgroundColor),
                        startY = 0f,
                    )
            )
        }

        Box(
            modifier =
                Modifier.fillMaxSize()
                    .padding(top = safePadding.top + MaterialTheme.spacings.gutter)
        ) {
            content()
        }
    }
}
