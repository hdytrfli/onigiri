package dev.hdytrfli.onigiri.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Hosts the selected tab's content without a transition. */
@Composable
fun TabContent(
    selectedTab: Int,
    modifier: Modifier = Modifier,
    content: @Composable (Int) -> Unit,
) {
    Box(modifier = modifier) { content(selectedTab) }
}
