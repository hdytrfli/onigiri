package dev.jdtech.jellyfin.presentation.film.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.jdtech.jellyfin.presentation.theme.spacings

/**
 * A non-scrolling grid: lays [items] out in [columns] equal-width columns, wrapping onto as many
 * rows as needed. Use it for a grid nested inside a scrolling parent, where a `LazyVerticalGrid`
 * would be measured with unbounded height.
 */
@Composable
fun <T> GridRows(
    items: List<T>,
    columns: Int,
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal =
        Arrangement.spacedBy(MaterialTheme.spacings.default),
    verticalArrangement: Arrangement.Vertical =
        Arrangement.spacedBy(MaterialTheme.spacings.default),
    content: @Composable (T) -> Unit,
) {
    val safeColumns = columns.coerceAtLeast(1)
    Column(modifier = modifier, verticalArrangement = verticalArrangement) {
        items.chunked(safeColumns).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = horizontalArrangement,
            ) {
                row.forEach { item -> Box(modifier = Modifier.weight(1f)) { content(item) } }
                repeat(safeColumns - row.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
    }
}
