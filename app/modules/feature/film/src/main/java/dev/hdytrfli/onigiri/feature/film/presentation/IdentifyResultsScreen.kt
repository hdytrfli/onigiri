package dev.hdytrfli.onigiri.feature.film.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import dev.hdytrfli.onigiri.core.R as CoreR
import dev.hdytrfli.onigiri.feature.film.presentation.components.OfflineNotice
import dev.hdytrfli.onigiri.feature.film.presentation.mediadetail.IdentifyEvent
import dev.hdytrfli.onigiri.feature.film.presentation.mediadetail.IdentifyState
import dev.hdytrfli.onigiri.feature.film.presentation.mediadetail.IdentifyViewModel
import dev.hdytrfli.onigiri.ui.components.AppButton
import dev.hdytrfli.onigiri.ui.components.AppButtonVariant
import dev.hdytrfli.onigiri.ui.components.AppIconButton
import dev.hdytrfli.onigiri.ui.components.AppLoadingIndicator
import dev.hdytrfli.onigiri.ui.components.BaseDialog
import dev.hdytrfli.onigiri.ui.components.PageContent
import dev.hdytrfli.onigiri.ui.components.PageScaffold
import dev.hdytrfli.onigiri.ui.components.PageTopBar
import dev.hdytrfli.onigiri.ui.theme.spacings
import dev.hdytrfli.onigiri.ui.utils.GridSize
import dev.hdytrfli.onigiri.ui.utils.LocalGridGap
import dev.hdytrfli.onigiri.ui.utils.LocalOfflineMode
import dev.hdytrfli.onigiri.ui.utils.gridColumns
import dev.hdytrfli.onigiri.utils.ObserveAsEvents
import java.util.UUID
import org.jellyfin.sdk.model.api.RemoteSearchResult

/** The Identify results: a poster grid (movie-list style); tapping a poster applies it. */
@Composable
fun IdentifyResultsScreen(
    itemId: UUID,
    query: String,
    year: String,
    imdbId: String,
    tmdbId: String,
    tvdbId: String,
    musicBrainzId: String = "",
    discogsId: String = "",
    series: Boolean = false,
    musicKind: String? = null,
    navigateBack: () -> Unit,
    onApplied: () -> Unit,
    viewModel: IdentifyViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(itemId) {
        val providerIds = buildMap {
            if (musicKind != null) {
                musicBrainzId
                    .trim()
                    .takeIf { it.isNotEmpty() }
                    ?.let {
                        put(
                            if (musicKind == "artist") "MusicBrainzArtist" else "MusicBrainzAlbum",
                            it,
                        )
                    }
                discogsId.trim().takeIf { it.isNotEmpty() }?.let { put("Discogs", it) }
            } else {
                imdbId.trim().takeIf { it.isNotEmpty() }?.let { put("Imdb", it) }
                tmdbId.trim().takeIf { it.isNotEmpty() }?.let { put("Tmdb", it) }
                tvdbId.trim().takeIf { it.isNotEmpty() }?.let { put("Tvdb", it) }
            }
        }
        viewModel.search(
            itemId = itemId,
            query = query.trim(),
            year = year.trim().toIntOrNull(),
            providerIds = providerIds,
            series = series,
            musicKind = musicKind,
        )
    }

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is IdentifyEvent.Applied -> onApplied()
        }
    }

    IdentifyResultsLayout(
        state = state,
        onApply = { result -> viewModel.apply(itemId, result) },
        onInfo = viewModel::showInfo,
        onCloseInfo = viewModel::closeInfo,
        navigateBack = navigateBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IdentifyResultsLayout(
    state: IdentifyState,
    onApply: (RemoteSearchResult) -> Unit,
    onInfo: (RemoteSearchResult) -> Unit,
    onCloseInfo: () -> Unit,
    navigateBack: () -> Unit,
) {
    PageScaffold(
        topBar = {
            PageTopBar(
                title = stringResource(CoreR.string.identify),
                onBack = navigateBack,
            )
        }
    ) { innerPadding ->
        if (LocalOfflineMode.current) {
            OfflineNotice(modifier = Modifier.padding(innerPadding))
            return@PageScaffold
        }
        PageContent(modifier = Modifier.padding(innerPadding)) {
            when {
                state.isSearching ->
                    AppLoadingIndicator(modifier = Modifier.align(Alignment.Center))
                state.error != null ->
                    Text(
                        text = stringResource(CoreR.string.identify_error),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.align(Alignment.Center),
                    )
                state.results.isEmpty() ->
                    Text(
                        text = stringResource(CoreR.string.identify_empty),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.align(Alignment.Center),
                    )
                else ->
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(gridColumns(GridSize.POSTER)),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = MaterialTheme.spacings.gutter),
                        horizontalArrangement = Arrangement.spacedBy(LocalGridGap.current),
                        verticalArrangement = Arrangement.spacedBy(LocalGridGap.current),
                    ) {
                        items(items = state.results) { result ->
                            IdentifyResultCard(
                                result = result,
                                onClick = { onApply(result) },
                                onInfo = { onInfo(result) },
                            )
                        }
                    }
            }
        }
    }

    state.info?.let { info -> IdentifyInfoDialog(info = info, onDismiss = onCloseInfo) }
}

@Composable
private fun IdentifyResultCard(
    result: RemoteSearchResult,
    onClick: () -> Unit,
    onInfo: () -> Unit,
) {
    Column(
        modifier =
            Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small).clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = result.imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier =
                Modifier.fillMaxWidth().aspectRatio(2f / 3f).clip(MaterialTheme.shapes.small),
        )
        Spacer(Modifier.height(MaterialTheme.spacings.small))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = result.name.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = result.productionYear?.toString().orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            AppIconButton(
                onClick = onInfo,
                variant = AppButtonVariant.Ghost,
                content = {
                    Icon(
                        painter = painterResource(CoreR.drawable.ic_info),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
            )
        }
    }
}

@Composable
private fun IdentifyInfoDialog(info: RemoteSearchResult, onDismiss: () -> Unit) {
    BaseDialog(
        title = info.name.orEmpty(),
        onDismiss = onDismiss,
        negativeButton = {},
        positiveButton = {
            AppButton(onClick = onDismiss) { Text(stringResource(CoreR.string.close)) }
        },
    ) { contentPadding ->
        Column(
            modifier =
                Modifier.heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small),
        ) {
            info.productionYear?.let {
                InfoRow(stringResource(CoreR.string.identify_year), it.toString())
            }
            info.searchProviderName?.let {
                InfoRow(stringResource(CoreR.string.identify_source), it)
            }
            info.providerIds
                ?.takeIf { it.isNotEmpty() }
                ?.let { ids ->
                    InfoRow(
                        stringResource(CoreR.string.identify_providers),
                        ids.entries.joinToString(", ") { "${it.key}: ${it.value}" },
                    )
                }
            info.overview
                ?.takeIf { it.isNotBlank() }
                ?.let { InfoRow(stringResource(CoreR.string.identify_overview), it) }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}
