package dev.jdtech.jellyfin.presentation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.jdtech.jellyfin.TAB_TRANSITION_SPEC

/** The shared fade-through used when a pill-tab page switches its content. */
@Composable
fun AnimatedTabContent(
    selectedTab: Int,
    modifier: Modifier = Modifier,
    content: @Composable (Int) -> Unit,
) {
    AnimatedContent(
        targetState = selectedTab,
        modifier = modifier,
        transitionSpec = { TAB_TRANSITION_SPEC },
        label = "tab-content",
    ) { tab ->
        content(tab)
    }
}
