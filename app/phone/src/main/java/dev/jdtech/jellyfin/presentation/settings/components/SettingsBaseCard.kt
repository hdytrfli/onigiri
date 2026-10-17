package dev.jdtech.jellyfin.presentation.settings.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.settings.presentation.models.Preference

/** Maximum fraction of a row's width its trailing value may take before it ellipsises. */
private const val TRAILING_VALUE_MAX_WIDTH_FRACTION = 0.45f

/** Alpha every part of a disabled row fades to, title and trailing text alike. */
private const val DISABLED_ALPHA = 0.38f

/**
 * One settings row: optional leading icon, title with its description underneath, and an optional
 * trailing value and/or trailing content. The title column always takes the remaining width so a
 * long trailing value can never squeeze it.
 *
 * Pass [onClick] to make the whole row clickable (the common case) or leave it null for rows whose
 * only interactions are their trailing controls, such as [SettingsStepperCard].
 */
@Composable
fun SettingsBaseCard(
    preference: Preference,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    title: String = stringResource(preference.nameStringResource),
    value: String? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    val contentColor =
        contentColorFor(MaterialTheme.colorScheme.surface)
            .copy(alpha = if (preference.enabled) 1.0f else DISABLED_ALPHA)

    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = modifier,
            enabled = preference.enabled,
            color = Color.Transparent,
            contentColor = contentColor,
        ) {
            SettingsCardRow(
                preference = preference,
                title = title,
                value = value,
                trailing = trailing,
            )
        }
    } else {
        Surface(modifier = modifier, color = Color.Transparent, contentColor = contentColor) {
            SettingsCardRow(
                preference = preference,
                title = title,
                value = value,
                trailing = trailing,
            )
        }
    }
}

@Composable
private fun SettingsCardRow(
    preference: Preference,
    title: String,
    value: String?,
    trailing: @Composable (() -> Unit)?,
) {
    BoxWithConstraints {
        val trailingValueMaxWidth = maxWidth * TRAILING_VALUE_MAX_WIDTH_FRACTION
        // The title fades through its Surface's content colour, so the trailing texts have to
        // apply the same fade themselves to stay in step with it.
        val textAlpha = if (preference.enabled) 1.0f else DISABLED_ALPHA
        Row(
            modifier = Modifier.padding(MaterialTheme.spacings.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (preference.iconDrawableId != null) {
                Icon(
                    painter = painterResource(preference.iconDrawableId!!),
                    contentDescription = null,
                )
                Spacer(modifier = Modifier.width(MaterialTheme.spacings.default))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                preference.descriptionStringRes?.let {
                    Spacer(modifier = Modifier.height(MaterialTheme.spacings.extraSmall))
                    Text(
                        text = stringResource(id = it),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = textAlpha),
                    )
                }
            }
            if (value != null) {
                Spacer(modifier = Modifier.width(MaterialTheme.spacings.default))
                Text(
                    text = value,
                    modifier = Modifier.widthIn(max = trailingValueMaxWidth),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = textAlpha),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End,
                )
            }
            if (trailing != null) {
                Spacer(modifier = Modifier.width(MaterialTheme.spacings.default))
                trailing()
            }
        }
    }
}
