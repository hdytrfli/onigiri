package dev.hdytrfli.onigiri.feature.film.presentation.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.hdytrfli.onigiri.core.R as CoreR
import dev.hdytrfli.onigiri.feature.film.R as FilmR
import dev.hdytrfli.onigiri.feature.film.presentation.components.FullPageError
import dev.hdytrfli.onigiri.feature.film.presentation.components.Section
import dev.hdytrfli.onigiri.ui.components.AppLoadingIndicator
import dev.hdytrfli.onigiri.ui.theme.spacings
import dev.hdytrfli.onigiri.ui.utils.GridSize
import dev.hdytrfli.onigiri.ui.utils.LocalGridGap
import dev.hdytrfli.onigiri.ui.utils.gridColumns
import org.jellyfin.sdk.model.api.BaseItemKind

@Composable
fun HomeSearchContent(
    state: HomeSearchState,
    contentPadding: PaddingValues,
    onResultClick: (HomeSearchResult) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        state.query.isBlank() && !state.loading ->
            SearchMessage(
                message = stringResource(FilmR.string.search_home_prompt),
                modifier = modifier.padding(contentPadding),
            )
        state.loading && state.groups.isEmpty() ->
            Box(
                modifier = modifier.fillMaxSize().padding(contentPadding),
                contentAlignment = Alignment.Center,
            ) {
                AppLoadingIndicator()
            }
        state.error != null && state.groups.isEmpty() ->
            FullPageError(
                onRefresh = onRetry,
                isRefreshing = state.loading,
                error = state.error,
                modifier = modifier.padding(contentPadding),
            )
        state.groups.isEmpty() ->
            SearchMessage(
                message = stringResource(FilmR.string.search_home_empty, state.query),
                modifier = modifier.padding(contentPadding),
            )
        else ->
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = contentPadding,
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.medium),
            ) {
                items(items = state.groups, key = { it.type }) { group ->
                    SearchGroup(group = group, onResultClick = onResultClick)
                }
            }
    }
}

