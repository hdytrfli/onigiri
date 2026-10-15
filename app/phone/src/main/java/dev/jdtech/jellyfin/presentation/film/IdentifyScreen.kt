package dev.jdtech.jellyfin.presentation.film

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
import androidx.compose.foundation.layout.recalculateWindowInsets
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.film.presentation.mediadetail.IdentifyEvent
import dev.jdtech.jellyfin.film.presentation.mediadetail.IdentifyState
import dev.jdtech.jellyfin.film.presentation.mediadetail.IdentifyViewModel
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.presentation.utils.topBarWindowInsets
import dev.jdtech.jellyfin.utils.ObserveAsEvents
import java.util.UUID
import org.jellyfin.sdk.model.api.RemoteSearchResult

/** Searches remote metadata providers for a movie and applies the chosen result. */
@Composable
fun IdentifyScreen(
    itemId: UUID,
    navigateBack: () -> Unit,
    viewModel: IdentifyViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is IdentifyEvent.Applied -> navigateBack()
        }
    }

    IdentifyScreenLayout(
        state = state,
        onQueryChange = viewModel::setQuery,
        onYearChange = viewModel::setYear,
        onSearch = { viewModel.search(itemId) },
        onApply = { result -> viewModel.apply(itemId, result) },
        onInfo = viewModel::showInfo,
        onCloseInfo = viewModel::closeInfo,
        navigateBack = navigateBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IdentifyScreenLayout(
    state: IdentifyState,
    onQueryChange: (String) -> Unit,
    onYearChange: (String) -> Unit,
    onSearch: () -> Unit,
    onApply: (RemoteSearchResult) -> Unit,
    onInfo: (RemoteSearchResult) -> Unit,
    onCloseInfo: () -> Unit,
    navigateBack: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val contentPadding = PaddingValues(all = MaterialTheme.spacings.default)

    Scaffold(
        modifier =
            Modifier.fillMaxSize()
                .recalculateWindowInsets()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(CoreR.string.identify)) },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(
                            painter = painterResource(CoreR.drawable.ic_arrow_left),
                            contentDescription = null,
                        )
                    }
                },
                windowInsets = topBarWindowInsets(),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxWidth().padding(innerPadding),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.medium),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item {
                Column(modifier = Modifier.widthIn(max = 640.dp)) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors =
                            CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                            ),
                    ) {
                        Column(
                            modifier = Modifier.padding(MaterialTheme.spacings.medium),
                            verticalArrangement =
                                Arrangement.spacedBy(MaterialTheme.spacings.medium),
                        ) {
                            OutlinedTextField(
                                value = state.query,
                                onValueChange = onQueryChange,
                                singleLine = true,
                                label = { Text(stringResource(CoreR.string.identify_search_hint)) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            OutlinedTextField(
                                value = state.year,
                                onValueChange = onYearChange,
                                singleLine = true,
                                label = { Text(stringResource(CoreR.string.identify_year_hint)) },
                                keyboardOptions =
                                    KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Row(
                                modifier =
                                    Modifier.fillMaxWidth()
                                        .clickable(
                                            enabled =
                                                !state.isSearching && state.query.isNotBlank(),
                                            onClick = onSearch,
                                        )
                                        .padding(MaterialTheme.spacings.small),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    painter = painterResource(CoreR.drawable.ic_search),
                                    contentDescription = null,
                                )
                                Spacer(Modifier.size(MaterialTheme.spacings.small))
                                Text(
                                    text = stringResource(CoreR.string.search),
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.weight(1f),
                                )
                                if (state.isSearching) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (state.error != null) {
                item {
                    Text(
                        text = stringResource(CoreR.string.identify_error),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.widthIn(max = 640.dp),
                    )
                }
            } else if (!state.isSearching && state.results.isEmpty() && state.query.isNotBlank()) {
                item {
                    Text(
                        text = stringResource(CoreR.string.identify_empty),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.widthIn(max = 640.dp),
                    )
                }
            }

            state.results.chunked(2).forEach { rowItems ->
                item {
                    Row(
                        modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.medium),
                    ) {
                        rowItems.forEach { result ->
                            IdentifyResultCard(
                                result = result,
                                applying = state.isApplying,
                                onClick = { onApply(result) },
                                onInfo = { onInfo(result) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (rowItems.size == 1) {
                            Spacer(Modifier.weight(1f))
                        }
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
    applying: Boolean,
    onClick: () -> Unit,
    onInfo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.clip(MaterialTheme.shapes.small).clickable(onClick = onClick)) {
        AsyncImage(
            model = result.imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier =
                Modifier.fillMaxWidth()
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(MaterialTheme.spacings.small)),
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
            IconButton(onClick = onInfo) {
                Icon(
                    painter = painterResource(CoreR.drawable.ic_info),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun IdentifyInfoDialog(info: RemoteSearchResult, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = info.name.orEmpty(),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState()),
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
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(CoreR.string.close)) }
        },
    )
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
