package dev.jdtech.jellyfin.presentation.components

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.core.presentation.theme.OnScrim
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings

/**
 * The one button in the app.
 *
 * [AppButton] lays its children out in a row and sizes to its label; [AppIconButton] is a square
 * box for an icon alone. Both are pills - a square pill is a circle - and both share this
 * implementation, so they cannot drift apart.
 *
 * The call site writes the children: an icon, a label, both in either order, a spinner, a tinted
 * icon - the button only supplies the row (with the gap between children) and the content colour.
 * There is no label/icon parameter to restate, and no [androidx.compose.foundation.layout.Spacer]
 * to write. The button owns the spacing.
 *
 * @param size the button's role in the app, which fixes its height (and width, for the icon form).
 *   [AppButtonSize.Default] is used everywhere; [AppButtonSize.Player] only for the player's
 *   transport controls.
 * @param variant the colour treatment; see [AppButtonVariant].
 * @param effect how the button reacts to a press; see [AppButtonEffect]. Defaults to
 *   [AppButtonEffect.Tint].
 * @param onHold the press edge, called `true` on press and `false` on release. Only the offset
 *   steppers use it, to step immediately and then repeat while held.
 */
@Composable
fun AppButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: AppButtonSize = AppButtonSize.Default,
    variant: AppButtonVariant = AppButtonVariant.Primary,
    effect: AppButtonEffect = AppButtonEffect.Tint,
    enabled: Boolean = true,
    onHold: ((pressed: Boolean) -> Unit)? = null,
    content: @Composable RowScope.() -> Unit,
) {
    AppButtonImpl(
        onClick = onClick,
        modifier = modifier,
        size = size,
        variant = variant,
        effect = effect,
        enabled = enabled,
        onHold = onHold,
        square = false,
        content = content,
    )
}

/** The square form of [AppButton] for an icon on its own; a circle. */
@Composable
fun AppIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: AppButtonSize = AppButtonSize.Default,
    variant: AppButtonVariant = AppButtonVariant.Secondary,
    effect: AppButtonEffect = AppButtonEffect.Tint,
    enabled: Boolean = true,
    onHold: ((pressed: Boolean) -> Unit)? = null,
    content: @Composable RowScope.() -> Unit,
) {
    AppButtonImpl(
        onClick = onClick,
        modifier = modifier,
        size = size,
        variant = variant,
        effect = effect,
        enabled = enabled,
        onHold = onHold,
        square = true,
        content = content,
    )
}

/**
 * A row of buttons sharing the app's one gap, so a set of actions reads as a group and no call site
 * writes its own spacer between buttons.
 */
@Composable
fun AppButtonGroup(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
private fun AppButtonImpl(
    onClick: () -> Unit,
    modifier: Modifier,
    size: AppButtonSize,
    variant: AppButtonVariant,
    effect: AppButtonEffect,
    enabled: Boolean,
    onHold: ((pressed: Boolean) -> Unit)?,
    square: Boolean,
    content: @Composable RowScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    // Surface hard-wires the ripple and exposes no press hook, hence a Box with an explicit
    // indication: the ripple only when it is asked for, otherwise a tint of our own.
    if (onHold != null) {
        LaunchedEffect(interactionSource) {
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> onHold(true)
                    is PressInteraction.Release,
                    is PressInteraction.Cancel -> onHold(false)
                }
            }
        }
    }

    val containerColor = variant.containerColor()
    val contentColor = variant.contentColor()
    val shownContentColor = if (enabled) contentColor else contentColor.copy(alpha = DisabledAlpha)

    Box(
        // No fillMaxWidth here: the button sizes to its content and only stretches when the call
        // site gives it a weight.
        modifier =
            modifier
                .then(if (square) Modifier.size(size.value) else Modifier.height(size.value))
                .clip(CircleShape)
                .background(containerColor)
                .clickable(
                    interactionSource = interactionSource,
                    indication =
                        if (effect == AppButtonEffect.Ripple) LocalIndication.current else null,
                    enabled = enabled,
                    onClick = onClick,
                ),
        contentAlignment = Alignment.Center,
    ) {
        if (effect == AppButtonEffect.Tint && pressed && enabled) {
            Box(
                modifier =
                    Modifier.matchParentSize()
                        .background(
                            (if (variant == AppButtonVariant.Ghost) OnScrim else contentColor).copy(
                                alpha = TintAlpha
                            )
                        )
            )
        }

        CompositionLocalProvider(
            LocalContentColor provides shownContentColor,
            LocalTextStyle provides MaterialTheme.typography.labelLarge,
        ) {
            Row(
                modifier =
                    if (square) {
                        Modifier
                    } else {
                        Modifier.padding(horizontal = MaterialTheme.spacings.medium)
                    },
                horizontalArrangement =
                    if (square) Arrangement.Center
                    else Arrangement.spacedBy(MaterialTheme.spacings.small),
                verticalAlignment = Alignment.CenterVertically,
                content = content,
            )
        }
    }
}

