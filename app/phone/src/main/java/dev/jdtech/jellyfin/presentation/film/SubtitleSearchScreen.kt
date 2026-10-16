package dev.jdtech.jellyfin.presentation.film

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.recalculateWindowInsets
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.film.presentation.mediadetail.SubtitleSearchState
import dev.jdtech.jellyfin.film.presentation.mediadetail.SubtitleSearchViewModel
import dev.jdtech.jellyfin.models.FindroidMediaStream
import dev.jdtech.jellyfin.presentation.components.BaseDialog
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.presentation.utils.plus
import dev.jdtech.jellyfin.presentation.utils.topBarWindowInsets
import dev.jdtech.jellyfin.settings.R as SettingsR
import java.util.UUID
import org.jellyfin.sdk.model.api.RemoteSubtitleInfo

/**
 * Remote-subtitle management for an item: pick a search language, search the server's providers,
 * download or preview results, and delete subtitles that are already attached. Styled like the
 * settings pages (centred, width-capped cards).
 */
@Composable
fun SubtitleSearchScreen(
    itemId: UUID,
    navigateBack: () -> Unit,
    viewModel: SubtitleSearchViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(itemId) { viewModel.load(itemId) }

    SubtitleSearchScreenLayout(
        state = state,
        onLanguageChange = viewModel::setLanguage,
        onSearch = { viewModel.search(itemId) },
        onRefresh = { viewModel.refresh(itemId) },
        onDownload = { subtitleId -> viewModel.download(itemId, subtitleId) },
        onDelete = { index -> viewModel.delete(itemId, index) },
        onInfo = viewModel::showInfo,
        onCloseInfo = viewModel::closeInfo,
        navigateBack = navigateBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubtitleSearchScreenLayout(
    state: SubtitleSearchState,
    onLanguageChange: (String) -> Unit,
    onSearch: () -> Unit,
    onRefresh: () -> Unit,
    onDownload: (String) -> Unit,
    onDelete: (Int) -> Unit,
    onInfo: (RemoteSubtitleInfo) -> Unit,
    onCloseInfo: () -> Unit,
    navigateBack: () -> Unit,
) {
    val languageNames = stringArrayResource(SettingsR.array.languages)
    val languageValues = stringArrayResource(SettingsR.array.languages_values)
    var languagePickerOpen by remember { mutableStateOf(false) }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val contentPadding = PaddingValues(all = MaterialTheme.spacings.default)

    Scaffold(
        modifier =
            Modifier.fillMaxSize()
                .recalculateWindowInsets()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(CoreR.string.search_subtitles)) },
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
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize().padding(innerPadding),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = contentPadding,
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.medium),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                item {
                    SettingsSection {
                        LanguageSelectRow(
                            label = languageLabel(state.language, languageNames, languageValues),
                            onClick = { languagePickerOpen = true },
                        )
                    }
                }
                item {
                    Box(
                        modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth(),
                        contentAlignment = Alignment.CenterEnd,
                    ) {
                        Button(onClick = onSearch, enabled = !state.isSearching) {
                            Icon(
                                painter = painterResource(CoreR.drawable.ic_search),
                                contentDescription = null,
                            )
                            Spacer(modifier = Modifier.width(MaterialTheme.spacings.small))
                            Text(text = stringResource(CoreR.string.search))
                        }
                    }
                }

                if (state.existing.isNotEmpty()) {
                    item {
                        SettingsSection(title = stringResource(CoreR.string.existing_subtitles)) {
                            state.existing.forEachIndexed { index, stream ->
                                ExistingSubtitleRow(
                                    stream = stream,
                                    deleting = state.deletingIndex == stream.index,
                                    onDelete = { onDelete(stream.index) },
                                )
                                if (index < state.existing.lastIndex) {
                                    HorizontalDivider(
                                        color = DividerDefaults.color.copy(alpha = 0.2f)
                                    )
                                }
                            }
                        }
                    }
                }

                if (state.isSearching || state.results.isNotEmpty()) {
                    item {
                        SettingsSection(title = stringResource(CoreR.string.available_subtitles)) {
                            if (state.isSearching) {
                                Box(
                                    modifier =
                                        Modifier.fillMaxWidth()
                                            .padding(MaterialTheme.spacings.medium),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(28.dp))
                                }
                            } else {
                                state.results.forEachIndexed { index, result ->
                                    RemoteSubtitleRow(
                                        result = result,
                                        isDownloading =
                                            state.downloadingId != null &&
                                                state.downloadingId == result.id,
                                        onDownload = { result.id?.let(onDownload) },
                                        onInfo = { onInfo(result) },
                                    )
                                    if (index < state.results.lastIndex) {
                                        HorizontalDivider(
                                            color = DividerDefaults.color.copy(alpha = 0.2f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Plain empty/error state: no card, so it doesn't look like a list item.
                    item {
                        Column(modifier = Modifier.widthIn(max = 640.dp)) {
                            Text(
                                text = stringResource(CoreR.string.available_subtitles),
                                modifier = Modifier.padding(start = MaterialTheme.spacings.medium),
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Spacer(modifier = Modifier.height(MaterialTheme.spacings.small))
                            Text(
                                text =
                                    if (state.error != null)
                                        stringResource(CoreR.string.search_subtitles_error)
                                    else stringResource(CoreR.string.search_subtitles_empty),
                                color =
                                    if (state.error != null) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier =
                                    Modifier.padding(horizontal = MaterialTheme.spacings.medium),
                            )
                        }
                    }
                }
            }
        }
    }

    if (languagePickerOpen) {
        BaseDialog(
            title = stringResource(CoreR.string.language),
            onDismiss = { languagePickerOpen = false },
        ) { dialogPadding ->
            val listState = rememberLazyListState()
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                state = listState,
                contentPadding = dialogPadding,
            ) {
                itemsIndexed(languageNames.toList()) { index, name ->
                    LanguageDialogItem(
                        name = name,
                        selected = languageValues.getOrNull(index) == state.language,
                        onSelect = {
                            languageValues.getOrNull(index)?.let(onLanguageChange)
                            languagePickerOpen = false
                        },
                    )
                }
            }
        }
    }

    state.info?.let { info -> SubtitleInfoDialog(info = info, onDismiss = onCloseInfo) }
}

/** A settings-style card, optionally with a section title above it. */
@Composable
private fun SettingsSection(
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = Modifier.widthIn(max = 640.dp)) {
        if (title != null) {
            Text(
                text = title,
                modifier = Modifier.padding(start = MaterialTheme.spacings.medium),
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacings.small))
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
            content = content,
        )
    }
}

@Composable
private fun LanguageSelectRow(label: String, onClick: () -> Unit) {
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(MaterialTheme.spacings.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painter = painterResource(SettingsR.drawable.ic_languages), contentDescription = null)
        Spacer(modifier = Modifier.width(MaterialTheme.spacings.default))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(CoreR.string.language),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacings.extraSmall))
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ActionRow(
    iconRes: Int,
    title: String,
    onClick: () -> Unit,
    enabled: Boolean,
    trailing: @Composable () -> Unit = {},
) {
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .clickable(enabled = enabled, onClick = onClick)
                .padding(MaterialTheme.spacings.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painter = painterResource(iconRes), contentDescription = null)
        Spacer(modifier = Modifier.width(MaterialTheme.spacings.default))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        trailing()
    }
}

