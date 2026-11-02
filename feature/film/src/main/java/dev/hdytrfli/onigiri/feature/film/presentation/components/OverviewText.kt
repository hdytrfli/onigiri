package dev.hdytrfli.onigiri.feature.film.presentation.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import dev.hdytrfli.onigiri.ui.preview.dummyMovie
import dev.hdytrfli.onigiri.ui.theme.FindroidTheme

@Composable
fun OverviewText(
    text: String,
    modifier: Modifier = Modifier,
    maxCollapsedLines: Int = Int.MAX_VALUE,
) {
    var isExpandable by remember { mutableStateOf(false) }
    var isOverviewExpanded by remember { mutableStateOf(false) }

    Text(
        text = text,
        modifier =
            modifier
                .then(
                    if (isExpandable)
                        Modifier.clickable { isOverviewExpanded = !isOverviewExpanded }
                    else Modifier
                )
                .animateContentSize(),
        overflow = TextOverflow.Ellipsis,
        maxLines = if (isOverviewExpanded) Int.MAX_VALUE else maxCollapsedLines,
        onTextLayout = { textLayoutResult ->
            if (!isOverviewExpanded) {
                isExpandable = textLayoutResult.hasVisualOverflow
            }
        },
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
@Preview(showBackground = true)
private fun OverviewTextPreview() {
    FindroidTheme { OverviewText(text = dummyMovie.overview, maxCollapsedLines = 3) }
}
