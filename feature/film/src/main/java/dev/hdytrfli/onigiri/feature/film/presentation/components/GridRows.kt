package dev.hdytrfli.onigiri.feature.film.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.hdytrfli.onigiri.ui.utils.LocalGridGap

/**
 * A non-scrolling grid: lays [items] out in [columns] equal-width columns, wrapping onto as many
 * rows as needed. Use it for a grid nested inside a scrolling parent, where a `LazyVerticalGrid`
 * would be measured with unbounded height. The cell gap comes from [LocalGridGap] so poster and
 * thumbnail grids stay consistent.
 */
@Composable
fun <T> GridRows(
    items: List<T>,
    columns: Int,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit,
) {
    val gap = LocalGridGap.current
    val safeColumns = columns.coerceAtLeast(1)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(gap)) {
        items.chunked(safeColumns).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(gap),
            ) {
                row.forEach { item -> Box(modifier = Modifier.weight(1f)) { content(item) } }
                repeat(safeColumns - row.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
    }
}
