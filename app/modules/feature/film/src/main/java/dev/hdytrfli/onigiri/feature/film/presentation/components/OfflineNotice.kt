package dev.hdytrfli.onigiri.feature.film.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.hdytrfli.onigiri.LocalOpenDownloads
import dev.hdytrfli.onigiri.core.R as CoreR
import dev.hdytrfli.onigiri.ui.components.AppButton
import dev.hdytrfli.onigiri.ui.components.AppButtonVariant
import dev.hdytrfli.onigiri.ui.theme.spacings
import dev.hdytrfli.onigiri.ui.utils.LocalSetOfflineMode
import dev.hdytrfli.onigiri.ui.utils.rememberSafePadding

/**
 * The full-page offline state, laid out like [FullPageError]: a cloud-off icon, a short explanation
 * and the one action that matters - leaving offline mode. Server-backed pages show it in place of
 * their content while offline mode is on.
 */
@Composable
fun OfflineNotice(modifier: Modifier = Modifier) {
    val safePadding = rememberSafePadding()
    val setOfflineMode = LocalSetOfflineMode.current
    val openDownloads = LocalOpenDownloads.current

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding =
            PaddingValues(
                start = safePadding.start + MaterialTheme.spacings.gutter,
                top = safePadding.top + MaterialTheme.spacings.gutter,
                end = safePadding.end + MaterialTheme.spacings.gutter,
                bottom = safePadding.bottom + MaterialTheme.spacings.gutter,
            ),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    painter = painterResource(CoreR.drawable.ic_cloud_off),
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spacings.medium))
                Text(
                    text = stringResource(CoreR.string.offline_mode_title),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spacings.small))
                Text(
                    text = stringResource(CoreR.string.offline_mode_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spacings.medium))
                Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small)) {
                    AppButton(
                        onClick = { setOfflineMode(false) },
                        content = { Text(stringResource(CoreR.string.go_online)) },
                    )
                    AppButton(
                        onClick = openDownloads,
                        variant = AppButtonVariant.Secondary,
                        content = { Text(stringResource(CoreR.string.title_download)) },
                    )
                }
            }
        }
    }
}
