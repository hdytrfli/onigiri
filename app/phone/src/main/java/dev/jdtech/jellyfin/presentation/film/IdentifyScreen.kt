package dev.jdtech.jellyfin.presentation.film

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.recalculateWindowInsets
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import dev.jdtech.jellyfin.film.presentation.mediadetail.IdentifyViewModel
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.presentation.utils.topBarWindowInsets
import java.util.UUID

/** The Identify search form: title/year/provider IDs, then a search that opens the results page. */
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
        onSearch = { onSearch(state.query, state.year, state.imdbId, state.tmdbId, state.tvdbId) },
        navigateBack = navigateBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IdentifyScreenLayout(
    state: dev.jdtech.jellyfin.film.presentation.mediadetail.IdentifyState,
    onQueryChange: (String) -> Unit,
    onYearChange: (String) -> Unit,
    onImdbChange: (String) -> Unit,
    onTmdbChange: (String) -> Unit,
    onTvdbChange: (String) -> Unit,
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
                Card(
                    modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth(),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ),
                ) {
                    Column(
                        modifier = Modifier.padding(MaterialTheme.spacings.medium),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.medium),
                    ) {
                        IdentifyField(
                            label = stringResource(CoreR.string.identify_name),
                            hint = stringResource(CoreR.string.identify_name_hint),
                            value = state.query,
                            onValueChange = onQueryChange,
                        )
                        IdentifyField(
                            label = stringResource(CoreR.string.identify_year),
                            hint = stringResource(CoreR.string.identify_year_field_hint),
                            value = state.year,
                            onValueChange = onYearChange,
                            keyboardType = KeyboardType.Number,
                        )
                        IdentifyField(
                            label = stringResource(CoreR.string.identify_imdb),
                            hint = stringResource(CoreR.string.identify_imdb_hint),
                            value = state.imdbId,
                            onValueChange = onImdbChange,
                        )
                        IdentifyField(
                            label = stringResource(CoreR.string.identify_tmdb),
                            hint = stringResource(CoreR.string.identify_tmdb_hint),
                            value = state.tmdbId,
                            onValueChange = onTmdbChange,
                            keyboardType = KeyboardType.Number,
                        )
                        IdentifyField(
                            label = stringResource(CoreR.string.identify_tvdb),
                            hint = stringResource(CoreR.string.identify_tvdb_hint),
                            value = state.tvdbId,
                            onValueChange = onTvdbChange,
                            keyboardType = KeyboardType.Number,
                        )
                    }
                }
            }
            item {
                Button(
                    onClick = onSearch,
                    enabled = state.query.isNotBlank(),
                    modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth(),
                ) {
                    Icon(
                        painter = painterResource(CoreR.drawable.ic_search),
                        contentDescription = null,
                    )
                    Spacer(Modifier.width(MaterialTheme.spacings.small))
                    Text(text = stringResource(CoreR.string.search))
                }
            }
        }
    }
}

/** A labelled text field with a hint, using a filled (non-outlined) look. */
@Composable
private fun IdentifyField(
    label: String,
    hint: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(MaterialTheme.spacings.extraSmall))
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
                Box(
                    modifier =
                        Modifier.fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .padding(MaterialTheme.spacings.medium)
                ) {
                    if (value.isEmpty()) {
                        Text(
                            text = hint,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        )
                    }
                    inner()
                }
            },
        )
    }
}
