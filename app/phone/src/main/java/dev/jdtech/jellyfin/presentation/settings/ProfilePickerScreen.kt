package dev.jdtech.jellyfin.presentation.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.jdtech.jellyfin.R
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.presentation.components.AppButton
import dev.jdtech.jellyfin.presentation.components.AppButtonVariant
import dev.jdtech.jellyfin.presentation.components.AppDropdownMenu
import dev.jdtech.jellyfin.presentation.components.AppIconButton
import dev.jdtech.jellyfin.presentation.components.AppTextField
import dev.jdtech.jellyfin.presentation.components.BaseDialog
import dev.jdtech.jellyfin.presentation.components.MenuActionItem
import dev.jdtech.jellyfin.presentation.film.components.ItemTopBar
import dev.jdtech.jellyfin.presentation.onboarding.PinCodeField
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.presentation.utils.GridSize
import dev.jdtech.jellyfin.presentation.utils.LocalGridGap
import dev.jdtech.jellyfin.presentation.utils.gridColumns
import dev.jdtech.jellyfin.settings.R as SettingsR
import dev.jdtech.jellyfin.utils.ObserveAsEvents

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilePickerScreen(
    onBack: (() -> Unit)?,
    onAddProfile: () -> Unit,
    canGoBack: Boolean,
    viewModel: ProfilePickerViewModel = hiltViewModel(),
    resetViewModel: ServerResetViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showChangeServerDialog by remember { mutableStateOf(false) }

    ObserveAsEvents(viewModel.events) { event ->
        if (event is ProfilePickerEvent.Switched) {
            context.clearProfileCachesAndRestart()
        }
    }
    ObserveAsEvents(resetViewModel.events) { event ->
        if (event is ServerResetEvent.Reset) {
            context.clearProfileCachesAndRestart()
        }
    }

    BackHandler(enabled = !canGoBack) {}

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(CoreR.drawable.onboarding_background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)))

        Column(modifier = Modifier.fillMaxSize()) {
            ItemTopBar(
                hasBackButton = canGoBack && onBack != null,
                onBackClick = { onBack?.invoke() },
                trailingContent = {
                    AppIconButton(
                        onClick = { showChangeServerDialog = true },
                        variant = AppButtonVariant.Secondary,
                    ) {
                        Icon(
                            painter = painterResource(CoreR.drawable.ic_server),
                            contentDescription =
                                stringResource(SettingsR.string.settings_change_server),
                        )
                    }
                },
            )
            ProfilePickerContent(
                state = state,
                onProfileClick = { profile ->
                    if (profile.isCurrent && canGoBack) Unit else viewModel.onProfileClick(profile)
                },
                onAddProfile = onAddProfile,
                onDismissPin = viewModel::dismissPin,
                onSubmitPin = viewModel::submitPin,
                onForgotPin = viewModel::showForgotPin,
                onDismissForgot = viewModel::dismissForgotPin,
                onSubmitPassword = viewModel::submitPassword,
                onRemoveProfile = viewModel::showRemoveProfile,
                onDismissRemove = viewModel::dismissRemoveProfile,
                onConfirmRemove = viewModel::confirmRemoveProfile,
                onChangePin = viewModel::showPinEditor,
                onUnlockProfile = viewModel::unlockProfile,
                onDismissPinEditor = viewModel::dismissPinEditor,
                onSavePin = viewModel::saveProfilePin,
                modifier =
                    Modifier.weight(1f)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = MaterialTheme.spacings.gutter),
            )
        }
    }

    if (showChangeServerDialog) {
        BaseDialog(
            title = stringResource(SettingsR.string.settings_change_server),
            description = stringResource(R.string.settings_change_server_confirm),
            onDismiss = { showChangeServerDialog = false },
            negativeButton = {
                AppButton(
                    onClick = { showChangeServerDialog = false },
                    variant = AppButtonVariant.Secondary,
                ) {
                    Text(text = stringResource(R.string.profile_cancel))
                }
            },
            positiveButton = {
                AppButton(
                    onClick = {
                        showChangeServerDialog = false
                        resetViewModel.reset()
                    },
                    variant = AppButtonVariant.Destructive,
                ) {
                    Text(text = stringResource(R.string.settings_change_server_action))
                }
            },
        )
    }
}

private sealed interface ProfileCell {
    data class Profile(val item: ProfileItem) : ProfileCell

    data object Add : ProfileCell
}

