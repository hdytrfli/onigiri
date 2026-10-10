package dev.jdtech.jellyfin.presentation.film.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.core.presentation.dummy.dummyMovie
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme

@Composable
fun OverviewText(text: String, maxCollapsedLines: Int = Int.MAX_VALUE) {
    var showChevron by remember { mutableStateOf(false) }
    var isOverviewExpanded by remember { mutableStateOf(false) }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = text,
            modifier =
                Modifier.then(
                        if (showChevron)
                            Modifier.clickable { isOverviewExpanded = !isOverviewExpanded }
                        else Modifier
                    )
                    .animateContentSize(),
            overflow = TextOverflow.Ellipsis,
            maxLines = if (isOverviewExpanded) Int.MAX_VALUE else maxCollapsedLines,
            onTextLayout = { textLayoutResult ->
                if (!isOverviewExpanded) {
                    showChevron = textLayoutResult.hasVisualOverflow
                }
            },
            style = MaterialTheme.typography.bodyMedium,
        )
        if (showChevron) {
            Icon(
                painter =
                    painterResource(
                        if (isOverviewExpanded) CoreR.drawable.ic_chevron_up
                        else CoreR.drawable.ic_chevron_down
                    ),
                contentDescription = null,
                modifier =
                    Modifier.size(24.dp).clickable { isOverviewExpanded = !isOverviewExpanded },
            )
        }
    }
}

@Composable
@Preview(showBackground = true)
private fun OverviewTextPreview() {
    FindroidTheme { OverviewText(text = dummyMovie.overview, maxCollapsedLines = 3) }
}
