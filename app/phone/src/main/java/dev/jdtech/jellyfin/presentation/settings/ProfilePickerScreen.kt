package dev.jdtech.jellyfin.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.jdtech.jellyfin.R
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.presentation.components.AppButton
import dev.jdtech.jellyfin.presentation.components.AppButtonVariant
import dev.jdtech.jellyfin.presentation.components.AppTextField
import dev.jdtech.jellyfin.presentation.components.BaseDialog
import dev.jdtech.jellyfin.presentation.components.PageScaffold
import dev.jdtech.jellyfin.presentation.components.PageTopBar
import dev.jdtech.jellyfin.presentation.onboarding.PinCodeField
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.presentation.utils.GridSize
import dev.jdtech.jellyfin.presentation.utils.LocalGridGap
import dev.jdtech.jellyfin.presentation.utils.gridColumns
import dev.jdtech.jellyfin.utils.ObserveAsEvents

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilePickerScreen(
    onBack: () -> Unit,
    onAddProfile: () -> Unit,
    viewModel: ProfilePickerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    ObserveAsEvents(viewModel.events) { event ->
        if (event is ProfilePickerEvent.Switched) {
            context.clearProfileCachesAndRestart()
        }
    }

    PageScaffold(
        topBar = {
            PageTopBar(
                title = stringResource(R.string.profile_title),
                onBack = onBack,
            )
        }
    ) { innerPadding ->
        ProfilePickerContent(
            state = state,
            onProfileClick = viewModel::onProfileClick,
            onAddProfile = onAddProfile,
            onDismissPin = viewModel::dismissPin,
            onSubmitPin = viewModel::submitPin,
            onForgotPin = viewModel::showForgotPin,
            onDismissForgot = viewModel::dismissForgotPin,
            onSubmitPassword = viewModel::submitPassword,
            modifier =
                Modifier.fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = MaterialTheme.spacings.gutter),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
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
    modifier: Modifier = Modifier,
) {
    val columns = gridColumns(GridSize.PROFILE)
    val gap = LocalGridGap.current

    if (state.isLoading) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        BoxWithConstraints(modifier = modifier) {
            val cellWidth = (maxWidth - gap * (columns - 1).toFloat()) / columns.toFloat()

            Column(
                modifier =
                    Modifier.fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = MaterialTheme.spacings.large),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = stringResource(R.string.profile_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = MaterialTheme.spacings.large),
                )
                if (state.profiles.isEmpty()) {
                    Text(
                        text = stringResource(R.string.profile_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = MaterialTheme.spacings.large),
                    )
                }
                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(top = MaterialTheme.spacings.large),
                    horizontalArrangement = Arrangement.spacedBy(gap, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(gap),
                    maxItemsInEachRow = columns,
                ) {
                    state.profiles.forEach { profile ->
                        ProfileCard(
                            profile = profile,
                            baseUrl = state.baseUrl,
                            width = cellWidth,
                            onClick = { onProfileClick(profile) },
                        )
                    }
                    AddProfileCard(width = cellWidth, onClick = onAddProfile)
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
                AppButton(onClick = onDismissPin, variant = AppButtonVariant.Secondary) {
                    Text(text = stringResource(R.string.profile_cancel))
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
                AppButton(
                    onClick = { onForgotPin(profile) },
                    variant = AppButtonVariant.Ghost,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = stringResource(R.string.profile_forgot_pin))
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
                    Text(text = stringResource(R.string.profile_password_unlock))
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
) {
    Column(
        modifier =
            Modifier.width(width)
                .clickable(onClick = onClick)
                .padding(vertical = MaterialTheme.spacings.small),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small),
    ) {
        Box {
            ProfileAvatar(
                avatarUrl = baseUrl?.let { "$it/Users/${profile.id}/Images/Primary?maxWidth=200" },
                accessToken = profile.accessToken,
                size = 72.dp,
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
            if (profile.isCurrent) {
                Box(
                    modifier =
                        Modifier.align(Alignment.TopStart)
                            .size(24.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                            .padding(5.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(CoreR.drawable.ic_check),
                        contentDescription = stringResource(R.string.profile_current),
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
        Text(
            text = profile.name,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (profile.isAdmin) {
            ProfileAdminBadge()
        }
    }
}

@Composable
private fun AddProfileCard(width: androidx.compose.ui.unit.Dp, onClick: () -> Unit) {
    Column(
        modifier =
            Modifier.width(width)
                .clickable(onClick = onClick)
                .padding(vertical = MaterialTheme.spacings.small),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small),
    ) {
        Box(
            modifier =
                Modifier.size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(CoreR.drawable.ic_plus),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(32.dp),
            )
        }
        Text(
            text = stringResource(R.string.profile_add),
            style = MaterialTheme.typography.titleSmall,
            textAlign = TextAlign.Center,
        )
    }
}
