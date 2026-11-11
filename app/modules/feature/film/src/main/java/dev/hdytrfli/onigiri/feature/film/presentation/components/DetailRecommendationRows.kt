package dev.hdytrfli.onigiri.feature.film.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import dev.hdytrfli.onigiri.feature.film.presentation.music.MusicCard
import dev.hdytrfli.onigiri.models.FindroidEpisode
import dev.hdytrfli.onigiri.models.FindroidItem
import dev.hdytrfli.onigiri.models.FindroidMusicAlbum
import dev.hdytrfli.onigiri.models.FindroidMusicArtist
import dev.hdytrfli.onigiri.ui.theme.spacings
import dev.hdytrfli.onigiri.ui.utils.LocalGridGap

/** A horizontally scrolling movie/show/collection row used by the detail-page sections. */
@Composable
fun DetailItemRow(
    title: String,
    items: List<FindroidItem>,
    onItemClick: (FindroidItem) -> Unit,
    contentPadding: PaddingValues,
) {
    if (items.isEmpty()) return
    val layoutDirection = LocalLayoutDirection.current
    val start = contentPadding.calculateStartPadding(layoutDirection)
    val end = contentPadding.calculateEndPadding(layoutDirection)
    Section(
        title = title,
        titleModifier = Modifier.padding(start = start, end = end),
    ) {
        LazyRow(
            contentPadding = PaddingValues(start = start, end = end),
            horizontalArrangement = Arrangement.spacedBy(LocalGridGap.current),
        ) {
            items(items = items, key = { it.id }) { item ->
                ItemCard(
                    item = item,
                    direction = Direction.VERTICAL,
                    onClick = onItemClick,
                    showYear = true,
                )
            }
        }
    }
}

/** A horizontally scrolling season row. */
@Composable
fun DetailEpisodeRow(
    title: String,
    episodes: List<FindroidEpisode>,
    onEpisodeClick: (FindroidEpisode) -> Unit,
    contentPadding: PaddingValues,
) {
    if (episodes.isEmpty()) return
    val layoutDirection = LocalLayoutDirection.current
    val start = contentPadding.calculateStartPadding(layoutDirection)
    val end = contentPadding.calculateEndPadding(layoutDirection)
    Section(
        title = title,
        titleModifier = Modifier.padding(start = start, end = end),
    ) {
        LazyRow(
            contentPadding = PaddingValues(start = start, end = end),
            horizontalArrangement = Arrangement.spacedBy(LocalGridGap.current),
        ) {
            items(items = episodes, key = { it.id }) { episode ->
                EpisodeCard(
                    episode = episode,
                    onClick = { onEpisodeClick(episode) },
                    modifier = Modifier.width(320.dp),
                )
            }
        }
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
    val layoutDirection = LocalLayoutDirection.current
    val start = contentPadding.calculateStartPadding(layoutDirection)
    val end = contentPadding.calculateEndPadding(layoutDirection)
    Section(
        title = title,
        titleModifier = Modifier.padding(start = start, end = end),
    ) {
        LazyRow(
            contentPadding = PaddingValues(start = start, end = end),
            horizontalArrangement = Arrangement.spacedBy(LocalGridGap.current),
        ) {
            items(items = albums, key = { it.id }) { album ->
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
                    modifier = Modifier.width(150.dp),
                )
            }
        }
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
    val layoutDirection = LocalLayoutDirection.current
    val start = contentPadding.calculateStartPadding(layoutDirection)
    val end = contentPadding.calculateEndPadding(layoutDirection)
    Section(
        title = title,
        titleModifier = Modifier.padding(start = start, end = end),
    ) {
        LazyRow(
            contentPadding = PaddingValues(start = start, end = end),
            horizontalArrangement = Arrangement.spacedBy(LocalGridGap.current),
        ) {
            items(items = artists, key = { it.id }) { artist ->
                MusicCard(
                    title = artist.name,
                    subtitle = null,
                    image = artist.images.primary,
                    circular = true,
                    onClick = { onArtistClick(artist) },
                    modifier = Modifier.width(120.dp),
                )
            }
        }
    }
}
