package dev.jdtech.jellyfin.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
import dev.jdtech.jellyfin.presentation.components.BaseDialog
import dev.jdtech.jellyfin.presentation.components.MenuActionItem
import dev.jdtech.jellyfin.presentation.onboarding.PinCodeField
import dev.jdtech.jellyfin.presentation.theme.spacings

@Composable
fun SettingsProfileHeader(
    onLogoutProfile: () -> Unit,
    onChangeProfile: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileHeaderViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var menuExpanded by remember { mutableStateOf(false) }
    var showPinEditor by remember { mutableStateOf(false) }

    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = MaterialTheme.spacings.small),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProfileAvatar(
            avatarUrl = state.avatarUrl,
            accessToken = state.accessToken,
            size = 96.dp,
        )
        Column(modifier = Modifier.weight(1f)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = state.userName.orEmpty(),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (state.isAdmin) {
                    ProfileAdminBadge()
                }
            }
            Text(
                text = state.serverName.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box {
            AppIconButton(
                onClick = { menuExpanded = true },
                variant = AppButtonVariant.Secondary,
            ) {
                Icon(
                    painter = painterResource(CoreR.drawable.ic_ellipsis),
                    contentDescription = stringResource(R.string.profile_options),
                )
            }
            AppDropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                alignEnd = true,
            ) {
                if (state.userName != null) {
                    MenuActionItem(
                        icon = CoreR.drawable.ic_lock,
                        label =
                            stringResource(
                                if (state.isLocked) {
                                    R.string.profile_change_lock
                                } else {
                                    R.string.profile_add_lock
                                }
                            ),
                        onClick = {
                            menuExpanded = false
                            showPinEditor = true
                        },
                    )
                }
                MenuActionItem(
                    icon = CoreR.drawable.ic_user,
                    label = stringResource(R.string.profile_switch),
                    onClick = {
                        menuExpanded = false
                        onChangeProfile()
                    },
                )
                MenuActionItem(
                    icon = CoreR.drawable.ic_logout,
                    label = stringResource(R.string.profile_logout),
                    onClick = {
                        menuExpanded = false
                        onLogoutProfile()
                    },
                )
            }
        }
    }

    if (showPinEditor) {
        var pin by rememberSaveable { mutableStateOf("") }
        BaseDialog(
            title =
                stringResource(
                    if (state.isLocked) R.string.profile_change_lock else R.string.profile_add_lock
                ),
            description =
                stringResource(R.string.profile_pin_description, state.userName.orEmpty()),
            onDismiss = { showPinEditor = false },
            negativeButton = {
                AppButton(
                    onClick = { showPinEditor = false },
                    variant = AppButtonVariant.Secondary,
                ) {
                    Text(text = stringResource(R.string.profile_cancel))
                }
            },
            positiveButton = {
                AppButton(
                    onClick = {
                        viewModel.savePin(pin)
                        showPinEditor = false
                    },
                    enabled = pin.length == 4,
                ) {
                    Text(text = stringResource(R.string.profile_save_pin))
                }
            },
        ) { contentPadding ->
            Column(modifier = Modifier.padding(contentPadding)) {
                PinCodeField(
                    value = pin,
                    onValueChange = { pin = it.filter(Char::isDigit).take(4) },
                    length = 4,
                )
            }
        }
    }
}
