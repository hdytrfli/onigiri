package dev.hdytrfli.onigiri.settings.presentation.models

import dev.hdytrfli.onigiri.settings.domain.models.Preference as PreferenceBackend

interface Preference {
    val nameStringResource: Int
    val descriptionStringRes: Int?
    val iconDrawableId: Int?
    val enabled: Boolean
    val dependencies: List<PreferenceBackend<Boolean>>

    /** When true and [dependencies] are not all satisfied, hide instead of disabling. */
    val hideWhenDisabled: Boolean
        get() = false
}
