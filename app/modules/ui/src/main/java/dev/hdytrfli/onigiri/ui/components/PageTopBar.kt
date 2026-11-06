package dev.hdytrfli.onigiri.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.hdytrfli.onigiri.core.R as CoreR
import dev.hdytrfli.onigiri.ui.theme.spacings
import dev.hdytrfli.onigiri.ui.utils.rememberSafePadding

/**
 * The floating top bar of a secondary page, matching the item detail pages: an optional back icon
 * button and a pill holding the page name, both on the page's gutter. There is no full-width bar -
 * the page background shows through, and the content starts below the same spine.
 *
 * The name uses the toolbar pills' label style, so long names truncate in the pill instead of
 * growing the bar.
 */
@Composable
fun PageTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    trailingContent: (@Composable RowScope.() -> Unit)? = null,
    includeTopInset: Boolean = true,
) {
    val safePadding = rememberSafePadding()
    val includeOwnTopInset = includeTopInset && !LocalTopBarExternalInset.current

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(
                    start = safePadding.start + MaterialTheme.spacings.gutter,
                    top =
                        if (includeOwnTopInset) {
                            safePadding.top + MaterialTheme.spacings.gutter
                        } else {
                            0.dp
                        },
                    end = safePadding.end + MaterialTheme.spacings.gutter,
                    bottom = MaterialTheme.spacings.gutter,
                ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppButtonGroup {
            if (onBack != null) {
                AppIconButton(
                    onClick = onBack,
                    variant = AppButtonVariant.Secondary,
                    content = {
                        Icon(
                            painter = painterResource(CoreR.drawable.ic_arrow_left),
                            contentDescription = null,
                        )
                    },
                )
            }
            Box(
                modifier =
                    Modifier.clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .height(48.dp)
                        .padding(horizontal = MaterialTheme.spacings.medium),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (trailingContent != null) {
            Spacer(Modifier.weight(1f))
            AppButtonGroup(content = trailingContent)
        }
    }
}
