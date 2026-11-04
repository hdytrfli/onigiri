package dev.hdytrfli.onigiri

/** A music detail page to open after the player hands control back to the main activity. */
sealed interface MusicLaunchDestination {
    data class Artist(val id: String) : MusicLaunchDestination

    data class Album(val id: String) : MusicLaunchDestination
}

const val ExtraMusicArtistId = "dev.hdytrfli.onigiri.extra.MUSIC_ARTIST_ID"
const val ExtraMusicAlbumId = "dev.hdytrfli.onigiri.extra.MUSIC_ALBUM_ID"
