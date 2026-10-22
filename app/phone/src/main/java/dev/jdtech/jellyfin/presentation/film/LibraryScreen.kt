package dev.jdtech.jellyfin.presentation.film

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.core.presentation.dummy.dummyMovies
import dev.jdtech.jellyfin.film.presentation.library.LibraryAction
import dev.jdtech.jellyfin.film.presentation.library.LibraryState
import dev.jdtech.jellyfin.film.presentation.library.LibraryViewModel
import dev.jdtech.jellyfin.film.presentation.library.ListDisplayField
import dev.jdtech.jellyfin.models.CollectionType
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.presentation.components.AppButton
import dev.jdtech.jellyfin.presentation.components.AppButtonSize
import dev.jdtech.jellyfin.presentation.components.AppButtonVariant
import dev.jdtech.jellyfin.presentation.components.AppIconButton
import dev.jdtech.jellyfin.presentation.components.AppOptionRow
import dev.jdtech.jellyfin.presentation.components.AppTextField
import dev.jdtech.jellyfin.presentation.components.BaseDialog
import dev.jdtech.jellyfin.presentation.components.LocalRootPageContentInset
import dev.jdtech.jellyfin.presentation.components.PageScaffold
import dev.jdtech.jellyfin.presentation.film.components.Direction
import dev.jdtech.jellyfin.presentation.film.components.FullPageError
import dev.jdtech.jellyfin.presentation.film.components.ItemCard
import dev.jdtech.jellyfin.presentation.film.components.ItemCountBadge
import dev.jdtech.jellyfin.presentation.film.components.SortByDialog
import dev.jdtech.jellyfin.presentation.film.components.section
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.presentation.utils.GridSize
import dev.jdtech.jellyfin.presentation.utils.LocalGridGap
import dev.jdtech.jellyfin.presentation.utils.gridColumns
import dev.jdtech.jellyfin.presentation.utils.pageContentWindowInsets
import dev.jdtech.jellyfin.presentation.utils.plus
import dev.jdtech.jellyfin.presentation.utils.rememberSafePadding
import dev.jdtech.jellyfin.presentation.utils.topBarWindowInsets
import java.util.UUID

