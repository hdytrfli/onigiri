package dev.hdytrfli.onigiri.presentation.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.hdytrfli.onigiri.R
import dev.hdytrfli.onigiri.core.R as CoreR
import dev.hdytrfli.onigiri.feature.film.presentation.components.ItemTopBar
import dev.hdytrfli.onigiri.settings.R as SettingsR
import dev.hdytrfli.onigiri.ui.components.AppButton
import dev.hdytrfli.onigiri.ui.components.AppTextField
import dev.hdytrfli.onigiri.ui.components.LanguageOption
import dev.hdytrfli.onigiri.ui.components.LanguagePickerDialog
import dev.hdytrfli.onigiri.ui.components.LoadingButton
import dev.hdytrfli.onigiri.ui.components.LocalRootPageContentInset
import dev.hdytrfli.onigiri.ui.components.TopBarPageShell
import dev.hdytrfli.onigiri.ui.theme.OnScrimSecondary
import dev.hdytrfli.onigiri.ui.theme.spacings
import dev.hdytrfli.onigiri.utils.ObserveAsEvents
import java.util.Locale

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        if (event is OnboardingEvent.Completed) onComplete()
    }

    BackHandler(enabled = state.step != OnboardingStep.OVERVIEW || state.quickConnectVisible) {
        viewModel.handleBack()
    }

    val showBack = state.step != OnboardingStep.OVERVIEW || state.quickConnectVisible
    OnboardingScaffold(
        stepIndex = state.step.ordinal,
        onBack = if (showBack) ({ viewModel.handleBack() }) else null,
    ) {
        AnimatedContent(
            targetState = if (state.quickConnectVisible) "quick-connect" else state.step.name,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "onboarding-page",
        ) { page ->
            when (page) {
                OnboardingStep.OVERVIEW.name ->
                    OverviewPage(onContinue = viewModel::continueFromOverview)
                OnboardingStep.SERVER.name ->
                    ServerPage(
                        state = state,
                        onUrlChange = viewModel::updateServerUrl,
                        onConnect = viewModel::connectServer,
                    )

                OnboardingStep.ACCOUNT.name ->
                    AccountPage(
                        state = state,
                        onUsernameChange = viewModel::updateUsername,
                        onPasswordChange = viewModel::updatePassword,
                        onLogin = viewModel::login,
                        onQuickConnect = viewModel::startQuickConnect,
                    )

                "quick-connect" ->
                    QuickConnectPage(
                        state = state,
                        onCancel = viewModel::cancelQuickConnect,
                    )

                OnboardingStep.PIN.name ->
                    PinPage(
                        state = state,
                        onPinChange = viewModel::updatePin,
                        onLock = viewModel::lockProfile,
                        onSkip = viewModel::skipPin,
                    )

                OnboardingStep.LANGUAGES.name ->
                    LanguagesPage(
                        state = state,
                        onAudioLanguageChange = viewModel::updateAudioLanguage,
                        onSubtitleLanguageChange = viewModel::updateSubtitleLanguage,
                        onAlwaysPlaySubtitlesChange = viewModel::updateAlwaysPlaySubtitles,
                        onContinue = viewModel::finishLanguages,
                    )
            }
        }
    }
}

@Composable
private fun OverviewPage(onContinue: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.medium)) {
        OnboardingHeader(
            title = stringResource(R.string.onboarding_welcome_title),
            description = stringResource(R.string.onboarding_welcome_description),
        )
        AppButton(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(R.string.onboarding_get_started))
        }
    }
}

