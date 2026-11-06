package dev.hdytrfli.onigiri.presentation.reader

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.hdytrfli.onigiri.R as AppR
import dev.hdytrfli.onigiri.core.R as CoreR
import dev.hdytrfli.onigiri.settings.domain.SubtitleFonts
import dev.hdytrfli.onigiri.ui.components.LocalRootPageContentInset
import dev.hdytrfli.onigiri.ui.components.PageTopBar
import dev.hdytrfli.onigiri.ui.components.TopBarPageShell
import dev.hdytrfli.onigiri.ui.theme.Dimens
import dev.hdytrfli.onigiri.ui.theme.FindroidTheme
import dev.hdytrfli.onigiri.ui.theme.spacings
import dev.hdytrfli.onigiri.ui.utils.plus

/** Reader font choice, using the same font list and preview style as the subtitle font setting. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderFontScreen(
    navigateBack: () -> Unit,
    viewModel: ReaderFontViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val fontsDir = remember { SubtitleFonts.fontsDir(context) }

    val defaultFonts = state.fonts.filter { it.family == SubtitleFonts.DEFAULT_FAMILY }
    val bundledFonts =
        state.fonts.filter {
            it.family != SubtitleFonts.DEFAULT_FAMILY && it.file?.parentFile == fontsDir
        }
    val systemFonts =
        state.fonts.filter {
            it.family != SubtitleFonts.DEFAULT_FAMILY && it.file?.parentFile != fontsDir
        }

    val contentPadding = PaddingValues(all = MaterialTheme.spacings.gutter)

    TopBarPageShell(
        topBar = {
            PageTopBar(
                title = stringResource(AppR.string.reader_font_title),
                onBack = navigateBack,
            )
        }
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding =
                contentPadding + PaddingValues(top = LocalRootPageContentInset.current),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.medium),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (defaultFonts.isNotEmpty()) {
                item {
                    FontGroup(
                        titleRes = AppR.string.subtitle_font_section_default,
                        fonts = defaultFonts,
                        selectedFamily = state.selectedFamily,
                        onSelect = viewModel::select,
                        modifier = Modifier.widthIn(max = Dimens.contentMaxWidth),
                    )
                }
            }
            if (bundledFonts.isNotEmpty()) {
                item {
                    FontGroup(
                        titleRes = AppR.string.subtitle_font_section_bundled,
                        fonts = bundledFonts,
                        selectedFamily = state.selectedFamily,
                        onSelect = viewModel::select,
                        modifier = Modifier.widthIn(max = Dimens.contentMaxWidth),
                    )
                }
            }
            if (systemFonts.isNotEmpty()) {
                item {
                    FontGroup(
                        titleRes = AppR.string.subtitle_font_section_system,
                        fonts = systemFonts,
                        selectedFamily = state.selectedFamily,
                        onSelect = viewModel::select,
                        modifier = Modifier.widthIn(max = Dimens.contentMaxWidth),
                    )
                }
            }
            item {
                Text(
                    text = stringResource(AppR.string.reader_font_note),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier =
                        Modifier.widthIn(max = Dimens.contentMaxWidth)
                            .padding(horizontal = MaterialTheme.spacings.gutter),
                )
            }
        }
    }
}

@Composable
private fun FontGroup(
    @StringRes titleRes: Int,
    fonts: List<SubtitleFonts.Font>,
    selectedFamily: String,
    onSelect: (SubtitleFonts.Font) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column {
        Text(
            text = stringResource(titleRes),
            modifier = Modifier.padding(start = MaterialTheme.spacings.medium),
            style = MaterialTheme.typography.titleSmall,
        )
        Spacer(modifier = Modifier.height(MaterialTheme.spacings.small))
        Card(
            modifier = modifier,
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
        ) {
            fonts.fastForEachIndexed { index, font ->
                FontRow(
                    font = font,
                    selected = font.family == selectedFamily,
                    onSelect = onSelect,
                )
                if (index < fonts.lastIndex) {
                    HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.2f))
                }
            }
        }
    }
}

@Composable
private fun FontRow(
    font: SubtitleFonts.Font,
    selected: Boolean,
    onSelect: (SubtitleFonts.Font) -> Unit,
) {
    val fontFamily =
        remember(font.file) {
            font.file?.let { file ->
                runCatching { FontFamily(android.graphics.Typeface.createFromFile(file)) }
                    .getOrNull()
            } ?: FontFamily.SansSerif
        }

    Row(
        modifier =
            Modifier.fillMaxWidth()
                .clickable { onSelect(font) }
                .padding(MaterialTheme.spacings.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = font.displayName,
            fontFamily = fontFamily,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            Icon(
                painter = painterResource(CoreR.drawable.ic_check),
                contentDescription = stringResource(AppR.string.subtitle_font_selected),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@PreviewScreenSizes
@Composable
private fun ReaderFontScreenPreview() {
    FindroidTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = "Geist", fontFamily = FontFamily.SansSerif)
            Text(text = "Libre Baskerville", fontFamily = FontFamily.Serif)
            Text(text = "Vollkorn", fontFamily = FontFamily.Serif)
        }
    }
}
