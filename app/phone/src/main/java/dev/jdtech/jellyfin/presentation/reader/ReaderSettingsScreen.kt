package dev.jdtech.jellyfin.presentation.reader

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.jdtech.jellyfin.R as AppR
import dev.jdtech.jellyfin.presentation.components.PageContent
import dev.jdtech.jellyfin.presentation.components.PageScaffold
import dev.jdtech.jellyfin.presentation.components.PageTopBar
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme

/** The same reader preferences as the in-reader sheet, reachable from app Settings. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderSettingsScreen(
    navigateBack: () -> Unit,
    viewModel: ReaderSettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    PageScaffold(
        topBar = {
            PageTopBar(
                title = stringResource(AppR.string.reader_settings),
                onBack = navigateBack,
            )
        }
    ) { innerPadding ->
        PageContent(modifier = Modifier.padding(innerPadding)) {
            ReaderSettingsContent(
                settings = settings,
                onSettingsChange = viewModel::update,
            )
        }
    }
}

@PreviewScreenSizes
@Composable
private fun ReaderSettingsScreenPreview() {
    FindroidTheme {
        // The preview exercises the shared editor without a ViewModel.
        ReaderSettingsContent(settings = ReaderSettings(), onSettingsChange = {})
    }
}
