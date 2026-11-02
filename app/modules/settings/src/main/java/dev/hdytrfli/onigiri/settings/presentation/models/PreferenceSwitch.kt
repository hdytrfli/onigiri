package dev.hdytrfli.onigiri.settings.presentation.models

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import dev.hdytrfli.onigiri.settings.domain.models.Preference as PreferenceBackend

data class PreferenceSwitch(
    @param:StringRes override val nameStringResource: Int,
    @param:StringRes override val descriptionStringRes: Int? = null,
    @param:DrawableRes override val iconDrawableId: Int? = null,
    override val enabled: Boolean = true,
    override val hideWhenDisabled: Boolean = false,
    override val dependencies: List<PreferenceBackend<Boolean>> = emptyList(),
    val onClick: (PreferenceSwitch) -> Unit = {},
    val backendPreference: PreferenceBackend<Boolean>,
    val value: Boolean = false,
) : Preference
