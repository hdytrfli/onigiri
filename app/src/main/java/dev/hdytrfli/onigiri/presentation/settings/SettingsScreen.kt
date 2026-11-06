package dev.hdytrfli.onigiri.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.hdytrfli.onigiri.R
import dev.hdytrfli.onigiri.core.R as CoreR
import dev.hdytrfli.onigiri.presentation.settings.components.SettingsGroupCard
import dev.hdytrfli.onigiri.settings.R as SettingsR
import dev.hdytrfli.onigiri.settings.presentation.models.PreferenceCategory
import dev.hdytrfli.onigiri.settings.presentation.models.PreferenceGroup
import dev.hdytrfli.onigiri.settings.presentation.settings.SettingsAction
import dev.hdytrfli.onigiri.settings.presentation.settings.SettingsEvent
import dev.hdytrfli.onigiri.settings.presentation.settings.SettingsState
import dev.hdytrfli.onigiri.settings.presentation.settings.SettingsViewModel
import dev.hdytrfli.onigiri.ui.components.AppButton
import dev.hdytrfli.onigiri.ui.components.AppButtonVariant
import dev.hdytrfli.onigiri.ui.components.BaseDialog
import dev.hdytrfli.onigiri.ui.components.LocalRootPageContentInset
import dev.hdytrfli.onigiri.ui.components.PageContent
import dev.hdytrfli.onigiri.ui.components.PageTopBar
import dev.hdytrfli.onigiri.ui.components.TopBarPageShell
import dev.hdytrfli.onigiri.ui.theme.FindroidTheme
import dev.hdytrfli.onigiri.ui.theme.spacings
import dev.hdytrfli.onigiri.ui.utils.plus
import dev.hdytrfli.onigiri.utils.ObserveAsEvents
import timber.log.Timber

@Composable
fun SettingsScreen(
    indexes: List<Int> = emptyList(),
    navigateToSettings: (indexes: List<Int>) -> Unit,
    navigateToSettingsFileEdit: (filePath: String) -> Unit,
    navigateToAbout: () -> Unit,
    navigateToSubtitleFont: () -> Unit,
    navigateToReaderFont: () -> Unit,
    navigateBack: () -> Unit,
    profileHeader: (@Composable () -> Unit)? = null,
    viewModel: SettingsViewModel = hiltViewModel(),
    resetViewModel: ServerResetViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    var showResetServerDialog by remember { mutableStateOf(false) }

    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(true) { viewModel.loadPreferences(indexes) }

    ObserveAsEvents(resetViewModel.events) { event ->
        if (event is ServerResetEvent.Reset) {
            context.clearProfileCachesAndRestart()
        }
    }

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is SettingsEvent.NavigateToSettings -> navigateToSettings(event.indexes)
            is SettingsEvent.NavigateToSettingsFileEdit ->
                navigateToSettingsFileEdit(event.filePath)
            is SettingsEvent.ResetServer -> showResetServerDialog = true
            is SettingsEvent.NavigateToAbout -> navigateToAbout()
            is SettingsEvent.NavigateToSubtitleFont -> navigateToSubtitleFont()
            is SettingsEvent.NavigateToReaderFont -> navigateToReaderFont()
            is SettingsEvent.LaunchIntent -> {
                try {
                    context.startActivity(event.intent)
                } catch (e: Exception) {
                    Timber.e(e)
                }
            }
        }
    }

    SettingsScreenLayout(
        state = state,
        // The pushed indexes name the page: the root is "Settings", every deeper page takes the
        // name of the category that was pushed.
        pageTitle = stringResource(indexes.lastOrNull() ?: CoreR.string.title_settings),
        onBack = if (indexes.isEmpty()) null else navigateBack,
        profileHeader = profileHeader?.takeIf { indexes.isEmpty() },
        onAction = { action ->
            viewModel.onAction(action)
            viewModel.loadPreferences(indexes)
        },
    )

    if (showResetServerDialog) {
        BaseDialog(
            title = stringResource(SettingsR.string.settings_change_server),
            description = stringResource(SettingsR.string.settings_change_server_summary),
            onDismiss = { showResetServerDialog = false },
            negativeButton = {
                AppButton(
                    onClick = { showResetServerDialog = false },
                    variant = AppButtonVariant.Secondary,
                ) {
                    Text(text = stringResource(R.string.profile_cancel))
                }
            },
            positiveButton = {
                AppButton(
                    onClick = {
                        showResetServerDialog = false
                        resetViewModel.reset()
                    },
                    variant = AppButtonVariant.Destructive,
                ) {
                    Text(text = stringResource(R.string.settings_change_server_action))
                }
            },
        ) { contentPadding ->
            Text(
                text = stringResource(R.string.settings_change_server_confirm),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(contentPadding),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreenLayout(
    state: SettingsState,
    pageTitle: String,
    onBack: (() -> Unit)?,
    profileHeader: (@Composable () -> Unit)? = null,
    onAction: (SettingsAction) -> Unit,
) {
    // Settings pages keep the navigation suite and get its bottom inset from SuiteInsetBox. The
    // shared PageContent rule fills the width up to the content cap, centres it, and applies the
    // global gutter; the floating title bar owns the top spacing.
    TopBarPageShell(topBar = { PageTopBar(title = pageTitle, onBack = onBack) }) {
        PageContent {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding =
                    PaddingValues(
                        top = LocalRootPageContentInset.current + MaterialTheme.spacings.gutter,
                        bottom = MaterialTheme.spacings.gutter,
                    ),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.medium),
            ) {
                profileHeader?.let { header -> item(key = "profile") { header() } }
                items(
                    state.preferenceGroups,
                    key = { group ->
                        group.nameStringResource
                            ?: group.preferences.firstOrNull()?.nameStringResource
                            ?: 0
                    },
                ) { group ->
                    SettingsGroupCard(
                        group = group,
                        onAction = onAction,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@PreviewScreenSizes
@Composable
private fun SettingsScreenLayoutPreview() {
    FindroidTheme {
        SettingsScreenLayout(
            state =
                SettingsState(
                    preferenceGroups =
                        listOf(
                            PreferenceGroup(
                                nameStringResource = null,
                                preferences =
                                    listOf(
                                        PreferenceCategory(
                                            nameStringResource =
                                                SettingsR.string.settings_category_language,
                                            descriptionStringRes =
                                                SettingsR.string.settings_category_language_summary,
                                            iconDrawableId = SettingsR.drawable.ic_languages,
                                        )
                                    ),
                            ),
                            PreferenceGroup(
                                nameStringResource = null,
                                preferences =
                                    listOf(
                                        PreferenceCategory(
                                            nameStringResource =
                                                SettingsR.string.settings_category_interface,
                                            descriptionStringRes =
                                                SettingsR.string
                                                    .settings_category_interface_summary,
                                            iconDrawableId = SettingsR.drawable.ic_palette,
                                        )
                                    ),
                            ),
                        )
                ),
            pageTitle = "Settings",
            onBack = null,
            onAction = {},
        )
    }
}
