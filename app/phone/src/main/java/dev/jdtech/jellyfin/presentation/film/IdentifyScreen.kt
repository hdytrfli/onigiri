package dev.jdtech.jellyfin.presentation.film

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.film.presentation.mediadetail.IdentifyState
import dev.jdtech.jellyfin.film.presentation.mediadetail.IdentifyViewModel
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.presentation.utils.topBarWindowInsets
import java.util.UUID

/**
 * The Identify search form: the title, then an optional "advanced" group (year and provider IDs)
 * behind a switch. Searching opens the results page.
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
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

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
            contentPadding = PaddingValues(all = MaterialTheme.spacings.default),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.medium),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.medium),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    IdentifyCard {
                        IdentifyField(
                            label = stringResource(CoreR.string.identify_name),
                            hint = stringResource(CoreR.string.identify_name_hint),
                            icon = CoreR.drawable.ic_film,
                            value = state.query,
                            onValueChange = onQueryChange,
                        )
                    }

                    IdentifyCard {
                        Row(
                            modifier =
                                Modifier.fillMaxWidth().padding(MaterialTheme.spacings.medium),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(CoreR.string.identify_advanced),
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Spacer(Modifier.height(MaterialTheme.spacings.extraSmall))
                                Text(
                                    text =
                                        stringResource(CoreR.string.identify_advanced_description),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Spacer(Modifier.width(MaterialTheme.spacings.medium))
                            Switch(checked = state.advanced, onCheckedChange = onAdvancedChange)
                        }
                    }

                    // Last child of the column, so collapsing it adds no stray spacing.
                    AnimatedVisibility(visible = state.advanced) {
                        IdentifyCard {
                            IdentifyField(
                                label = stringResource(CoreR.string.identify_year),
                                hint = stringResource(CoreR.string.identify_year_field_hint),
                                icon = CoreR.drawable.ic_calendar,
                                value = state.year,
                                onValueChange = onYearChange,
                                keyboardType = KeyboardType.Number,
                            )
                            IdentifyDivider()
                            IdentifyField(
                                label = stringResource(CoreR.string.identify_imdb),
                                hint = stringResource(CoreR.string.identify_imdb_hint),
                                icon = CoreR.drawable.ic_video,
                                value = state.imdbId,
                                onValueChange = onImdbChange,
                            )
                            IdentifyDivider()
                            IdentifyField(
                                label = stringResource(CoreR.string.identify_tmdb),
                                hint = stringResource(CoreR.string.identify_tmdb_hint),
                                icon = CoreR.drawable.ic_video,
                                value = state.tmdbId,
                                onValueChange = onTmdbChange,
                                keyboardType = KeyboardType.Number,
                            )
                            IdentifyDivider()
                            IdentifyField(
                                label = stringResource(CoreR.string.identify_tvdb),
                                hint = stringResource(CoreR.string.identify_tvdb_hint),
                                icon = CoreR.drawable.ic_video,
                                value = state.tvdbId,
                                onValueChange = onTvdbChange,
                                keyboardType = KeyboardType.Number,
                            )
                        }
                    }
                }
            }

            item {
                Box(
                    modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth(),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    Button(onClick = onSearch, enabled = state.query.isNotBlank()) {
                        Text(text = stringResource(CoreR.string.search))
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
        modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth(),
        colors =
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(content = content)
    }
}

@Composable
private fun IdentifyDivider() {
    HorizontalDivider(modifier = Modifier.padding(horizontal = MaterialTheme.spacings.medium))
}

/**
 * A labelled field: the label on top, then a row with the icon *outside* the filled input, and the
 * hint underneath.
 */
@Composable
private fun IdentifyField(
    label: String,
    hint: String,
    icon: Int,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(MaterialTheme.spacings.medium),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(MaterialTheme.spacings.medium))
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
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    Box(
                        modifier =
                            Modifier.fillMaxWidth()
                                .clip(MaterialTheme.shapes.small)
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .padding(
                                    horizontal = MaterialTheme.spacings.medium,
                                    vertical = 12.dp,
                                )
                    ) {
                        inner()
                    }
                },
            )
        }
        Text(
            text = hint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        )
    }
}
