package dev.hdytrfli.onigiri.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import dev.hdytrfli.onigiri.ui.theme.Dimens
import dev.hdytrfli.onigiri.ui.theme.spacings

/**
 * The app's page-content rule, CSS-style: the content fills the page width, inset by the global
 * gutter, and centres itself once it would grow past [maxWidth].
 *
 * The top bar is not part of this box, so it always spans the full screen width; only the page body
 * clamps. Apply this to every secondary page so phone portrait uses the whole width and wide
 * windows stay readable.
 */
@Composable
fun PageContent(
    modifier: Modifier = Modifier,
    maxWidth: Dp = Dimens.contentMaxWidth,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Box(
            modifier =
                Modifier.fillMaxHeight()
                    .widthIn(max = maxWidth)
                    .fillMaxWidth()
                    .padding(horizontal = MaterialTheme.spacings.gutter),
            content = content,
        )
    }
}
