package dev.hdytrfli.onigiri.settings.presentation.settings

import dev.hdytrfli.onigiri.settings.presentation.models.PreferenceGroup

data class SettingsState(
    val isLoading: Boolean = false,
    val preferenceGroups: List<PreferenceGroup> = emptyList(),
)