@Composable
private fun SearchGroup(
    group: HomeSearchGroup,
    onResultClick: (HomeSearchResult) -> Unit,
) {
    BoxWithConstraints {
        val columns = gridColumns(group.type.gridSize()).coerceAtLeast(1)
        val gap = LocalGridGap.current
        val gutter = MaterialTheme.spacings.gutter
        val availableWidth =
            (maxWidth - gutter * 2f - gap * (columns - 1).toFloat()).coerceAtLeast(0.dp)
        val itemWidth =
            (availableWidth / columns.toFloat()).coerceAtLeast(
                if (group.type == HomeSearchGroupType.PEOPLE) 72.dp else 96.dp
            )

        Section(
            title = group.type.title(),
            titleModifier = Modifier.padding(horizontal = gutter),
        ) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = gutter),
                horizontalArrangement = Arrangement.spacedBy(gap),
            ) {
                items(items = group.results, key = { "${it.kind}-${it.id}" }) { result ->
                    SearchResultCard(
                        result = result,
                        width = itemWidth,
                        onClick = onResultClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchResultCard(
    result: HomeSearchResult,
    width: Dp,
    onClick: (HomeSearchResult) -> Unit,
) {
    val circular = result.isPerson || result.kind == BaseItemKind.MUSIC_ARTIST
    val navigable = result.kind.isNavigable()
    val aspectRatio = result.aspectRatio()

    Column(
        modifier =
            Modifier.width(width)
                .then(if (navigable) Modifier.clickable { onClick(result) } else Modifier),
        horizontalAlignment = if (circular) Alignment.CenterHorizontally else Alignment.Start,
    ) {
        val shape = if (circular) CircleShape else MaterialTheme.shapes.small
        Surface(
            modifier = Modifier.fillMaxWidth().aspectRatio(aspectRatio).clip(shape),
            shape = shape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            if (result.image != null) {
                AsyncImage(
                    model = result.image,
                    contentDescription = result.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(result.kind.placeholderIcon()),
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Text(
            text = result.name,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = if (circular) TextAlign.Center else TextAlign.Start,
            modifier = Modifier.fillMaxWidth().padding(top = MaterialTheme.spacings.small),
        )
        result.subtitle?.let { subtitle ->
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = if (circular) TextAlign.Center else TextAlign.Start,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun SearchMessage(message: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = MaterialTheme.spacings.gutter),
        )
    }
}

@Composable
private fun HomeSearchGroupType.title(): String =
    when (this) {
        HomeSearchGroupType.PEOPLE -> stringResource(FilmR.string.search_group_people)
        HomeSearchGroupType.MOVIES -> stringResource(CoreR.string.movies_label)
        HomeSearchGroupType.SHOWS -> stringResource(CoreR.string.shows_label)
        HomeSearchGroupType.EPISODES -> stringResource(FilmR.string.search_group_episodes)
        HomeSearchGroupType.SEASONS -> stringResource(FilmR.string.search_group_seasons)
        HomeSearchGroupType.BOOKS -> stringResource(CoreR.string.books_label)
        HomeSearchGroupType.AUDIOBOOKS -> stringResource(CoreR.string.audiobooks_label)
        HomeSearchGroupType.SONGS -> stringResource(FilmR.string.search_group_songs)
        HomeSearchGroupType.ALBUMS -> stringResource(FilmR.string.search_group_albums)
        HomeSearchGroupType.ARTISTS -> stringResource(FilmR.string.search_group_artists)
        HomeSearchGroupType.PLAYLISTS -> stringResource(FilmR.string.search_group_playlists)
        HomeSearchGroupType.COLLECTIONS -> stringResource(CoreR.string.collections_label)
        HomeSearchGroupType.VIDEOS -> stringResource(FilmR.string.search_group_videos)
    }

private fun HomeSearchGroupType.gridSize(): GridSize =
    when (this) {
        HomeSearchGroupType.EPISODES,
        HomeSearchGroupType.VIDEOS -> GridSize.THUMBNAIL
        HomeSearchGroupType.PEOPLE -> GridSize.AVATAR
        else -> GridSize.POSTER
    }

private fun HomeSearchResult.aspectRatio(): Float =
    when (kind) {
        BaseItemKind.EPISODE,
        BaseItemKind.VIDEO,
        BaseItemKind.MUSIC_VIDEO,
        BaseItemKind.TRAILER -> 16f / 9f
        BaseItemKind.AUDIO,
        BaseItemKind.AUDIO_BOOK,
        BaseItemKind.MUSIC_ALBUM,
        BaseItemKind.MUSIC_ARTIST,
        BaseItemKind.PLAYLIST,
        BaseItemKind.PERSON -> 1f
        else -> 2f / 3f
    }

private fun BaseItemKind?.isNavigable(): Boolean =
    when (this) {
        BaseItemKind.PERSON,
        BaseItemKind.MOVIE,
        BaseItemKind.SERIES,
        BaseItemKind.EPISODE,
        BaseItemKind.SEASON,
        BaseItemKind.BOOK,
        BaseItemKind.AUDIO_BOOK,
        BaseItemKind.BOX_SET -> true
        else -> false
    }

private fun BaseItemKind?.placeholderIcon(): Int =
    when (this) {
        BaseItemKind.PERSON -> CoreR.drawable.ic_user
        BaseItemKind.AUDIO,
        BaseItemKind.MUSIC_ALBUM,
        BaseItemKind.MUSIC_ARTIST,
        BaseItemKind.PLAYLIST -> CoreR.drawable.ic_music
        BaseItemKind.BOOK,
        BaseItemKind.AUDIO_BOOK -> CoreR.drawable.ic_book_open
        BaseItemKind.SERIES,
        BaseItemKind.SEASON,
        BaseItemKind.EPISODE -> CoreR.drawable.ic_tv
        BaseItemKind.MOVIE -> CoreR.drawable.ic_clapperboard
        else -> CoreR.drawable.ic_search
    }
