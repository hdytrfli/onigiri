package dev.hdytrfli.onigiri.feature.film.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import dev.hdytrfli.onigiri.ui.theme.OnScrim

/**
 * The badge drawn over artwork - a poster or a thumbnail - as one treatment for all of them: white
 * content on an opaque black scrim at 70%, so the count and the icons read the same wherever they
 * appear. The content colour is provided rather than set per badge, which is what keeps them
 * uniform.
 */
@Composable
fun BaseBadge(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    CompositionLocalProvider(LocalContentColor provides OnScrim) {
        Box(
            modifier =
                modifier
                    .defaultMinSize(minWidth = 24.dp, minHeight = 24.dp)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.7f))
        ) {
            content()
        }
    }
}