@Composable
fun LibraryScreen(
    libraryId: UUID?,
    libraryName: String?,
    libraryType: CollectionType,
    onItemClick: (item: FindroidItem) -> Unit,
    navigateBack: () -> Unit,
    showTopBar: Boolean = true,
    embedded: Boolean = false,
    genreIds: List<UUID> = emptyList(),
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(true) {
        viewModel.setup(parentId = libraryId, libraryType = libraryType, genreIds = genreIds)
        // The ViewModel owns the list cache, so a revisit renders it instead of reloading.
        viewModel.loadItemsIfNeeded()
    }

    LibraryScreenLayout(
        libraryName = libraryName,
        showTopBar = showTopBar,
        embedded = embedded,
        state = state,
        onAction = { action ->
            when (action) {
                is LibraryAction.OnItemClick -> onItemClick(action.item)
                is LibraryAction.OnBackClick -> navigateBack()
                else -> Unit
            }
            viewModel.onAction(action)
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryScreenLayout(
    libraryName: String?,
    showTopBar: Boolean,
    embedded: Boolean,
    state: LibraryState,
    onAction: (LibraryAction) -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    var showSortByDialog by remember { mutableStateOf(false) }
    var showDisplayDialog by remember { mutableStateOf(false) }

    PageScaffold(
        scrollBehavior = scrollBehavior,
        contentWindowInsets =
            if (embedded) {
                WindowInsets(0, 0, 0, 0)
            } else {
                pageContentWindowInsets()
            },
        topBar = {
            if (showTopBar) {
                TopAppBar(
                    title = { Text(libraryName.orEmpty()) },
                    navigationIcon = {
                        AppIconButton(
                            onClick = { onAction(LibraryAction.OnBackClick) },
                            variant = AppButtonVariant.Ghost,
                            content = {
                                Icon(
                                    painter = painterResource(CoreR.drawable.ic_arrow_left),
                                    contentDescription = null,
                                )
                            },
                        )
                    },
                    actions = {
                        LibraryActionButtons(
                            onSearch = { onAction(LibraryAction.ToggleSearch) },
                            onRefresh = { onAction(LibraryAction.RefreshItems) },
                            onInfo = { showDisplayDialog = true },
                            onSort = { showSortByDialog = true },
                        )
                    },
                    windowInsets = topBarWindowInsets(),
                    scrollBehavior = scrollBehavior,
                )
            }
        },
    ) { innerPadding ->
        LibraryContent(
            state = state,
            onAction = onAction,
            showSortDialog = showSortByDialog,
            showDisplayDialog = showDisplayDialog,
            onShowSortDialogChange = { showSortByDialog = it },
            onShowDisplayDialogChange = { showDisplayDialog = it },
            embedded = embedded,
            innerPadding = innerPadding,
        )
    }
}

/** The grid, its search field and its dialogs, without a top bar or window insets. */
@Composable
fun LibraryContent(
    state: LibraryState,
    onAction: (LibraryAction) -> Unit,
    showSortDialog: Boolean,
    showDisplayDialog: Boolean,
    onShowSortDialogChange: (Boolean) -> Unit,
    onShowDisplayDialogChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    embedded: Boolean = false,
    innerPadding: PaddingValues = PaddingValues(0.dp),
    sectionTitle: String? = null,
    showSearchField: Boolean = true,
) {
    val safePadding = rememberSafePadding(handleStartInsets = false)
    // Under a root page the floating header overlays the content, so the first grid item
    // starts below it; standalone the grid keeps its own spacing.
    val rootInset = LocalRootPageContentInset.current
    val contentPadding =
        if (embedded) {
            PaddingValues(
                start = safePadding.start + MaterialTheme.spacings.default,
                top =
                    rootInset +
                        if (sectionTitle != null) {
                            0.dp
                        } else {
                            MaterialTheme.spacings.default
                        },
                end = safePadding.end + MaterialTheme.spacings.default,
                bottom = safePadding.bottom + MaterialTheme.spacings.default,
            )
        } else {
            PaddingValues(all = MaterialTheme.spacings.default)
        }

    val items = state.items
    // The badge on the section title shows the cached list's total.
    val itemCount = items.size.takeIf { it > 0 }

    // Do not refresh the paged list on resume: reloading it resets the scroll position. Only the
    // item whose detail page was opened can have changed, so the ViewModel re-fetches just that.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { onAction(LibraryAction.RefreshOpenedItem) }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(state.searchExpanded) {
        if (showSearchField && state.searchExpanded) {
            focusRequester.requestFocus()
        }
    }

    Column(modifier = modifier.fillMaxSize().padding(innerPadding)) {
        if (showSearchField && state.searchExpanded) {
            AppTextField(
                value = state.searchQuery,
                onValueChange = { onAction(LibraryAction.SetSearchQuery(it)) },
                modifier =
                    Modifier.fillMaxWidth()
                        .padding(
                            start = MaterialTheme.spacings.default,
                            top = MaterialTheme.spacings.small,
                            end = MaterialTheme.spacings.default,
                        )
                        .focusRequester(focusRequester),
                hint = stringResource(CoreR.string.search),
                leadingContent = {
                    Icon(
                        painter = painterResource(CoreR.drawable.ic_search),
                        contentDescription = null,
                    )
                },
                trailingContent = {
                    // Keep the clear button's footprint even while the field is empty, otherwise
                    // the text area jumps sideways as soon as the first character is typed.
                    Box(
                        modifier = Modifier.size(32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (state.searchQuery.isNotEmpty()) {
                            AppIconButton(
                                onClick = { onAction(LibraryAction.SetSearchQuery("")) },
                                size = AppButtonSize.Decorator,
                                variant = AppButtonVariant.Ghost,
                                content = {
                                    Icon(
                                        painter = painterResource(CoreR.drawable.ic_x),
                                        contentDescription = null,
                                    )
                                },
                            )
                        }
                    }
                },
            )
        }
        val error = state.error
        if (error != null && items.isEmpty()) {
            FullPageError(
                onRefresh = { onAction(LibraryAction.RefreshItems) },
                isRefreshing = state.isLoading,
                error = error,
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(gridColumns(GridSize.POSTER)),
                modifier = Modifier.fillMaxSize(),
                contentPadding = contentPadding,
                horizontalArrangement = Arrangement.spacedBy(LocalGridGap.current),
                verticalArrangement = Arrangement.spacedBy(LocalGridGap.current),
            ) {
                // The title is content, not a header: it scrolls away with the grid. The badge
                // on its right shows the cached list's total.
                section(
                    title = sectionTitle,
                    trailingContent =
                        itemCount?.let { count ->
                            {
                                ItemCountBadge(
                                    count = count,
                                    modifier = Modifier.padding(start = 8.dp),
                                )
                            }
                        },
                ) {
                    items(items = items, key = { it.id }) { item ->
                        ItemCard(
                            item = item,
                            direction = Direction.VERTICAL,
                            onClick = { onAction(LibraryAction.OnItemClick(item)) },
                            modifier = Modifier.animateItem(),
                            fillWidth = true,
                            showYear = ListDisplayField.Year in state.displayFields,
                            showCommunityRating =
                                ListDisplayField.CommunityRating in state.displayFields,
                            showAgeRating = ListDisplayField.AgeRating in state.displayFields,
                        )
                    }
                }
            }
        }
    }

    if (showSortDialog) {
        SortByDialog(
            currentSortBy = state.sortBy,
            currentSortOrder = state.sortOrder,
            onUpdate = { sortBy, sortOrder ->
                onAction(LibraryAction.ChangeSorting(sortBy, sortOrder))
            },
            onDismissRequest = { onShowSortDialogChange(false) },
        )
    }

    if (showDisplayDialog) {
        DisplayInfoDialog(
            fields = state.displayFields,
            onToggle = { onAction(LibraryAction.ToggleDisplayField(it)) },
            onDismiss = { onShowDisplayDialogChange(false) },
        )
    }
}

@Composable
private fun LibraryActionButtons(
    onSearch: () -> Unit,
    onRefresh: () -> Unit,
    onInfo: () -> Unit,
    onSort: () -> Unit,
) {
    AppIconButton(
        onClick = onSearch,
        variant = AppButtonVariant.Ghost,
        content = {
            Icon(
                painter = painterResource(CoreR.drawable.ic_search),
                contentDescription = null,
            )
        },
    )
    AppIconButton(
        onClick = onRefresh,
        variant = AppButtonVariant.Ghost,
        content = {
            Icon(
                painter = painterResource(CoreR.drawable.ic_rotate_ccw),
                contentDescription = null,
            )
        },
    )
    AppIconButton(
        onClick = onInfo,
        variant = AppButtonVariant.Ghost,
        content = {
            Icon(
                painter = painterResource(CoreR.drawable.ic_layout_list),
                contentDescription = null,
            )
        },
    )
    AppIconButton(
        onClick = onSort,
        variant = AppButtonVariant.Ghost,
        content = {
            Icon(
                painter = painterResource(CoreR.drawable.ic_layout_arrow_down),
                contentDescription = null,
            )
        },
    )
}

@Composable
fun DisplayInfoDialog(
    fields: Set<ListDisplayField>,
    onToggle: (ListDisplayField) -> Unit,
    onDismiss: () -> Unit,
) {
    BaseDialog(
        title = stringResource(CoreR.string.display_info),
        description = stringResource(CoreR.string.display_info_summary),
        onDismiss = onDismiss,
        negativeButton = {},
        positiveButton = {
            AppButton(onClick = onDismiss) { Text(stringResource(CoreR.string.close)) }
        },
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            AppOptionRow(
                text = stringResource(CoreR.string.display_year),
                onClick = { onToggle(ListDisplayField.Year) },
                selected = ListDisplayField.Year in fields,
                iconDrawableId = CoreR.drawable.ic_calendar,
                role = Role.Checkbox,
            )
            AppOptionRow(
                text = stringResource(CoreR.string.display_rating),
                onClick = { onToggle(ListDisplayField.CommunityRating) },
                selected = ListDisplayField.CommunityRating in fields,
                iconDrawableId = CoreR.drawable.ic_star,
                role = Role.Checkbox,
            )
            AppOptionRow(
                text = stringResource(CoreR.string.display_age_rating),
                onClick = { onToggle(ListDisplayField.AgeRating) },
                selected = ListDisplayField.AgeRating in fields,
                iconDrawableId = CoreR.drawable.ic_shield,
                role = Role.Checkbox,
            )
        }
    }
}

@PreviewScreenSizes
@Composable
private fun LibraryScreenLayoutPreview() {
    FindroidTheme {
        LibraryScreenLayout(
            libraryName = "Movies",
            showTopBar = true,
            embedded = false,
            state = LibraryState(items = dummyMovies),
            onAction = {},
        )
    }
}
