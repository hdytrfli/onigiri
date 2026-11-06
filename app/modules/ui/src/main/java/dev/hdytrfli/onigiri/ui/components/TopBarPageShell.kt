package dev.hdytrfli.onigiri.ui.components

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier

/**
 * The single overlay page shell for every screen with a top bar.
 *
 * The bar always floats above the page. Scrollable content reads [LocalRootPageContentInset] for
 * its first-item padding, so the same header height and safe area apply to every page without each
 * screen owning its own top-bar inset.
 */
@Composable
fun TopBarPageShell(
    topBar: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    CompositionLocalProvider(LocalTopBarExternalInset provides true) {
        RootPage(header = topBar, modifier = modifier) { content() }
    }
}
