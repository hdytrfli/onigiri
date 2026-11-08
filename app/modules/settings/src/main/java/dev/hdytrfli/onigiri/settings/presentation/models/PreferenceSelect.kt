package dev.hdytrfli.onigiri.settings.presentation.models

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import dev.hdytrfli.onigiri.settings.domain.models.Preference as PreferenceBackend

data class PreferenceSelect(
    @param:StringRes override val nameStringResource: Int,
    @param:StringRes override val descriptionStringRes: Int,
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
    val optionIcon: OptionIcon? = null,
    /** Use the shared language picker with suggestions instead of the generic option list. */
    val isLanguagePicker: Boolean = false,
    val suggestedValues: List<String> = emptyList(),
) : Preference
