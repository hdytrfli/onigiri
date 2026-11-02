package dev.hdytrfli.onigiri.settings.presentation.settings

import dev.hdytrfli.onigiri.settings.presentation.models.Preference

sealed interface SettingsAction {
    data class OnUpdate(val preference: Preference) : SettingsAction
}
