package dev.hdytrfli.onigiri.feature.film.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.hdytrfli.onigiri.LocalOpenDownloads
import dev.hdytrfli.onigiri.core.R as CoreR
import dev.hdytrfli.onigiri.ui.components.AppButton
import dev.hdytrfli.onigiri.ui.components.AppButtonVariant
import dev.hdytrfli.onigiri.ui.theme.spacings

/**
 * The app-wide empty state: one ghost icon, a contextual "No … yet" message, and the same two
 * actions everywhere: refresh, and open downloads.
 */
@Composable
fun EmptyState(
    context: String,
    modifier: Modifier = Modifier,
    onRefresh: (() -> Unit)? = null,
) {
    val openDownloads = LocalOpenDownloads.current
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 320.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                painter = painterResource(CoreR.drawable.ic_ghost),
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(MaterialTheme.spacings.medium))
            Text(
                text = stringResource(CoreR.string.empty_state_title, context),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(MaterialTheme.spacings.small))
            Text(
                text = stringResource(CoreR.string.empty_state_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(MaterialTheme.spacings.medium))
            Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small)) {
                if (onRefresh != null) {
                    AppButton(
                        onClick = onRefresh,
                        content = { Text(stringResource(CoreR.string.refresh)) },
                    )
                }
                AppButton(
                    onClick = openDownloads,
                    variant = AppButtonVariant.Secondary,
                    content = { Text(stringResource(CoreR.string.title_download)) },
                )
            }
        }
    }
}

/**
 * A media grid with the shared empty state drawn on top of it. Keeping the state above the grid
 * means the empty state's buttons receive taps, while the grid underneath still owns the scroll
 * gesture used by pull-to-refresh.
 */
@Composable
fun EmptyStateOverlay(
    visible: Boolean,
    context: String,
    onRefresh: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    title: String? = null,
    titlePadding: PaddingValues = PaddingValues(),
    contentPadding: PaddingValues = PaddingValues(),
    loading: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier = modifier.fillMaxSize()) {
        content()

        if (visible) {
            Column(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
                if (title != null) {
                    SectionHeader(
                        title = title,
                        modifier = Modifier.padding(titlePadding),
                    )
                }
                if (!loading) {
                    EmptyState(
                        context = context,
                        onRefresh = onRefresh,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}
