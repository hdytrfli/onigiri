package dev.hdytrfli.onigiri.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.hdytrfli.onigiri.ui.theme.spacings
import dev.hdytrfli.onigiri.ui.utils.rememberSafePadding

/**
 * The shared back/title/actions bar with an optional expanding search field.
 *
 * Both states occupy the same rail and use the same safe-area insets, so switching to search does
 * not move the bar, introduce a black status-bar gap, or change how the page below is inset.
 */
@Composable
fun SearchablePageTopBar(
    searchExpanded: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onCloseSearch: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    includeTopInset: Boolean = true,
    trailingContent: (@Composable RowScope.() -> Unit)? = null,
) {
    val safePadding = rememberSafePadding()
    AnimatedContent(
        targetState = searchExpanded,
        label = "searchable-page-top-bar",
        modifier = modifier,
    ) { expanded ->
        if (expanded) {
            val topInset =
                if (includeTopInset && !LocalTopBarExternalInset.current) {
                    safePadding.top + MaterialTheme.spacings.gutter
                } else {
                    0.dp
                }
            Box(
                modifier =
                    Modifier.fillMaxWidth()
                        .padding(
                            start = safePadding.start,
                            end = safePadding.end,
                            top = topInset,
                            bottom = MaterialTheme.spacings.gutter,
                        )
            ) {
                PageSearchField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    onClose = onCloseSearch,
                )
            }
        } else {
            PageTopBar(
                title = title,
                onBack = onBack,
                trailingContent = trailingContent,
                includeTopInset = includeTopInset,
            )
        }
    }
}