@Composable
private fun ProfilePickerContent(
    state: ProfilePickerState,
    onProfileClick: (ProfileItem) -> Unit,
    onAddProfile: () -> Unit,
    onDismissPin: () -> Unit,
    onSubmitPin: (String) -> Unit,
    onForgotPin: (ProfileItem) -> Unit,
    onDismissForgot: () -> Unit,
    onSubmitPassword: (String) -> Unit,
    onRemoveProfile: (ProfileItem) -> Unit,
    onDismissRemove: () -> Unit,
    onConfirmRemove: () -> Unit,
    onChangePin: (ProfileItem) -> Unit,
    onUnlockProfile: (ProfileItem) -> Unit,
    onDismissPinEditor: () -> Unit,
    onSavePin: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val configuredColumns = gridColumns(GridSize.PROFILE)
    val gap = LocalGridGap.current

    if (state.isLoading) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color.White)
        }
    } else {
        androidx.compose.foundation.layout.BoxWithConstraints(modifier = modifier) {
            // Keep the 96dp avatars from overlapping; the configured column count is the target.
            val maxColumns = (maxWidth / 96.dp).toInt().coerceAtLeast(1)
            val columns = configuredColumns.coerceIn(1, maxColumns)
            val cellWidth = (maxWidth - gap * (columns - 1).toFloat()) / columns.toFloat()
            val hasCurrentUser = state.profiles.any { it.isCurrent }
            val cells =
                buildList<ProfileCell> {
                    state.profiles.forEach { add(ProfileCell.Profile(it)) }
                    add(ProfileCell.Add)
                }

            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start,
            ) {
                cells.chunked(columns).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.spacedBy(gap, Alignment.CenterHorizontally),
                    ) {
                        row.forEach { cell ->
                            when (cell) {
                                is ProfileCell.Profile ->
                                    ProfileCard(
                                        profile = cell.item,
                                        baseUrl = state.baseUrl,
                                        width = cellWidth,
                                        onClick = { onProfileClick(cell.item) },
                                        onRemove =
                                            if (
                                                hasCurrentUser &&
                                                    (state.viewerIsAdmin || cell.item.isCurrent)
                                            ) {
                                                { onRemoveProfile(cell.item) }
                                            } else {
                                                null
                                            },
                                        onChangePin =
                                            if (
                                                hasCurrentUser &&
                                                    (state.viewerIsAdmin || cell.item.isCurrent)
                                            ) {
                                                { onChangePin(cell.item) }
                                            } else {
                                                null
                                            },
                                        onUnlock =
                                            if (
                                                hasCurrentUser &&
                                                    cell.item.isLocked &&
                                                    (state.viewerIsAdmin || cell.item.isCurrent)
                                            ) {
                                                { onUnlockProfile(cell.item) }
                                            } else {
                                                null
                                            },
                                        showActions =
                                            hasCurrentUser &&
                                                (state.viewerIsAdmin || cell.item.isCurrent),
                                    )

                                ProfileCell.Add ->
                                    AddProfileCard(width = cellWidth, onClick = onAddProfile)
                            }
                        }
                    }
                }
            }
        }
    }

    state.pinPrompt?.let { profile ->
        var pin by remember(profile.id) { mutableStateOf("") }
        BaseDialog(
            title = stringResource(R.string.profile_pin_title),
            description = stringResource(R.string.profile_pin_description, profile.name),
            onDismiss = onDismissPin,
            negativeButton = {
                AppButton(
                    onClick = { onForgotPin(profile) },
                    variant = AppButtonVariant.Secondary,
                ) {
                    Text(text = stringResource(R.string.profile_forgot_pin))
                }
            },
            positiveButton = {
                AppButton(onClick = { onSubmitPin(pin) }) {
                    Text(text = stringResource(R.string.profile_pin_unlock))
                }
            },
        ) { contentPadding ->
            Column(
                modifier = Modifier.padding(contentPadding),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small),
            ) {
                PinCodeField(
                    value = pin,
                    onValueChange = { pin = it },
                    length = 4,
                    isError = state.pinError != null,
                )
                state.pinError?.let {
                    Text(
                        text = it.asString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }

    state.removePrompt?.let { profile ->
        BaseDialog(
            title = stringResource(R.string.profile_remove_title),
            description = stringResource(R.string.profile_remove_description, profile.name),
            onDismiss = onDismissRemove,
            negativeButton = {
                AppButton(onClick = onDismissRemove, variant = AppButtonVariant.Secondary) {
                    Text(text = stringResource(R.string.profile_cancel))
                }
            },
            positiveButton = {
                AppButton(
                    onClick = onConfirmRemove,
                    variant = AppButtonVariant.Destructive,
                    enabled = !state.isBusy,
                ) {
                    Text(text = stringResource(R.string.profile_remove_action))
                }
            },
        )
    }

    state.pinEditorPrompt?.let { profile ->
        var pin by rememberSaveable(profile.id) { mutableStateOf("") }
        BaseDialog(
            title =
                stringResource(
                    if (profile.isLocked) {
                        R.string.profile_change_lock
                    } else {
                        R.string.profile_add_lock
                    }
                ),
            description = stringResource(R.string.profile_pin_description, profile.name),
            onDismiss = onDismissPinEditor,
            negativeButton = {
                AppButton(onClick = onDismissPinEditor, variant = AppButtonVariant.Secondary) {
                    Text(text = stringResource(R.string.profile_cancel))
                }
            },
            positiveButton = {
                AppButton(
                    onClick = { onSavePin(pin) },
                    enabled = !state.isBusy,
                ) {
                    Text(text = stringResource(R.string.profile_save_pin))
                }
            },
        ) { contentPadding ->
            Column(
                modifier = Modifier.padding(contentPadding),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small),
            ) {
                PinCodeField(
                    value = pin,
                    onValueChange = { pin = it },
                    length = 4,
                    isError = state.pinEditorError != null,
                )
                state.pinEditorError?.let {
                    Text(
                        text = it.asString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }

    state.forgotPrompt?.let { profile ->
        var password by rememberSaveable(profile.id) { mutableStateOf("") }
        BaseDialog(
            title = stringResource(R.string.profile_password_title),
            description = stringResource(R.string.profile_password_description, profile.name),
            onDismiss = onDismissForgot,
            negativeButton = {
                AppButton(onClick = onDismissForgot, variant = AppButtonVariant.Secondary) {
                    Text(text = stringResource(R.string.profile_cancel))
                }
            },
            positiveButton = {
                AppButton(
                    onClick = { onSubmitPassword(password) },
                    enabled = !state.isBusy,
                ) {
                    Text(text = stringResource(R.string.profile_password_sign_in))
                }
            },
        ) { contentPadding ->
            Column(
                modifier = Modifier.padding(contentPadding),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small),
            ) {
                AppTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = stringResource(R.string.onboarding_password),
                    visualTransformation = PasswordVisualTransformation(),
                    enabled = !state.isBusy,
                    isError = state.forgotError != null,
                    supportingText = state.forgotError?.asString(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun ProfileCard(
    profile: ProfileItem,
    baseUrl: String?,
    width: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
    onRemove: (() -> Unit)?,
    onChangePin: (() -> Unit)?,
    onUnlock: (() -> Unit)?,
    showActions: Boolean,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = Modifier.width(width).padding(vertical = MaterialTheme.spacings.small),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small),
    ) {
        Box {
            ProfileAvatar(
                avatarUrl = baseUrl?.let { "$it/Users/${profile.id}/Images/Primary?maxWidth=200" },
                accessToken = profile.accessToken,
                size = 96.dp,
                modifier =
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick,
                    ),
            )
            if (profile.isLocked) {
                Box(
                    modifier =
                        Modifier.align(Alignment.BottomEnd)
                            .size(24.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceContainerHigh,
                                CircleShape,
                            )
                            .padding(5.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(CoreR.drawable.ic_lock),
                        contentDescription = stringResource(R.string.profile_locked),
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.extraSmall),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = profile.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                ProfileRoleBadge(
                    isAdmin = profile.isAdmin,
                    modifier = Modifier.padding(top = MaterialTheme.spacings.extraSmall),
                )
            }
            if (showActions && (onChangePin != null || onUnlock != null || onRemove != null)) {
                Box {
                    AppIconButton(
                        onClick = { menuExpanded = true },
                        variant = AppButtonVariant.Ghost,
                    ) {
                        Icon(
                            painter = painterResource(CoreR.drawable.ic_ellipsis),
                            contentDescription = stringResource(R.string.profile_options),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    AppDropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        alignEnd = true,
                    ) {
                        if (onChangePin != null) {
                            MenuActionItem(
                                icon = CoreR.drawable.ic_lock,
                                label =
                                    stringResource(
                                        if (profile.isLocked) {
                                            R.string.profile_change_lock
                                        } else {
                                            R.string.profile_add_lock
                                        }
                                    ),
                                onClick = {
                                    menuExpanded = false
                                    onChangePin()
                                },
                            )
                        }
                        if (onUnlock != null) {
                            MenuActionItem(
                                icon = CoreR.drawable.ic_lock_open,
                                label = stringResource(R.string.profile_unlock),
                                onClick = {
                                    menuExpanded = false
                                    onUnlock()
                                },
                            )
                        }
                        if (onRemove != null) {
                            MenuActionItem(
                                icon = CoreR.drawable.ic_trash,
                                label = stringResource(R.string.profile_remove),
                                onClick = {
                                    menuExpanded = false
                                    onRemove()
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddProfileCard(width: androidx.compose.ui.unit.Dp, onClick: () -> Unit) {
    Column(
        modifier = Modifier.width(width).padding(vertical = MaterialTheme.spacings.small),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small),
    ) {
        Box(
            modifier =
                Modifier.size(96.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.14f))
                    .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(CoreR.drawable.ic_plus),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(32.dp),
            )
        }
        Text(
            text = stringResource(R.string.profile_add),
            style = MaterialTheme.typography.titleSmall,
            color = Color.White,
        )
    }
}
