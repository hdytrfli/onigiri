package dev.jdtech.jellyfin.settings.presentation.models

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import dev.jdtech.jellyfin.settings.domain.models.Preference as PreferenceBackend

/**
 * A preference shown as a row with its title/description on the left and the current value followed
 * by a chevron on the right.
 *
 * Unlike [PreferenceSelect] it does not open a dialog and, unlike [PreferenceCategory], it shows a
 * value; its [onClick] typically navigates to a bespoke screen. The value is read from
 * [backendPreference] on every load and formatted with [formatValue].
 */
data class PreferenceAction(
    @param:StringRes override val nameStringResource: Int,
    @param:StringRes override val descriptionStringRes: Int? = null,
    @param:DrawableRes override val iconDrawableId: Int? = null,
    override val enabled: Boolean = true,
    override val dependencies: List<PreferenceBackend<Boolean>> = emptyList(),
    override val hideWhenDisabled: Boolean = false,
    val backendPreference: PreferenceBackend<String?>,
    val value: String? = null,
    val formatValue: (String?) -> String? = { it },
    val onClick: (PreferenceAction) -> Unit = {},
) : Preference
