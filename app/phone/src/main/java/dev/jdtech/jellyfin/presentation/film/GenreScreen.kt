package dev.jdtech.jellyfin.presentation.film

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.film.presentation.library.LibraryAction
import dev.jdtech.jellyfin.film.presentation.library.LibraryViewModel
import dev.jdtech.jellyfin.models.CollectionType
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.presentation.components.AppButtonVariant
import dev.jdtech.jellyfin.presentation.components.AppIconButton
import dev.jdtech.jellyfin.presentation.components.PageSearchField
import dev.jdtech.jellyfin.presentation.components.PageTopBar
import dev.jdtech.jellyfin.presentation.components.RootPage
import java.util.UUID

/** A genre's media grid, with a floating title bar, search field, and display/sort actions. */
@Composable
fun GenreScreen(
    genreId: UUID,
    genreName: String,
    libraryType: CollectionType,
    onItemClick: (FindroidItem) -> Unit,
    navigateBack: () -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showSortDialog by rememberSaveable { mutableStateOf(false) }
    var showDisplayDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(genreId, libraryType) {
        viewModel.setup(
            parentId = null,
            libraryType = libraryType,
            genreIds = listOf(genreId),
        )
        viewModel.loadItems()
    }

    BackHandler(enabled = state.searchExpanded) {
        viewModel.onAction(LibraryAction.SetSearchQuery(""))
        viewModel.onAction(LibraryAction.ToggleSearch)
    }

    val toolbarActions: @Composable RowScope.() -> Unit = {
        if (!state.searchExpanded) {
            AppIconButton(
                onClick = { viewModel.onAction(LibraryAction.ToggleSearch) },
                variant = AppButtonVariant.Secondary,
                content = {
                    Icon(
                        painter = painterResource(CoreR.drawable.ic_search),
                        contentDescription = stringResource(CoreR.string.search),
                    )
                },
            )
        }
        AppIconButton(
            onClick = { showDisplayDialog = true },
            variant = AppButtonVariant.Secondary,
            content = {
                Icon(
                    painter = painterResource(CoreR.drawable.ic_tv_minimal),
                    contentDescription = stringResource(CoreR.string.display_info),
                )
            },
        )
        AppIconButton(
            onClick = { showSortDialog = true },
            variant = AppButtonVariant.Secondary,
            content = {
                Icon(
                    painter = painterResource(CoreR.drawable.ic_arrow_down_up),
                    contentDescription = stringResource(CoreR.string.sort_by),
                )
            },
        )
    }

    RootPage(
        header = {
            AnimatedContent(
                targetState = state.searchExpanded,
                label = "genre-search",
            ) { searchExpanded ->
                if (searchExpanded) {
                    PageSearchField(
                        value = state.searchQuery,
                        onValueChange = { viewModel.onAction(LibraryAction.SetSearchQuery(it)) },
                        onClose = { viewModel.onAction(LibraryAction.ToggleSearch) },
                    )
                } else {
                    PageTopBar(
                        title = genreName.ifBlank { stringResource(CoreR.string.genres) },
                        onBack = navigateBack,
                        trailingContent = toolbarActions,
                        includeTopInset = false,
                    )
                }
            }
        }
    ) {
        LibraryContent(
            state = state,
            onAction = { action ->
                if (action is LibraryAction.OnItemClick) {
                    onItemClick(action.item)
                }
                viewModel.onAction(action)
            },
            showSortDialog = showSortDialog,
            showDisplayDialog = showDisplayDialog,
            onShowSortDialogChange = { showSortDialog = it },
            onShowDisplayDialogChange = { showDisplayDialog = it },
            embedded = true,
            sectionTitle = genreName.takeIf { it.isNotBlank() },
            showSearchField = false,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