@Composable
private fun LanguageDialogItem(name: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .clickable(onClick = onSelect)
                .padding(horizontal = MaterialTheme.spacings.default),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Spacer(modifier = Modifier.width(MaterialTheme.spacings.medium))
        Text(text = name, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun SubtitleInfoDialog(info: RemoteSubtitleInfo, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.width(480.dp).height(440.dp),
            shape = RoundedCornerShape(28.dp),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = stringResource(CoreR.string.subtitle_details),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(MaterialTheme.spacings.default),
                )
                HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.2f))
                Column(
                    modifier =
                        Modifier.fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(MaterialTheme.spacings.default),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.medium),
                ) {
                    info.providerName?.let {
                        InfoRow(stringResource(CoreR.string.subtitle_source), it)
                    }
                    info.threeLetterIsoLanguageName?.let {
                        InfoRow(stringResource(CoreR.string.language), it)
                    }
                    info.format?.let { InfoRow(stringResource(CoreR.string.subtitle_format), it) }
                    info.author?.let { InfoRow(stringResource(CoreR.string.subtitle_author), it) }
                    info.downloadCount?.let {
                        InfoRow(stringResource(CoreR.string.subtitle_downloads), it.toString())
                    }
                    info.communityRating?.let {
                        InfoRow(stringResource(CoreR.string.subtitle_rating), "%.1f".format(it))
                    }
                    info.frameRate?.let {
                        InfoRow(stringResource(CoreR.string.subtitle_frame_rate), it.toString())
                    }
                    val flags =
                        listOfNotNull(
                            if (info.hearingImpaired == true)
                                stringResource(CoreR.string.subtitle_hearing_impaired)
                            else null,
                            if (info.forced == true) stringResource(CoreR.string.subtitle_forced)
                            else null,
                            if (info.aiTranslated == true)
                                stringResource(CoreR.string.subtitle_ai_translated)
                            else null,
                            if (info.machineTranslated == true)
                                stringResource(CoreR.string.subtitle_machine_translated)
                            else null,
                        )
                    if (flags.isNotEmpty()) {
                        InfoRow(
                            stringResource(CoreR.string.subtitle_details),
                            flags.joinToString(" · "),
                        )
                    }
                    info.comment
                        ?.takeIf { it.isNotBlank() }
                        ?.let { InfoRow(stringResource(CoreR.string.subtitle_comment), it) }
                }
                HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.2f))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(MaterialTheme.spacings.small),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) { Text(stringResource(CoreR.string.close)) }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.medium),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(140.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ExistingSubtitleRow(
    stream: FindroidMediaStream,
    deleting: Boolean,
    onDelete: () -> Unit,
) {
    val title =
        stream.displayTitle?.takeIf { it.isNotBlank() }
            ?: stream.title.takeIf { it.isNotBlank() }
            ?: stream.language
    SubtitleRow(title = title, metadata = null) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Only external subtitles can be deleted from the server.
            if (stream.isExternal) {
                if (deleting) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    IconButton(onClick = onDelete) {
                        Icon(
                            painter = painterResource(CoreR.drawable.ic_trash),
                            contentDescription = stringResource(CoreR.string.delete),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RemoteSubtitleRow(
    result: RemoteSubtitleInfo,
    isDownloading: Boolean,
    onDownload: () -> Unit,
    onInfo: () -> Unit,
) {
    SubtitleRow(title = result.name.orEmpty(), metadata = null) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onInfo) {
                Icon(
                    painter = painterResource(CoreR.drawable.ic_info),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            // Fixed size so the row doesn't shift when the button becomes a spinner.
            Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                if (isDownloading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    IconButton(onClick = onDownload, enabled = result.id != null) {
                        Icon(
                            painter = painterResource(CoreR.drawable.ic_arrow_down_to_line),
                            contentDescription = stringResource(CoreR.string.download),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SubtitleRow(
    title: String,
    metadata: String?,
    trailing: @Composable () -> Unit,
) {
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .padding(
                    horizontal = MaterialTheme.spacings.medium,
                    vertical = MaterialTheme.spacings.small,
                ),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(32.dp), contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(CoreR.drawable.ic_case_sensitive),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge,
            )
            if (!metadata.isNullOrBlank()) {
                Text(
                    text = metadata,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        trailing()
    }
}

/**
 * Maps an ISO 639-2 code (e.g. "eng") to its display name, falling back to the upper-cased code.
 */
private fun languageLabel(code: String, names: Array<String>, values: Array<String>): String {
    val index = values.indexOf(code)
    return names.getOrNull(index) ?: code.uppercase()
}
