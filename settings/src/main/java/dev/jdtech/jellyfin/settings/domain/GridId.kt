package dev.jdtech.jellyfin.settings.domain

/**
 * A configurable media grid. Grids that render the same kind of item share one [GridId] so that a
 * change to the column count applies to every one of them, while grids with a different item shape
 * (a poster versus a wide library tile, or a 2:3 poster result versus a 16:9 wide result) keep
 * separate settings.
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

    /** Poster-shaped (2:3) remote image results, e.g. identify results and primary image search. */
    IMAGE_POSTERS("image_posters", defaultPortrait = 3, defaultLandscape = 4),

    /** Wide (16:9) remote image results, e.g. backdrop or episode primary image search. */
    IMAGE_WIDE("image_wide", defaultPortrait = 3, defaultLandscape = 4),

    /** The single Primary preview area on the edit-images page. */
    EDIT_PRIMARY("edit_primary", defaultPortrait = 2, defaultLandscape = 4),

    /** The Backdrop strip on the edit-images page. */
    EDIT_BACKDROP("edit_backdrop", defaultPortrait = 3, defaultLandscape = 4),

    /** The Thumb/Logo/Art row on the edit-images page. */
    EDIT_THUMB_ROW("edit_thumb_row", defaultPortrait = 3, defaultLandscape = 4),
}
