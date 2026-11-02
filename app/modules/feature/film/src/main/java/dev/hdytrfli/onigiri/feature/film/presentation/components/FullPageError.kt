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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.hdytrfli.onigiri.core.R as CoreR
import dev.hdytrfli.onigiri.ui.components.AppButton
import dev.hdytrfli.onigiri.ui.components.AppButtonVariant
import dev.hdytrfli.onigiri.ui.components.AppPullToRefreshBox
import dev.hdytrfli.onigiri.ui.components.ErrorDialog
import dev.hdytrfli.onigiri.ui.theme.spacings
import dev.hdytrfli.onigiri.ui.utils.LocalOfflineMode
import dev.hdytrfli.onigiri.ui.utils.rememberSafePadding

/**
 * A full-page error state: a warning icon, a short title, one line of explanation and the actions
 * the user needs (refresh, and details when the exception is available). The page can also be
 * pulled down to retry.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullPageError(
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    isRefreshing: Boolean = false,
    title: String = stringResource(CoreR.string.error_title),
    description: String? = stringResource(CoreR.string.error_try_again),
    error: Throwable? = null,
) {
    val safePadding = rememberSafePadding()
    var showDetails by rememberSaveable { mutableStateOf(false) }

    // Offline mode is not a failure: explain it in the same full-page layout, with its own icon and
    // the action that leaves offline mode.
    if (LocalOfflineMode.current) {
        OfflineNotice(modifier = modifier)
        return
    }

    AppPullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
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
                        painter = painterResource(CoreR.drawable.ic_alert_triangle),
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(MaterialTheme.spacings.medium))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (!description.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(MaterialTheme.spacings.small))
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Spacer(modifier = Modifier.height(MaterialTheme.spacings.medium))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small)
                    ) {
                        AppButton(
                            onClick = onRefresh,
                            content = { Text(stringResource(CoreR.string.refresh)) },
                        )
                        if (error != null) {
                            AppButton(
                                onClick = { showDetails = true },
                                variant = AppButtonVariant.Secondary,
                                content = { Text(stringResource(CoreR.string.details)) },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDetails && error != null) {
        ErrorDialog(exception = error, onDismissRequest = { showDetails = false })
    }
}
