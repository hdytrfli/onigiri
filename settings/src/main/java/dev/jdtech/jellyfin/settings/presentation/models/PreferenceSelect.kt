package dev.jdtech.jellyfin.settings.presentation.models

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import dev.jdtech.jellyfin.settings.domain.models.Preference as PreferenceBackend

/**
 * The leading icon a select dialog shows on its option rows; the UI layer maps it to a drawable.
 */
enum class SelectOptionIcon {
    Audio,
    Subtitle,
}

data class PreferenceSelect(
    @param:StringRes override val nameStringResource: Int,
    @param:StringRes override val descriptionStringRes: Int? = null,
    @param:DrawableRes override val iconDrawableId: Int? = null,
    override val enabled: Boolean = true,
    override val dependencies: List<PreferenceBackend<Boolean>> = emptyList(),
    override val hideWhenDisabled: Boolean = false,
    val onUpdate: (String?) -> Unit = {},
    val backendPreference: PreferenceBackend<String?>,
    val options: Int,
    val optionValues: Int,
    val optionsIncludeNull: Boolean = false,
    val value: String? = null,
    val optionIcon: SelectOptionIcon? = null,
) : Preference
