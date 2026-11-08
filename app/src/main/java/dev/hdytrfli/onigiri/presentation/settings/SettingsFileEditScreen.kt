package dev.hdytrfli.onigiri.presentation.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.hdytrfli.onigiri.core.R as CoreR
import dev.hdytrfli.onigiri.settings.R as SettingsR
import dev.hdytrfli.onigiri.settings.presentation.settings.SettingsFileEditAction
import dev.hdytrfli.onigiri.settings.presentation.settings.SettingsFileEditViewModel
import dev.hdytrfli.onigiri.ui.components.AppButton
import dev.hdytrfli.onigiri.ui.components.AppButtonVariant
import dev.hdytrfli.onigiri.ui.components.BaseDialog
import dev.hdytrfli.onigiri.ui.components.LocalRootPageContentInset
import dev.hdytrfli.onigiri.ui.components.PageTopBar
import dev.hdytrfli.onigiri.ui.components.TopBarPageShell
import dev.hdytrfli.onigiri.ui.theme.FindroidTheme

@Composable
fun SettingsFileEditScreen(
    filePath: String,
    navigateBack: () -> Unit,
    viewModel: SettingsFileEditViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(filePath) { viewModel.loadFile(filePath = filePath) }

    SettingsFileEditScreenLayout(
        initialText = state.initialText,
        fileName = filePath.substringAfterLast('/'),
        onAction = { action ->
            when (action) {
                is SettingsFileEditAction.OnBackClick -> navigateBack()
                else -> viewModel.onAction(action)
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsFileEditScreenLayout(
    initialText: String,
    fileName: String,
    onAction: (SettingsFileEditAction) -> Unit,
) {
    val textFieldState = rememberTextFieldState()
    val isModified = initialText != textFieldState.text

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(initialText) {
        textFieldState.setTextAndPlaceCursorAtEnd(initialText)
        focusRequester.requestFocus()
    }

    var showDiscardDialog by remember { mutableStateOf(false) }
    BackHandler(isModified) { showDiscardDialog = true }

    TopBarPageShell(
        modifier = Modifier.imePadding(),
        topBar = {
            PageTopBar(
                title = stringResource(SettingsR.string.edit_file_title, fileName),
                onBack = {
                    if (isModified) showDiscardDialog = true
                    else onAction(SettingsFileEditAction.OnBackClick)
                },
                trailingContent = {
                    AppButton(
                        onClick = {
                            val text = textFieldState.text.toString()
                            onAction(SettingsFileEditAction.OnSave(text))
                        },
                        enabled = isModified,
                    ) {
                        Icon(
                            painter = painterResource(CoreR.drawable.ic_save),
                            contentDescription = null,
                        )
                        Text(stringResource(SettingsR.string.save))
                    }
                },
            )
        },
    ) {
        TextField(
            state = textFieldState,
            modifier =
                Modifier.fillMaxSize()
                    .padding(top = LocalRootPageContentInset.current)
                    .focusRequester(focusRequester),
            textStyle =
                TextStyle(
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                ),
            keyboardOptions = KeyboardOptions(autoCorrectEnabled = false),
            colors =
                TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
        )
    }

    if (showDiscardDialog) {
        BaseDialog(
            title = stringResource(SettingsR.string.discard_changes_dialog_title),
            description = stringResource(SettingsR.string.discard_changes_dialog_text),
            onDismiss = { showDiscardDialog = false },
            negativeButton = {
                AppButton(
                    onClick = { showDiscardDialog = false },
                    variant = AppButtonVariant.Secondary,
                ) {
                    Text(stringResource(CoreR.string.cancel))
                }
            },
            positiveButton = {
                AppButton(
                    onClick = { onAction(SettingsFileEditAction.OnBackClick) },
                    variant = AppButtonVariant.Destructive,
                ) {
                    Text(stringResource(CoreR.string.discard))
                }
            },
        ) { contentPadding ->
            Text(
                text = stringResource(SettingsR.string.discard_changes_dialog_text),
                modifier = Modifier.padding(contentPadding),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
@PreviewScreenSizes
private fun SettingsFileEditScreenLayoutPreview() {
    FindroidTheme {
        SettingsFileEditScreenLayout(
            initialText = "sample text",
            fileName = "preferences.xml",
            onAction = {},
        )
    }
}
