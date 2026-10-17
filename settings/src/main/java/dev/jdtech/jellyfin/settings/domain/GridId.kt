package dev.jdtech.jellyfin.settings.domain

/**
 * A configurable media grid. Grids that render the same kind of item share one [GridId] so that a
 * change to the column count applies to every one of them, while grids with a different item shape
 * (a poster versus a wide library tile) keep separate settings.
 *
 * [defaultPortrait] and [defaultLandscape] reproduce the column counts the app used before this
 * setting existed, so nothing changes until the user edits it.
 */
enum class GridId(
    val key: String,
    val defaultPortrait: Int,
    val defaultLandscape: Int,
) {
    /** Poster/show/movie cards (vertical [ItemCard]). */
    POSTERS("posters", defaultPortrait = 2, defaultLandscape = 4),

    /** Wide library tiles (horizontal [ItemCard]) shown on the media tab. */
    LIBRARIES("libraries", defaultPortrait = 2, defaultLandscape = 3),

    /** The season episode grid (16:9 episode cards). */
    EPISODES("episodes", defaultPortrait = 3, defaultLandscape = 4),

    /** Remote image results (identify results and the edit-image search). */
    IMAGES("images", defaultPortrait = 3, defaultLandscape = 4),
}
