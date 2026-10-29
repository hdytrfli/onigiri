package dev.jdtech.jellyfin.presentation.film

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.jdtech.jellyfin.BooksRoute
import dev.jdtech.jellyfin.LocalTabReselect
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.film.presentation.books.BooksViewModel
import dev.jdtech.jellyfin.film.presentation.library.ListDisplayField
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.presentation.components.LocalRootPageContentInset
import dev.jdtech.jellyfin.presentation.components.PageToolbar
import dev.jdtech.jellyfin.presentation.components.RootPage
import dev.jdtech.jellyfin.presentation.film.components.Direction
import dev.jdtech.jellyfin.presentation.film.components.EmptyState
import dev.jdtech.jellyfin.presentation.film.components.FullPageError
import dev.jdtech.jellyfin.presentation.film.components.ItemCard
import dev.jdtech.jellyfin.presentation.film.components.OfflineNotice
import dev.jdtech.jellyfin.presentation.film.components.section
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.presentation.utils.GridSize
import dev.jdtech.jellyfin.presentation.utils.LocalGridGap
import dev.jdtech.jellyfin.presentation.utils.LocalOfflineMode
import dev.jdtech.jellyfin.presentation.utils.gridColumns
import dev.jdtech.jellyfin.presentation.utils.rememberSafePadding
import kotlinx.coroutines.flow.filter

/** The Books root page, one toolbar tab per Books library. */
@Composable
fun BooksScreen(
    onItemClick: (FindroidItem) -> Unit,
    viewModel: BooksViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableStateOf(0) }
    val safePadding = rememberSafePadding()
    val libraryCount = state.libraries.size

    LaunchedEffect(libraryCount) { if (selectedTab >= libraryCount) selectedTab = 0 }

    // Pressing the Books item again cycles through the libraries.
    val tabReselect = LocalTabReselect.current
    LaunchedEffect(tabReselect, libraryCount) {
        tabReselect.events
            .filter { it is BooksRoute }
            .collect { if (libraryCount > 0) selectedTab = (selectedTab + 1) % libraryCount }
    }

    LaunchedEffect(true) { viewModel.load() }

    val selectedLibrary = state.libraries.getOrNull(selectedTab)
    val books = selectedLibrary?.let { state.items[it.id] }.orEmpty()
    val safeSelectedTab = selectedTab.coerceIn(0, (libraryCount - 1).coerceAtLeast(0))

    RootPage(
        header = {
            PageToolbar(
                tabs =
                    if (state.libraries.isEmpty()) {
                        listOf(stringResource(CoreR.string.books_label))
                    } else {
                        state.libraries.map { it.name }
                    },
                tabIcons =
                    if (state.libraries.isEmpty()) {
                        listOf(CoreR.drawable.ic_book_open)
                    } else {
                        state.libraries.map { CoreR.drawable.ic_book_open }
                    },
                selectedIndex = safeSelectedTab,
                onSelect = { selectedTab = it },
                dropdownOnCompact = true,
            )
        }
    ) {
        if (LocalOfflineMode.current) {
            OfflineNotice()
            return@RootPage
        }
        if (state.error != null && books.isEmpty()) {
            FullPageError(
                onRefresh = { viewModel.retry() },
                isRefreshing = state.isLoading,
                error = state.error,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                if (state.isLoading && books.isEmpty()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                if (!state.isLoading && state.libraries.isEmpty()) {
                    EmptyState(text = stringResource(CoreR.string.no_book_libraries))
                }
                if (!state.isLoading && selectedLibrary != null && books.isEmpty()) {
                    EmptyState(text = stringResource(CoreR.string.no_books))
                }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(gridColumns(GridSize.POSTER)),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding =
                        PaddingValues(
                            start = safePadding.start + MaterialTheme.spacings.gutter,
                            top = LocalRootPageContentInset.current,
                            end = safePadding.end + MaterialTheme.spacings.gutter,
                            bottom = safePadding.bottom + MaterialTheme.spacings.gutter,
                        ),
                    horizontalArrangement = Arrangement.spacedBy(LocalGridGap.current),
                    verticalArrangement = Arrangement.spacedBy(LocalGridGap.current),
                ) {
                    if (selectedLibrary != null && books.isNotEmpty()) {
                        section(title = selectedLibrary.name) {
                            items(items = books, key = { it.id }) { item ->
                                ItemCard(
                                    item = item,
                                    direction = Direction.VERTICAL,
                                    onClick = { onItemClick(item) },
                                    modifier = Modifier.animateItem(),
                                    fillWidth = true,
                                    showYear = ListDisplayField.Year in state.displayFields,
                                    showCommunityRating =
                                        ListDisplayField.CommunityRating in state.displayFields,
                                    showAgeRating =
                                        ListDisplayField.AgeRating in state.displayFields,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