@Composable
private fun ServerPage(
    state: OnboardingState,
    onUrlChange: (String) -> Unit,
    onConnect: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.medium)) {
        OnboardingHeader(
            title = stringResource(R.string.onboarding_server_title),
            description = stringResource(R.string.onboarding_server_description),
        )
        Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small)) {
            AppTextField(
                value = state.serverUrl,
                onValueChange = onUrlChange,
                label = stringResource(R.string.onboarding_server_url),
                hint = stringResource(R.string.onboarding_server_url_hint),
                leadingContent = {
                    Icon(
                        painter = painterResource(CoreR.drawable.ic_server),
                        contentDescription = null,
                    )
                },
                keyboardOptions =
                    KeyboardOptions(
                        autoCorrectEnabled = false,
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Go,
                    ),
                keyboardActions = KeyboardActions(onGo = { onConnect() }),
                enabled = !state.isConnecting,
                isError = state.serverError != null,
                supportingText = state.serverError?.asString(),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small)) {
            LoadingButton(
                text = stringResource(R.string.onboarding_connect),
                onClick = onConnect,
                isLoading = state.isConnecting,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = stringResource(R.string.onboarding_server_info),
                style = MaterialTheme.typography.bodySmall,
                color = OnScrimSecondary,
            )
        }
    }
}

@Composable
private fun AccountPage(
    state: OnboardingState,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLogin: () -> Unit,
    onQuickConnect: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.medium)) {
        OnboardingHeader(
            title = stringResource(R.string.onboarding_account_title),
            description = stringResource(R.string.onboarding_account_description),
        )
        AccountForm(
            username = state.username,
            password = state.password,
            isLoggingIn = state.isLoggingIn,
            loginError = state.loginError,
            onUsernameChange = onUsernameChange,
            onPasswordChange = onPasswordChange,
            onLogin = onLogin,
            onQuickConnect = onQuickConnect,
        )
    }
}

@Composable
private fun QuickConnectPage(
    state: OnboardingState,
    onCancel: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.medium)) {
        OnboardingHeader(
            title = stringResource(R.string.onboarding_quick_connect_title),
            description = stringResource(R.string.onboarding_quick_connect_description),
        )
        QuickConnectContent(
            code = state.quickConnectCode,
            error = state.quickConnectError,
            onCancel = onCancel,
        )
    }
}

@Composable
private fun PinPage(
    state: OnboardingState,
    onPinChange: (String) -> Unit,
    onLock: () -> Unit,
    onSkip: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.medium)) {
        OnboardingHeader(
            title = stringResource(R.string.onboarding_pin_title),
            description = stringResource(R.string.onboarding_pin_description),
        )
        ProfilePinContent(
            pin = state.pin,
            isSavingPin = state.isSavingPin,
            pinError = state.pinError,
            onPinChange = onPinChange,
            onLock = onLock,
            onSkip = onSkip,
        )
    }
}

@Composable
private fun LanguagesPage(
    state: OnboardingState,
    onAudioLanguageChange: (String?) -> Unit,
    onSubtitleLanguageChange: (String?) -> Unit,
    onAlwaysPlaySubtitlesChange: (Boolean) -> Unit,
    onContinue: () -> Unit,
) {
    val languageNames = stringArrayResource(SettingsR.array.languages)
    val languageValues = stringArrayResource(SettingsR.array.languages_values)
    val options =
        remember(languageNames, languageValues) {
            languageValues.mapIndexedNotNull { index, code ->
                languageNames.getOrNull(index)?.let { LanguageOption(code, it) }
            }
        }
    val labels = remember(options) { options.associateBy { it.code } }
    var picker by remember { mutableStateOf<String?>(null) }
    val deviceLanguage = remember {
        runCatching { Locale.getDefault().getISO3Language() }
            .getOrNull()
            ?.takeIf { it.isNotBlank() && it != "und" }
    }
    val suggested =
        remember(deviceLanguage, state.audioLanguage, state.subtitleLanguage) {
            listOfNotNull(deviceLanguage, state.audioLanguage, state.subtitleLanguage)
                .distinct()
                .take(5)
        }
    val notSet = stringResource(CoreR.string.not_set)

    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.medium)) {
        OnboardingHeader(
            title = stringResource(R.string.onboarding_languages_title),
            description = stringResource(R.string.onboarding_languages_description),
        )
        LanguageField(
            label = stringResource(R.string.onboarding_audio_language),
            value = labels[state.audioLanguage]?.label ?: notSet,
            onClick = { picker = "audio" },
        )
        LanguageField(
            label = stringResource(R.string.onboarding_subtitle_language),
            value = labels[state.subtitleLanguage]?.label ?: notSet,
            onClick = { picker = "subtitle" },
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.onboarding_always_subtitles),
                style = MaterialTheme.typography.bodyMedium,
                color = OnScrimSecondary,
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = state.alwaysPlaySubtitles,
                onCheckedChange = onAlwaysPlaySubtitlesChange,
            )
        }
        AppButton(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.onboarding_languages_continue))
        }
    }

    picker?.let { target ->
        LanguagePickerDialog(
            title =
                stringResource(
                    if (target == "audio") R.string.onboarding_audio_language
                    else R.string.onboarding_subtitle_language
                ),
            description = null,
            options = options,
            selectedCode = if (target == "audio") state.audioLanguage else state.subtitleLanguage,
            suggestedLabel = stringResource(CoreR.string.suggested),
            availableLabel = stringResource(CoreR.string.available),
            suggestedCodes = suggested,
            notSetLabel = notSet,
            onSelect = { code ->
                if (target == "audio") onAudioLanguageChange(code)
                else onSubtitleLanguageChange(code)
                picker = null
            },
            onDismiss = { picker = null },
        )
    }
}

