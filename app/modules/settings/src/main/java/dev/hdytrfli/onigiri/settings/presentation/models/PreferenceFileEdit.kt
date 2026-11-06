package dev.hdytrfli.onigiri.settings.presentation.models

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import dev.hdytrfli.onigiri.settings.domain.models.Preference as PreferenceBackend

data class PreferenceFileEdit(
    @param:StringRes override val nameStringResource: Int,
    @param:StringRes override val descriptionStringRes: Int,
    @param:DrawableRes override val iconDrawableId: Int? = null,
    override val enabled: Boolean = true,
    override val dependencies: List<PreferenceBackend<Boolean>> = emptyList(),
    val filePath: String,
    val onClick: (PreferenceFileEdit) -> Unit = {},
) : Preference
