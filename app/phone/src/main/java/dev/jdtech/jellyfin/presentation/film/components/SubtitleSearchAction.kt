package dev.jdtech.jellyfin.presentation.film.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.film.presentation.mediadetail.SubtitleSearchState
import dev.jdtech.jellyfin.film.presentation.mediadetail.SubtitleSearchViewModel
import dev.jdtech.jellyfin.presentation.theme.spacings
import java.util.UUID
import org.jellyfin.sdk.model.api.RemoteSubtitleInfo

/**
 * A subtitle-search button for the detail screens, shown only to admins. It opens a dialog that
 * searches the server's remote subtitle providers (e.g. the OpenSubtitles plugin) and asks the
 * server to download the chosen result.
 */
@Composable
fun SubtitleSearchAction(
    itemId: UUID,
    modifier: Modifier = Modifier,
    viewModel: SubtitleSearchViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var open by remember { mutableStateOf(false) }

    LaunchedEffect(itemId) { viewModel.load() }

    if (state.canManageSubtitles) {
        FilledTonalIconButton(
            onClick = {
                open = true
                viewModel.search(itemId)
            },
            modifier = modifier,
        ) {
            Icon(
                painter = painterResource(CoreR.drawable.ic_closed_caption),
                contentDescription = stringResource(CoreR.string.search_subtitles),
            )
        }
    }

    if (open) {
        SubtitleSearchDialog(
            state = state,
            onRetry = { viewModel.search(itemId) },
            onDownload = { subtitleId -> viewModel.download(itemId, subtitleId) },
            onDismiss = {
                open = false
                viewModel.consumeDownloaded()
            },
        )
    }
}

@Composable
private fun SubtitleSearchDialog(
    state: SubtitleSearchState,
    onRetry: () -> Unit,
    onDownload: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(CoreR.string.search_subtitles)) },
        text = {
            when {
                state.isSearching -> {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                    }
                }
                state.error != null -> Text(stringResource(CoreR.string.search_subtitles_error))
                state.results.isEmpty() -> Text(stringResource(CoreR.string.search_subtitles_empty))
                else -> {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 320.dp),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small),
                    ) {
                        items(
                            items = state.results,
                            key = { it.id ?: it.hashCode().toString() },
                        ) { result ->
                            SubtitleResultRow(
                                result = result,
                                isDownloading = state.downloadingId != null &&
                                    state.downloadingId == result.id,
                                isDownloaded = state.downloadedId != null &&
                                    state.downloadedId == result.id,
                                onDownload = { result.id?.let(onDownload) },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onRetry) { Text(stringResource(CoreR.string.search)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(CoreR.string.close)) }
        },
    )
}

@Composable
private fun SubtitleResultRow(
    result: RemoteSubtitleInfo,
    isDownloading: Boolean,
    isDownloaded: Boolean,
    onDownload: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = result.name.orEmpty(),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
            )
            val details =
                listOfNotNull(
                        result.providerName,
                        result.format,
                        result.downloadCount?.toString(),
                    )
                    .joinToString(" · ")
            if (details.isNotBlank()) {
                Text(
                    text = details,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        when {
            isDownloading -> CircularProgressIndicator(modifier = Modifier.size(24.dp))
            isDownloaded ->
                Text(
                    text = stringResource(CoreR.string.downloaded),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            else ->
                TextButton(onClick = onDownload, enabled = result.id != null) {
                    Text(stringResource(CoreR.string.download))
                }
        }
    }
}
