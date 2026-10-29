package dev.jdtech.jellyfin.presentation.film

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.core.presentation.theme.Dimens
import dev.jdtech.jellyfin.film.presentation.mediadetail.IdentifyState
import dev.jdtech.jellyfin.film.presentation.mediadetail.IdentifyViewModel
import dev.jdtech.jellyfin.presentation.components.AppButton
import dev.jdtech.jellyfin.presentation.components.PageContent
import dev.jdtech.jellyfin.presentation.components.PageScaffold
import dev.jdtech.jellyfin.presentation.components.PageTopBar
import dev.jdtech.jellyfin.presentation.film.components.OfflineNotice
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.presentation.utils.LocalOfflineMode
import java.util.UUID

/**
 * The Identify search form: the title and the "advanced" switch share a group, and the advanced
 * fields (year and provider IDs) live in their own group. Searching opens the results page.
 */
@Composable
fun IdentifyScreen(
    itemId: UUID,
    navigateBack: () -> Unit,
    onSearch: (query: String, year: String, imdbId: String, tmdbId: String, tvdbId: String) -> Unit,
    viewModel: IdentifyViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    IdentifyScreenLayout(
        state = state,
        onQueryChange = viewModel::setQuery,
        onYearChange = viewModel::setYear,
        onImdbChange = viewModel::setImdbId,
        onTmdbChange = viewModel::setTmdbId,
        onTvdbChange = viewModel::setTvdbId,
        onAdvancedChange = viewModel::setAdvanced,
        onSearch = {
            // Ignore the advanced fields when the switch is off, so a stale value cannot
            // silently narrow the search.
            if (state.advanced) {
                onSearch(state.query, state.year, state.imdbId, state.tmdbId, state.tvdbId)
            } else {
                onSearch(state.query, "", "", "", "")
            }
        },
        navigateBack = navigateBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IdentifyScreenLayout(
    state: IdentifyState,
    onQueryChange: (String) -> Unit,
    onYearChange: (String) -> Unit,
    onImdbChange: (String) -> Unit,
    onTmdbChange: (String) -> Unit,
    onTvdbChange: (String) -> Unit,
    onAdvancedChange: (Boolean) -> Unit,
    onSearch: () -> Unit,
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
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = MaterialTheme.spacings.gutter),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.medium),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                item {
                    IdentifyCard {
                        IdentifyField(
                            label = stringResource(CoreR.string.identify_name),
                            hint = stringResource(CoreR.string.identify_name_hint),
                            icon = CoreR.drawable.ic_film,
                            value = state.query,
                            onValueChange = onQueryChange,
                            modifier = Modifier.padding(horizontal = MaterialTheme.spacings.medium),
                        )
                        // The advanced fields stay in the same group, right below the title, two
                        // per
                        // row.
                        AnimatedVisibility(visible = state.advanced) {
                            Column(
                                verticalArrangement =
                                    Arrangement.spacedBy(MaterialTheme.spacings.medium)
                            ) {
                                AdvancedFieldRow {
                                    IdentifyField(
                                        label = stringResource(CoreR.string.identify_year),
                                        hint =
                                            stringResource(CoreR.string.identify_year_field_hint),
                                        icon = CoreR.drawable.ic_calendar,
                                        value = state.year,
                                        onValueChange = onYearChange,
                                        keyboardType = KeyboardType.Number,
                                        modifier = Modifier.weight(1f),
                                    )
                                    IdentifyField(
                                        label = stringResource(CoreR.string.identify_imdb),
                                        hint = stringResource(CoreR.string.identify_imdb_hint),
                                        icon = CoreR.drawable.ic_video,
                                        value = state.imdbId,
                                        onValueChange = onImdbChange,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                                AdvancedFieldRow {
                                    IdentifyField(
                                        label = stringResource(CoreR.string.identify_tmdb),
                                        hint = stringResource(CoreR.string.identify_tmdb_hint),
                                        icon = CoreR.drawable.ic_video,
                                        value = state.tmdbId,
                                        onValueChange = onTmdbChange,
                                        keyboardType = KeyboardType.Number,
                                        modifier = Modifier.weight(1f),
                                    )
                                    IdentifyField(
                                        label = stringResource(CoreR.string.identify_tvdb),
                                        hint = stringResource(CoreR.string.identify_tvdb_hint),
                                        icon = CoreR.drawable.ic_video,
                                        value = state.tvdbId,
                                        onValueChange = onTvdbChange,
                                        keyboardType = KeyboardType.Number,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        }
                        // Only the advanced toggle is separated, so the fields read as one group.
                        IdentifyDivider()
                        AdvancedRow(
                            checked = state.advanced,
                            onCheckedChange = onAdvancedChange,
                        )
                    }
                }

                item {
                    Box(
                        modifier = Modifier.widthIn(max = Dimens.contentMaxWidth).fillMaxWidth(),
                        contentAlignment = Alignment.CenterEnd,
                    ) {
                        AppButton(onClick = onSearch, enabled = state.query.isNotBlank()) {
                            Text(text = stringResource(CoreR.string.search))
                            Icon(
                                painter = painterResource(CoreR.drawable.ic_arrow_up_right),
                                contentDescription = null,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** A centred, width-capped settings-style card used by the Identify form. */
@Composable
private fun IdentifyCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.widthIn(max = Dimens.contentMaxWidth).fillMaxWidth(),
        colors =
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.padding(vertical = MaterialTheme.spacings.medium),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.medium),
            content = content,
        )
    }
}

/** Matches the divider style used by the subtitle list. */
@Composable
private fun IdentifyDivider() {
    HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.2f))
}

/** A row holding two advanced fields, inset to line up with the title field. */
@Composable
private fun AdvancedFieldRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = MaterialTheme.spacings.medium),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.medium),
        content = content,
    )
}

/** The "Advanced search" switch row, shown in the same group as the title field. */
@Composable
private fun AdvancedRow(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = MaterialTheme.spacings.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(CoreR.string.identify_advanced),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(MaterialTheme.spacings.extraSmall))
            Text(
                text = stringResource(CoreR.string.identify_advanced_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(MaterialTheme.spacings.medium))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/**
 * A labelled field: the label on top, then the filled input with the icon inside it and the hint as
 * a placeholder while it is empty.
 */
@Composable
private fun IdentifyField(
    label: String,
    hint: String,
    icon: Int,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle =
                MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                Row(
                    modifier =
                        Modifier.fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .padding(
                                horizontal = MaterialTheme.spacings.medium,
                                vertical = 12.dp,
                            ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        if (value.isEmpty()) {
                            Text(
                                text = hint,
                                style = MaterialTheme.typography.bodyLarge,
                                color =
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        inner()
                    }
                    Spacer(Modifier.width(MaterialTheme.spacings.small))
                    Icon(
                        painter = painterResource(icon),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
        )
    }
}