/** The height of a label button, or both sides of an icon button. Named for where it is used. */
enum class AppButtonSize(val value: Dp) {
    /** Everywhere else in the app. */
    Default(48.dp),

    /** The player's transport controls (56dp while they remain Views). */
    Player(56.dp),

    /** Small controls that decorate content: the offset and settings steppers. */
    Decorator(32.dp),
}

/**
 * The colour treatment of a button, named for where it is used rather than what it looks like, so a
 * call site reads as its intent.
 */
enum class AppButtonVariant {
    /** The main action of a screen: a light container with dark content. */
    Primary,

    /** A secondary action on a plain surface. */
    Secondary,

    /** An action with no container of its own: top bars, steppers, anything that inherits. */
    Ghost,

    /** An action over artwork, given its own dark scrim so it stays legible. */
    Overlay,

    /** Something destructive or in an error state. */
    Destructive,
}

/**
 * How a button responds to a press. [Tint] is the default; the ripple is available, not default.
 */
enum class AppButtonEffect {
    None,
    Tint,
    Ripple,
}

/** Opacity of a disabled button's content. */
private const val DisabledAlpha = 0.38f

/** Opacity of the [AppButtonEffect.Tint] press overlay. */
private const val TintAlpha = 0.12f

/** Opacity of the [AppButtonVariant.Overlay] scrim. */
private const val ScrimAlpha = 0.7f

@Composable
private fun AppButtonVariant.containerColor(): Color =
    when (this) {
        AppButtonVariant.Primary -> MaterialTheme.colorScheme.primary
        AppButtonVariant.Secondary -> MaterialTheme.colorScheme.secondaryContainer
        AppButtonVariant.Ghost -> Color.Transparent
        AppButtonVariant.Overlay -> MaterialTheme.colorScheme.scrim.copy(alpha = ScrimAlpha)
        AppButtonVariant.Destructive -> MaterialTheme.colorScheme.errorContainer
    }

@Composable
private fun AppButtonVariant.contentColor(): Color =
    when (this) {
        AppButtonVariant.Primary -> MaterialTheme.colorScheme.onPrimary
        AppButtonVariant.Secondary -> MaterialTheme.colorScheme.onSecondaryContainer
        AppButtonVariant.Ghost -> LocalContentColor.current
        AppButtonVariant.Overlay -> OnScrim
        AppButtonVariant.Destructive -> MaterialTheme.colorScheme.onErrorContainer
    }

@Preview(showBackground = true)
@Composable
private fun AppButtonPreview() {
    FindroidTheme {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            AppButton(onClick = {}, content = { Text("Play") })
            AppButton(
                onClick = {},
                variant = AppButtonVariant.Secondary,
                content = {
                    Icon(
                        painter = painterResource(CoreR.drawable.ic_play),
                        contentDescription = null,
                    )
                    Text("Play")
                },
            )
            AppIconButton(
                onClick = {},
                variant = AppButtonVariant.Ghost,
                content = {
                    Icon(
                        painter = painterResource(CoreR.drawable.ic_tune),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                    )
                },
            )
        }
    }
}
