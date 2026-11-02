package dev.hdytrfli.onigiri.settings.presentation.models

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import dev.hdytrfli.onigiri.settings.domain.models.Preference as PreferenceBackend

/**
 * A preference shown as a row with its title/description on the left and its current value between
 * minus and plus steppers on the right.
 *
 * The value is stored as a [String] (the same storage the grid-column selects used), so it is
 * parsed to an [Int] for display and stepping and clamped to [valueRange].
 */
data class PreferenceStepper(
    @param:StringRes override val nameStringResource: Int,
    @param:StringRes override val descriptionStringRes: Int? = null,
    @param:DrawableRes override val iconDrawableId: Int? = null,
    override val enabled: Boolean = true,
    override val dependencies: List<PreferenceBackend<Boolean>> = emptyList(),
    override val hideWhenDisabled: Boolean = false,
    val backendPreference: PreferenceBackend<String?>,
    val value: String? = null,
    val valueRange: IntRange = 1..10,
) : Preference