@Composable
private fun LanguageField(label: String, value: String, onClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.extraSmall)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = OnScrimSecondary,
        )
        AppButton(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            variant = dev.hdytrfli.onigiri.ui.components.AppButtonVariant.Secondary,
        ) {
            Text(value)
        }
    }
}

@Composable
fun OnboardingHeader(title: String, description: String) {
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small)) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            color = Color.White,
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = OnScrimSecondary,
        )
    }
}

@Composable
fun OnboardingScaffold(
    stepIndex: Int?,
    onBack: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    TopBarPageShell(
        topBar = {
            CompositionLocalProvider(LocalContentColor provides Color.White) {
                ItemTopBar(
                    hasBackButton = onBack != null,
                    onBackClick = { onBack?.invoke() },
                )
            }
        }
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val contentMaxHeight = maxHeight * 0.76f

            Image(
                painter = painterResource(CoreR.drawable.onboarding_background),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(
                    brush = Brush.verticalGradient(colors = listOf(Color(0x33000000), Color.Black))
                )
            }

            CompositionLocalProvider(LocalContentColor provides Color.White) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier =
                            Modifier.weight(1f)
                                .fillMaxWidth()
                                .imePadding()
                                .navigationBarsPadding()
                                .padding(
                                    top = LocalRootPageContentInset.current,
                                    start = MaterialTheme.spacings.default,
                                    end = MaterialTheme.spacings.default,
                                )
                    ) {
                        Spacer(modifier = Modifier.weight(1f))
                        Column(
                            modifier =
                                Modifier.widthIn(max = 420.dp)
                                    .fillMaxWidth()
                                    .align(Alignment.CenterHorizontally)
                                    .heightIn(max = contentMaxHeight)
                                    .verticalScroll(rememberScrollState()),
                            verticalArrangement =
                                Arrangement.spacedBy(MaterialTheme.spacings.medium),
                            content = content,
                        )
                        if (stepIndex != null) {
                            OnboardingDots(
                                stepIndex = stepIndex,
                                modifier =
                                    Modifier.fillMaxWidth()
                                        .padding(
                                            top = MaterialTheme.spacings.medium,
                                            bottom = MaterialTheme.spacings.small,
                                        ),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingDots(stepIndex: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(OnboardingStep.entries.size) { index ->
            Box(
                modifier =
                    Modifier.height(6.dp)
                        .width(if (index == stepIndex) 18.dp else 6.dp)
                        .clip(CircleShape)
                        .background(
                            if (index == stepIndex) {
                                Color.White
                            } else {
                                Color.White.copy(alpha = 0.4f)
                            }
                        )
            )
        }
    }
}
