package dev.jdtech.jellyfin.presentation.settings

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.jdtech.jellyfin.core.presentation.theme.Dimens
import dev.jdtech.jellyfin.presentation.components.PageScaffold
import dev.jdtech.jellyfin.presentation.settings.components.SettingsGroupCard
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.presentation.utils.plus
import dev.jdtech.jellyfin.settings.R as SettingsR
import dev.jdtech.jellyfin.settings.presentation.models.PreferenceCategory
import dev.jdtech.jellyfin.settings.presentation.models.PreferenceGroup
import dev.jdtech.jellyfin.settings.presentation.settings.SettingsAction
import dev.jdtech.jellyfin.settings.presentation.settings.SettingsEvent
import dev.jdtech.jellyfin.settings.presentation.settings.SettingsState
import dev.jdtech.jellyfin.settings.presentation.settings.SettingsViewModel
import dev.jdtech.jellyfin.utils.ObserveAsEvents
import dev.jdtech.jellyfin.utils.restart
import timber.log.Timber

@Composable
fun SettingsScreen(
    indexes: List<Int> = emptyList(),
    navigateToSettings: (indexes: List<Int>) -> Unit,
    navigateToSettingsFileEdit: (filePath: String) -> Unit,
    navigateToServers: () -> Unit,
    navigateToUsers: () -> Unit,
    navigateToAbout: () -> Unit,
    navigateToSubtitleFont: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current

    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(true) { viewModel.loadPreferences(indexes) }

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is SettingsEvent.NavigateToSettings -> navigateToSettings(event.indexes)
            is SettingsEvent.NavigateToSettingsFileEdit ->
                navigateToSettingsFileEdit(event.filePath)
            is SettingsEvent.NavigateToUsers -> navigateToUsers()
            is SettingsEvent.NavigateToServers -> navigateToServers()
            is SettingsEvent.NavigateToAbout -> navigateToAbout()
            is SettingsEvent.NavigateToSubtitleFont -> navigateToSubtitleFont()
            is SettingsEvent.LaunchIntent -> {
                try {
                    context.startActivity(event.intent)
                } catch (e: Exception) {
                    Timber.e(e)
                }
            }
            is SettingsEvent.RestartActivity -> {
                try {
                    (context as Activity).restart()
                } catch (_: Exception) {}
            }
        }
    }

    SettingsScreenLayout(
        state = state,
        onAction = { action ->
            viewModel.onAction(action)
            viewModel.loadPreferences(indexes)
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreenLayout(
    state: SettingsState,
    onAction: (SettingsAction) -> Unit,
) {
    // Settings pages keep the navigation suite and get its bottom inset from SuiteInsetBox,
    // so this list only adds its own gutter and never reserves the suite's space again.
    val contentPadding = PaddingValues(all = MaterialTheme.spacings.gutter)

    PageScaffold { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = contentPadding + innerPadding,
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.medium),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            items(state.preferenceGroups) { group ->
                SettingsGroupCard(
                    group = group,
                    onAction = onAction,
                    modifier = Modifier.widthIn(max = Dimens.contentMaxWidth),
                )
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
                                            iconDrawableId = SettingsR.drawable.ic_palette,
                                        )
                                    ),
                            ),
                        )
                ),
            onAction = {},
        )
    }
}
