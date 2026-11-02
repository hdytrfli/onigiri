package dev.hdytrfli.onigiri.settings.presentation.models

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import dev.hdytrfli.onigiri.settings.domain.models.Preference as PreferenceBackend

/**
 * A setting whose value is one of an ordered list of steps, edited with a stepped slider instead of
 * a list. [optionValues] and [options] are parallel arrays: the values are stored, the names label
 * the slider.
 */
data class PreferenceSlider(
    @param:StringRes override val nameStringResource: Int,
    @param:StringRes override val descriptionStringRes: Int? = null,
    @param:DrawableRes override val iconDrawableId: Int? = null,
    override val enabled: Boolean = true,
    override val dependencies: List<PreferenceBackend<Boolean>> = emptyList(),
    override val hideWhenDisabled: Boolean = false,
    val onUpdate: (String?) -> Unit = {},
    val backendPreference: PreferenceBackend<String>,
    val options: Int,
    val optionValues: Int,
    val value: String? = null,
    val optionIcon: OptionIcon? = null,
) : Preference
