package dev.jdtech.jellyfin.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.presentation.utils.rememberSafePadding
import dev.jdtech.jellyfin.presentation.utils.statusTopSpacing

/** The top inset a root page's scrollable content needs so it starts below the floating header. */
val LocalRootPageContentInset = compositionLocalOf { 0.dp }

/**
 * The common shell for a root navigation page. The header floats over the content with a
 * transparent background, so posters scroll underneath it; the content receives
 * [LocalRootPageContentInset] for its first-item padding. The header keeps the app's standard
 * safe-area spacing.
 */
@Composable
fun RootPage(
    header: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val safePadding = rememberSafePadding(handleStartInsets = false)
    // The gutter above and below the 48dp floating header keeps the status bar (unless hidden),
    // the header, and the content on one shared outer margin.
    val headerTop = safePadding.statusTopSpacing()
    val contentInset = headerTop + 48.dp + MaterialTheme.spacings.gutter

    CompositionLocalProvider(LocalRootPageContentInset provides contentInset) {
        Box(modifier = modifier.fillMaxSize()) {
            content()
            Box(
                modifier =
                    Modifier.align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .padding(
                            top = headerTop,
                            bottom = MaterialTheme.spacings.gutter,
                        )
            ) {
                header()
            }
        }
    }
}
