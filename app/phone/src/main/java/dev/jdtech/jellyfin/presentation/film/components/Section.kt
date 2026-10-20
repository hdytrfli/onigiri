package dev.jdtech.jellyfin.presentation.film.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import dev.jdtech.jellyfin.presentation.utils.LocalGridGap

/**
 * A titled content section. The wrapper owns everything around the content — the title
 * ([SectionHeader] style), the gap between the title and the content, and the optional trailing
 * action — but never the content itself: a horizontal row, a grid, a pager or anything else fills
 * the [content] slot. The grid counterpart [section] lives in this file too, so a title, gap or
 * action change made once applies to every section in the app.
 */
@Composable
fun Section(
    title: String,
    modifier: Modifier = Modifier,
    titleModifier: Modifier = Modifier,
    trailingContent: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier) {
        SectionHeader(title = title, modifier = titleModifier, trailingContent = trailingContent)
        content()
    }
}

/**
 * The grid counterpart of [Section]: emits the title as the first (full-span) item so it scrolls
 * with the content, followed by the items in [content]. A null title skips the title row.
 *
 * Grids put their cell gap after every item, which would push the first row of content further from
 * the title than [Section] does, so the title measures itself short by exactly that gap — the
 * content lands right after the title's fixed 44dp row, the same gap the other sections have.
 */
fun LazyGridScope.section(
    title: String?,
    trailingContent: (@Composable () -> Unit)? = null,
    content: LazyGridScope.() -> Unit,
) {
    if (title != null) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            val gridGap = LocalGridGap.current
            SectionHeader(
                title = title,
                trailingContent = trailingContent,
                modifier =
                    Modifier.layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        layout(
                            placeable.width,
                            (placeable.height - gridGap.roundToPx()).coerceAtLeast(0),
                        ) {
                            placeable.place(0, 0)
                        }
                    },
            )
        }
    }
    content(this)
}
