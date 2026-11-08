package dev.hdytrfli.onigiri.feature.film.presentation

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.hdytrfli.onigiri.core.R as CoreR
import dev.hdytrfli.onigiri.feature.film.presentation.components.OfflineNotice
import dev.hdytrfli.onigiri.feature.film.presentation.mediadetail.SubtitleSearchState
import dev.hdytrfli.onigiri.feature.film.presentation.mediadetail.SubtitleSearchViewModel
import dev.hdytrfli.onigiri.models.FindroidMediaStream
import dev.hdytrfli.onigiri.settings.R as SettingsR
import dev.hdytrfli.onigiri.ui.components.AppButton
import dev.hdytrfli.onigiri.ui.components.AppButtonVariant
import dev.hdytrfli.onigiri.ui.components.AppIconButton
import dev.hdytrfli.onigiri.ui.components.AppLoadingIndicator
import dev.hdytrfli.onigiri.ui.components.AppPullToRefreshBox
import dev.hdytrfli.onigiri.ui.components.BaseDialog
import dev.hdytrfli.onigiri.ui.components.DefaultSuggestedLanguageCodes
import dev.hdytrfli.onigiri.ui.components.LanguageOption
import dev.hdytrfli.onigiri.ui.components.LanguagePickerDialog
import dev.hdytrfli.onigiri.ui.components.LocalRootPageContentInset
import dev.hdytrfli.onigiri.ui.components.PageContent
import dev.hdytrfli.onigiri.ui.components.PageTopBar
import dev.hdytrfli.onigiri.ui.components.PullToRefreshButton
import dev.hdytrfli.onigiri.ui.components.TopBarPageShell
import dev.hdytrfli.onigiri.ui.theme.Dimens
import dev.hdytrfli.onigiri.ui.theme.spacings
import dev.hdytrfli.onigiri.ui.utils.LocalOfflineMode
import dev.hdytrfli.onigiri.ui.utils.plus
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
    pullToRefreshState: PullToRefreshState = rememberPullToRefreshState(),
) {
    val languageNames = stringArrayResource(SettingsR.array.languages)
    val languageValues = stringArrayResource(SettingsR.array.languages_values)
    var languagePickerOpen by remember { mutableStateOf(false) }
    val languageEntries = languageValues.mapIndexedNotNull { index, code ->
        languageNames.getOrNull(index)?.let { code to it }
    }
    val languageIcon = CoreR.drawable.ic_lucide_languages

    val contentPadding =
        PaddingValues(
            top = LocalRootPageContentInset.current + MaterialTheme.spacings.gutter,
            bottom = MaterialTheme.spacings.gutter,
        )

    TopBarPageShell(
        topBar = {
            PageTopBar(
                title = stringResource(CoreR.string.search_subtitles),
                onBack = navigateBack,
                trailingContent = {
                    PullToRefreshButton(
                        state = pullToRefreshState,
                        isRefreshing = state.isRefreshing,
                        onRefresh = onRefresh,
                    )
                },
            )
        }
    ) {
        if (LocalOfflineMode.current) {
            OfflineNotice(modifier = Modifier.padding(top = LocalRootPageContentInset.current))
            return@TopBarPageShell
        }
        AppPullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize(),
            state = pullToRefreshState,
            showIndicator = false,
        ) {
            PageContent {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = contentPadding,
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.medium),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    item {
                        SettingsSection {
                            LanguageSelectRow(
                                label =
                                    languageLabel(state.language, languageNames, languageValues),
                                onClick = { languagePickerOpen = true },
                            )
                        }
                    }
                    item {
                        Box(
                            modifier =
                                Modifier.widthIn(max = Dimens.contentMaxWidth).fillMaxWidth(),
                            contentAlignment = Alignment.CenterEnd,
                        ) {
                            AppButton(onClick = onSearch, enabled = !state.isSearching) {
                                Text(text = stringResource(CoreR.string.search))
                                Icon(
                                    painter = painterResource(CoreR.drawable.ic_arrow_up_right),
                                    contentDescription = null,
                                )
                            }
                        }
                    }

                    item {
                        SettingsSection(title = stringResource(CoreR.string.existing_subtitles)) {
                            if (state.existing.isEmpty()) {
                                Text(
                                    text = stringResource(CoreR.string.no_existing_subtitles),
                                    modifier = Modifier.padding(MaterialTheme.spacings.medium),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            } else {
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
                            SettingsSection(
                                title = stringResource(CoreR.string.available_subtitles)
                            ) {
                                if (state.isSearching) {
                                    Box(
                                        modifier =
                                            Modifier.fillMaxWidth()
                                                .padding(MaterialTheme.spacings.medium),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        AppLoadingIndicator(modifier = Modifier.size(28.dp))
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
                            Column(modifier = Modifier.widthIn(max = Dimens.contentMaxWidth)) {
                                Text(
                                    text = stringResource(CoreR.string.available_subtitles),
                                    modifier =
                                        Modifier.padding(start = MaterialTheme.spacings.medium),
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
                                        Modifier.padding(
                                            horizontal = MaterialTheme.spacings.medium
                                        ),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (languagePickerOpen) {
        LanguagePickerDialog(
            title = stringResource(CoreR.string.language),
            description = stringResource(CoreR.string.subtitle_search_language_summary),
            options = languageEntries.map { LanguageOption(it.first, it.second) },
            selectedCode = state.language,
            suggestedLabel = stringResource(CoreR.string.suggested),
            availableLabel = stringResource(CoreR.string.available),
            suggestedCodes =
                (state.recentLanguages + DefaultSuggestedLanguageCodes).distinct().take(6),
            optionIcon = languageIcon,
            onSelect = { code ->
                if (code != null) onLanguageChange(code)
                languagePickerOpen = false
            },
            onDismiss = { languagePickerOpen = false },
        )
    }

    state.info?.let { info -> SubtitleInfoDialog(info = info, onDismiss = onCloseInfo) }
}

/** A settings-style card, optionally with a section title above it. */
@Composable
private fun SettingsSection(
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = Modifier.widthIn(max = Dimens.contentMaxWidth)) {
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
private fun SubtitleInfoDialog(info: RemoteSubtitleInfo, onDismiss: () -> Unit) {
    BaseDialog(
        title = stringResource(CoreR.string.subtitle_details),
        onDismiss = onDismiss,
        negativeButton = {},
        positiveButton = {
            AppButton(onClick = onDismiss) { Text(stringResource(CoreR.string.close)) }
        },
    ) { contentPadding ->
        Column(
            modifier =
                Modifier.fillMaxWidth()
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.medium),
        ) {
            info.providerName?.let { InfoRow(stringResource(CoreR.string.subtitle_source), it) }
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
                    if (info.forced == true) stringResource(CoreR.string.subtitle_forced) else null,
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
            val canDelete = stream.isExternal
            // Fixed size so the row keeps the same height whether the button shows, is
            // disabled, or is replaced by the deleting spinner.
            Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                if (deleting && canDelete) {
                    AppLoadingIndicator(modifier = Modifier.size(24.dp))
                } else {
                    // Always shown, so deletable and non-deletable rows have the same height.
                    // When it can't be deleted it's disabled and drawn muted.
                    AppIconButton(
                        onClick = onDelete,
                        enabled = canDelete,
                        variant = AppButtonVariant.Ghost,
                        content = {
                            Icon(
                                painter = painterResource(CoreR.drawable.ic_trash),
                                contentDescription = stringResource(CoreR.string.delete),
                                tint =
                                    if (canDelete) {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                            alpha = 0.38f
                                        )
                                    },
                            )
                        },
                    )
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
            // Fixed size so the row doesn't shift when the button becomes a spinner.
            Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                if (isDownloading) {
                    AppLoadingIndicator(modifier = Modifier.size(24.dp))
                } else {
                    AppIconButton(
                        onClick = onDownload,
                        enabled = result.id != null,
                        variant = AppButtonVariant.Ghost,
                        content = {
                            Icon(
                                painter = painterResource(CoreR.drawable.ic_arrow_down_to_line),
                                contentDescription = stringResource(CoreR.string.download),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                    )
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
