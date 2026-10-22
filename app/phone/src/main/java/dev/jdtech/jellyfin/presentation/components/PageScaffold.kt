package dev.jdtech.jellyfin.presentation.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.recalculateWindowInsets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import dev.jdtech.jellyfin.presentation.utils.pageContentWindowInsets

/**
 * The Scaffold of every secondary page: a top bar over scrollable content on a table of insets the
 * screen manages itself.
 *
 * Because the pages reset the app-wide consumption of the hidden status bar, the hidden status-bar
 * contract is owned here and cannot be forgotten: the top bar collapses its inset
 * ([topBarWindowInsets]) and the content collapses its inset ([pageContentWindowInsets]), so no
 * screen has to remember the rule again.
 *
 * [scrollBehavior] is optional; screens without a collapsible top bar pass nothing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageScaffold(
    topBar: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    contentWindowInsets: WindowInsets = pageContentWindowInsets(),
    content: @Composable (PaddingValues) -> Unit,
) {
    val scaffoldModifier =
        modifier
            .fillMaxSize()
            .recalculateWindowInsets()
            .then(
                if (scrollBehavior != null) {
                    Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
                } else {
                    Modifier
                }
            )

    Scaffold(
        modifier = scaffoldModifier,
        topBar = { topBar?.invoke() },
        contentWindowInsets = contentWindowInsets,
        content = content,
    )
}
