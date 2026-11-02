package dev.hdytrfli.onigiri.settings.presentation.models

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
    Ratio,
    Width,
    Decode,
    Monitor,
    Clock,
    Segment,
    Sort,
    Storage,
    TabletMode,
}
