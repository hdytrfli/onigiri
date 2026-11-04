package dev.hdytrfli.onigiri.feature.film.presentation.music

enum class MusicTab {
    SONGS,
    ARTISTS,
    ALBUMS,
}

/** Per-tab fields that can be shown in music rows and cards. */
enum class MusicDisplayField(val key: String) {
    Duration("duration"),
    Artist("artist"),
    Album("album"),
    Year("year"),
    AlbumCount("album_count");

    companion object {
        fun fromKeys(keys: Set<String>): Set<MusicDisplayField> =
            entries.filterTo(mutableSetOf()) { it.key in keys }
    }
}

/** Per-tab sort fields. */
enum class MusicSortField {
    Title,
    Duration,
    Artist,
    Album,
    Year,
    AlbumCount,
}

val SongDisplayFields =
    setOf(
        MusicDisplayField.Duration,
        MusicDisplayField.Artist,
        MusicDisplayField.Album,
        MusicDisplayField.Year,
    )
val ArtistDisplayFields = setOf(MusicDisplayField.AlbumCount)
val AlbumDisplayFields = setOf(MusicDisplayField.Artist, MusicDisplayField.Year)

val SongSortFields =
    setOf(
        MusicSortField.Title,
        MusicSortField.Duration,
        MusicSortField.Artist,
        MusicSortField.Album,
        MusicSortField.Year,
    )
val ArtistSortFields = setOf(MusicSortField.Title, MusicSortField.AlbumCount)
val AlbumSortFields = setOf(MusicSortField.Title, MusicSortField.Artist, MusicSortField.Year)
