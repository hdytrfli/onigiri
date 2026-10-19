package dev.jdtech.jellyfin.settings.presentation.models

/**
 * The leading icon an option dialog shows on every row, like the player sheet's track list. The UI
 * layer maps each case to a drawable, so the settings module never references core resources.
 */
enum class OptionIcon {
    Audio,
    AudioOutput,
    Subtitle,
    Grid,
    Portrait,
    Landscape,
    Image,
    Width,
    Decode,
    Monitor,
    Clock,
    Segment,
    Sort,
    Storage,
}
