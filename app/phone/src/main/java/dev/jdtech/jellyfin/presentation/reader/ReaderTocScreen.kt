package dev.jdtech.jellyfin.presentation.reader

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.jdtech.jellyfin.R
import dev.jdtech.jellyfin.core.presentation.theme.Dimens
import dev.jdtech.jellyfin.presentation.components.PageScaffold
import dev.jdtech.jellyfin.presentation.components.PageTopBar
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.presentation.utils.plus
import org.readium.r2.shared.publication.Link

/** One table-of-contents entry, keeping its original Readium link for navigation. */
data class ReaderChapter(
    val title: String,
    val link: Link,
    val depth: Int = 0,
)

fun List<Link>.toReaderChapters(depth: Int = 0): List<ReaderChapter> = flatMap { link ->
    listOf(
        ReaderChapter(
            title = link.title?.takeIf { it.isNotBlank() } ?: link.href.toString(),
            link = link,
            depth = depth,
        )
    ) + link.children.toReaderChapters(depth + 1)
}

/** The reader's full-screen Contents page, matching the Reader settings page. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderTocScreen(
    chapters: List<ReaderChapter>,
    onBack: () -> Unit,
    onChapterClick: (ReaderChapter) -> Unit,
) {
    val contentPadding = PaddingValues(all = MaterialTheme.spacings.gutter)

    PageScaffold(
        topBar = {
            PageTopBar(
                title = stringResource(R.string.reader_contents_title),
                onBack = onBack,
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding + innerPadding,
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.medium),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (chapters.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.reader_contents_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                item {
                    Card(
                        modifier = Modifier.widthIn(max = Dimens.contentMaxWidth),
                        colors =
                            CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                            ),
                    ) {
                        chapters.forEachIndexed { index, chapter ->
                            Column(
                                modifier =
                                    Modifier.fillMaxWidth()
                                        .clickable { onChapterClick(chapter) }
                                        .padding(
                                            start =
                                                MaterialTheme.spacings.medium +
                                                    (chapter.depth * 16).dp,
                                            top = MaterialTheme.spacings.medium,
                                            end = MaterialTheme.spacings.medium,
                                            bottom = MaterialTheme.spacings.medium,
                                        )
                            ) {
                                Text(
                                    text = chapter.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            if (index < chapters.lastIndex) {
                                HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.2f))
                            }
                        }
                    }
                }
            }
        }
    }
}
