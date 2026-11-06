package dev.hdytrfli.onigiri.settings.presentation.models

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import dev.hdytrfli.onigiri.settings.domain.models.Preference as PreferenceBackend

/**
 * One setting edited with several stepped sliders inside a single dialog - a column count for
 * portrait and landscape, for example. Every entry of [sliders] is a self-contained
 * [PreferenceSlider]: its name labels the row, its backend stores the value, and its options drive
 * the range. Saving applies each row to its own backend preference.
 */
data class PreferenceSliders(
    @param:StringRes override val nameStringResource: Int,
    @param:StringRes override val descriptionStringRes: Int,
    @param:DrawableRes override val iconDrawableId: Int? = null,
    override val enabled: Boolean = true,
    override val dependencies: List<PreferenceBackend<Boolean>> = emptyList(),
    override val hideWhenDisabled: Boolean = false,
    val sliders: List<PreferenceSlider>,
) : Preference
