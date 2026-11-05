package dev.hdytrfli.onigiri.feature.film.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.hdytrfli.onigiri.core.R as CoreR
import dev.hdytrfli.onigiri.feature.film.R
import dev.hdytrfli.onigiri.feature.film.presentation.book.BookDetailViewModel
import dev.hdytrfli.onigiri.feature.film.presentation.components.ActorsRow
import dev.hdytrfli.onigiri.feature.film.presentation.components.DownloadButton
import dev.hdytrfli.onigiri.feature.film.presentation.components.FullPageError
import dev.hdytrfli.onigiri.feature.film.presentation.components.InfoRows
import dev.hdytrfli.onigiri.feature.film.presentation.components.ItemButtonsBar
import dev.hdytrfli.onigiri.feature.film.presentation.components.ItemHeader
import dev.hdytrfli.onigiri.feature.film.presentation.components.ItemTitle
import dev.hdytrfli.onigiri.feature.film.presentation.components.ItemTopBar
import dev.hdytrfli.onigiri.feature.film.presentation.components.OverviewText
import dev.hdytrfli.onigiri.feature.film.presentation.downloader.DownloaderAction
import dev.hdytrfli.onigiri.feature.film.presentation.downloader.DownloaderEvent
import dev.hdytrfli.onigiri.feature.film.presentation.downloader.DownloaderViewModel
import dev.hdytrfli.onigiri.models.FindroidBook
import dev.hdytrfli.onigiri.ui.components.AppButtonGroup
import dev.hdytrfli.onigiri.ui.components.AppLoadingIndicator
import dev.hdytrfli.onigiri.ui.components.OfflineModeReload
import dev.hdytrfli.onigiri.ui.theme.spacings
import dev.hdytrfli.onigiri.ui.utils.rememberSafePadding
import dev.hdytrfli.onigiri.utils.ObserveAsEvents
import java.util.UUID
import org.jellyfin.sdk.model.api.PersonKind

/** A book's detail page, built from the same components as the movie detail page. */
@Composable
fun BookDetailScreen(
    bookId: UUID,
    navigateBack: () -> Unit,
    onRead: (FindroidBook, Boolean) -> Unit,
    onOpenPlayer: () -> Unit = {},
    navigateToDownloads: () -> Unit,
    onAuthorClick: (UUID) -> Unit = {},
    viewModel: BookDetailViewModel = hiltViewModel(),
    downloaderViewModel: DownloaderViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val downloaderState by downloaderViewModel.state.collectAsStateWithLifecycle()

    LifecycleResumeEffect(bookId) {
        viewModel.load(bookId, force = true)
        onPauseOrDispose {}
    }
    OfflineModeReload { viewModel.load(bookId, force = true) }
    LaunchedEffect(state.item) { state.item?.let { item -> downloaderViewModel.update(item) } }
    ObserveAsEvents(downloaderViewModel.events) { event ->
        when (event) {
            is DownloaderEvent.Successful,
            is DownloaderEvent.Deleted -> viewModel.load(bookId, force = true)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            state.error != null ->
                FullPageError(
                    onRefresh = { viewModel.load(bookId) },
                    isRefreshing = false,
                    error = state.error,
                    modifier = Modifier.fillMaxSize(),
                )
            state.isLoading || state.item == null ->
                AppLoadingIndicator(modifier = Modifier.align(Alignment.Center))
            else -> {
                val item = state.item!!
                val progress = state.readingProgress
                val readable = item.isReadable
                val audiobook = item.isAudiobook
                val readLabel =
                    when {
                        audiobook -> stringResource(CoreR.string.play)
                        !readable -> stringResource(R.string.book_unsupported)
                        item.played -> "Read"
                        state.pagesLeft > 0 -> "${state.pagesLeft} pages left"
                        else -> "Read"
                    }
                val safePadding = rememberSafePadding()
                val paddingStart = safePadding.start + MaterialTheme.spacings.gutter
                val paddingEnd = safePadding.end + MaterialTheme.spacings.gutter
                val scrollState = rememberScrollState()

                Column(modifier = Modifier.fillMaxWidth().verticalScroll(scrollState)) {
                    ItemHeader(item = item, scrollState = scrollState) {
                        Column(
                            modifier =
                                Modifier.align(Alignment.BottomStart)
                                    .padding(start = paddingStart, end = paddingEnd)
                        ) {
                            ItemTitle(
                                title = item.name,
                                logoUri = item.images.logo,
                                maxLines = 1,
                            )
                        }
                    }
                    Column(modifier = Modifier.padding(start = paddingStart, end = paddingEnd)) {
                        Spacer(Modifier.height(MaterialTheme.spacings.small))
                        ItemButtonsBar(
                            item = item,
                            onPlayClick = { startFromBeginning ->
                                when {
                                    audiobook -> {
                                        viewModel.playAudiobook(item)
                                        onOpenPlayer()
                                    }
                                    readable -> onRead(item, startFromBeginning)
                                }
                            },
                            onMarkAsPlayedClick = { viewModel.setPlayed(item.id, !item.played) },
                            onMarkAsFavoriteClick = {
                                viewModel.setFavorite(item.id, !item.favorite)
                            },
                            canPlay = readable || audiobook,
                            playLabel = readLabel,
                            playIconRes =
                                if (audiobook) CoreR.drawable.ic_play
                                else CoreR.drawable.ic_book_open,
                            showRestartButton = readable,
                            hasProgress = readable && progress > 0f && !item.played,
                            forcePlayEnabled = readable || audiobook,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(MaterialTheme.spacings.medium))
                        OverviewText(text = item.overview, maxCollapsedLines = 4)
                        Spacer(Modifier.height(MaterialTheme.spacings.medium))
                        InfoRows(
                            rows =
                                listOf(
                                    stringResource(R.string.book_info_year) to
                                        (item.productionYear ?: item.premiereDate?.year)
                                            ?.toString()
                                            .orEmpty(),
                                    stringResource(R.string.book_info_author) to
                                        item.people
                                            .filter { it.type == PersonKind.AUTHOR }
                                            .ifEmpty { item.people }
                                            .joinToString(", ") { it.name },
                                    stringResource(R.string.book_info_publisher) to
                                        item.studios.joinToString(", "),
                                )
                        )
                        if (item.people.isNotEmpty()) {
                            Spacer(Modifier.height(MaterialTheme.spacings.medium))
                        }
                    }
                    if (item.people.isNotEmpty()) {
                        ActorsRow(
                            actors = item.people,
                            onActorClick = onAuthorClick,
                            contentPadding =
                                androidx.compose.foundation.layout.PaddingValues(
                                    start = paddingStart,
                                    end = paddingEnd,
                                ),
                            titleRes = CoreR.string.authors,
                        )
                    }
                }

                ItemTopBar(
                    hasBackButton = true,
                    onBackClick = navigateBack,
                    trailingContent = {
                        AppButtonGroup {
                            DownloadButton(
                                item = item,
                                downloaderState = downloaderState,
                                onDownloadClick = { storageIndex ->
                                    downloaderViewModel.onAction(
                                        DownloaderAction.Download(item, storageIndex)
                                    )
                                },
                                onCancelDownloadClick = {
                                    downloaderViewModel.onAction(
                                        DownloaderAction.CancelDownload(item)
                                    )
                                },
                                onDeleteDownloadClick = {
                                    downloaderViewModel.onAction(
                                        DownloaderAction.DeleteDownload(item)
                                    )
                                },
                                onNavigateToDownloads = navigateToDownloads,
                                downloadAvailable = true,
                            )
                        }
                    },
                )
            }
        }
    }
}
