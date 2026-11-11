package dev.hdytrfli.onigiri.feature.film.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.hdytrfli.onigiri.feature.film.presentation.music.MusicCard
import dev.hdytrfli.onigiri.models.FindroidEpisode
import dev.hdytrfli.onigiri.models.FindroidItem
import dev.hdytrfli.onigiri.models.FindroidMusicAlbum
import dev.hdytrfli.onigiri.models.FindroidMusicArtist
import dev.hdytrfli.onigiri.ui.utils.GridSize
import dev.hdytrfli.onigiri.ui.utils.LocalGridGap
import dev.hdytrfli.onigiri.ui.utils.gridColumns

/** A horizontally scrolling movie/show/collection row used by the detail-page sections. */
@Composable
fun DetailItemRow(
    title: String,
    items: List<FindroidItem>,
    onItemClick: (FindroidItem) -> Unit,
    contentPadding: PaddingValues,
) {
    if (items.isEmpty()) return
    GridWidthRow(
        title = title,
        items = items,
        gridSize = GridSize.POSTER,
        contentPadding = contentPadding,
        key = { it.id },
    ) { item, width ->
        ItemCard(
            item = item,
            direction = Direction.VERTICAL,
            onClick = onItemClick,
            modifier = Modifier.width(width),
            showYear = true,
        )
    }
}

/** A horizontally scrolling season row using the configured thumbnail grid size. */
@Composable
fun DetailEpisodeRow(
    title: String,
    episodes: List<FindroidEpisode>,
    onEpisodeClick: (FindroidEpisode) -> Unit,
    contentPadding: PaddingValues,
) {
    if (episodes.isEmpty()) return
    GridWidthRow(
        title = title,
        items = episodes,
        gridSize = GridSize.THUMBNAIL,
        contentPadding = contentPadding,
        key = { it.id },
    ) { episode, width ->
        EpisodeGridCard(
            episode = episode,
            onClick = { onEpisodeClick(episode) },
            modifier = Modifier.width(width),
        )
    }
}

/** A horizontally scrolling album row used for "More from [artist]". */
@Composable
fun DetailMusicAlbumRow(
    title: String,
    albums: List<FindroidMusicAlbum>,
    onAlbumClick: (FindroidMusicAlbum) -> Unit,
    contentPadding: PaddingValues,
) {
    if (albums.isEmpty()) return
    GridWidthRow(
        title = title,
        items = albums,
        gridSize = GridSize.POSTER,
        contentPadding = contentPadding,
        key = { it.id },
    ) { album, width ->
        MusicCard(
            title = album.name,
            subtitle =
                listOfNotNull(
                        album.artistName,
                        album.productionYear?.toString(),
                    )
                    .joinToString(" · "),
            image = album.images.primary,
            circular = false,
            onClick = { onAlbumClick(album) },
            modifier = Modifier.width(width),
        )
    }
}

/** A horizontally scrolling artist row used for "More like this". */
@Composable
fun DetailMusicArtistRow(
    title: String,
    artists: List<FindroidMusicArtist>,
    onArtistClick: (FindroidMusicArtist) -> Unit,
    contentPadding: PaddingValues,
) {
    if (artists.isEmpty()) return
    GridWidthRow(
        title = title,
        items = artists,
        gridSize = GridSize.POSTER,
        contentPadding = contentPadding,
        key = { it.id },
    ) { artist, width ->
        MusicCard(
            title = artist.name,
            subtitle = null,
            image = artist.images.primary,
            circular = true,
            onClick = { onArtistClick(artist) },
            modifier = Modifier.width(width),
        )
    }
}

@Composable
private fun <T> GridWidthRow(
    title: String,
    items: List<T>,
    gridSize: GridSize,
    contentPadding: PaddingValues,
    key: (T) -> Any,
    itemContent: @Composable (T, Dp) -> Unit,
) {
    val layoutDirection = LocalLayoutDirection.current
    val start = contentPadding.calculateStartPadding(layoutDirection)
    val end = contentPadding.calculateEndPadding(layoutDirection)
    val gap = LocalGridGap.current
    val columns = gridColumns(gridSize).coerceAtLeast(1)

    BoxWithConstraints {
        val cardWidth =
            ((maxWidth - start - end - gap * (columns - 1)) / columns).coerceAtLeast(72.dp)
        Section(
            title = title,
            titleModifier = Modifier.padding(start = start, end = end),
        ) {
            LazyRow(
                contentPadding = PaddingValues(start = start, end = end),
                horizontalArrangement = Arrangement.spacedBy(gap),
            ) {
                items(items = items, key = key) { item -> itemContent(item, cardWidth) }
            }
        }
    }
}
