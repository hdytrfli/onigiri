package dev.jdtech.jellyfin.presentation.settings

import android.graphics.Typeface
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.util.fastForEachIndexed
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.jdtech.jellyfin.R
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.core.presentation.theme.Dimens
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.presentation.utils.plus
import dev.jdtech.jellyfin.presentation.utils.topBarWindowInsets
import dev.jdtech.jellyfin.settings.domain.SubtitleFonts
import dev.jdtech.jellyfin.utils.ObserveAsEvents

@Composable
fun SubtitleFontScreen(
    navigateBack: () -> Unit,
    viewModel: SubtitleFontViewModel = hiltViewModel(),
) {
    val context = LocalContext.current

    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is SubtitleFontEvent.InvalidFontFile ->
                Toast.makeText(context, R.string.subtitle_font_import_error, Toast.LENGTH_SHORT)
                    .show()
        }
    }

    // Font files are frequently reported as "application/octet-stream", so no mime filter.
    val importLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let(viewModel::importFont)
        }

    SubtitleFontScreenLayout(
        fonts = state.fonts,
        selectedFamily = state.selectedFamily,
        onBack = navigateBack,
        onSelect = viewModel::selectFont,
        onDelete = viewModel::deleteFont,
        onImport = { importLauncher.launch(arrayOf("*/*")) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubtitleFontScreenLayout(
    fonts: List<SubtitleFonts.Font>,
    selectedFamily: String,
    onBack: () -> Unit,
    onSelect: (SubtitleFonts.Font) -> Unit,
    onDelete: (SubtitleFonts.Font) -> Unit,
    onImport: () -> Unit,
) {
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val contentPadding = PaddingValues(all = MaterialTheme.spacings.default)
    val fontsDir = remember { SubtitleFonts.fontsDir(context) }

    val defaultFonts = fonts.filter { it.family == SubtitleFonts.DEFAULT_FAMILY }
    // Bundled and imported fonts both live in the mpv font directory; only imported ones can be
    // removed, which is what separates them.
    val importedFonts = fonts.filter { it.removable }
    val bundledFonts = fonts.filter {
        !it.removable &&
            it.family != SubtitleFonts.DEFAULT_FAMILY &&
            it.file?.parentFile == fontsDir
    }
    val systemFonts = fonts.filter {
        !it.removable &&
            it.family != SubtitleFonts.DEFAULT_FAMILY &&
            it.file?.parentFile != fontsDir
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.subtitle_font_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(CoreR.drawable.ic_arrow_left),
                            contentDescription = null,
                        )
                    }
                },
                windowInsets = topBarWindowInsets(),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding + innerPadding,
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.medium),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (defaultFonts.isNotEmpty()) {
                item {
                    FontGroup(
                        titleRes = R.string.subtitle_font_section_default,
                        fonts = defaultFonts,
                        selectedFamily = selectedFamily,
                        onSelect = onSelect,
                        onDelete = onDelete,
                        modifier = Modifier.widthIn(max = Dimens.contentMaxWidth),
                    )
                }
            }
            if (bundledFonts.isNotEmpty()) {
                item {
                    FontGroup(
                        titleRes = R.string.subtitle_font_section_bundled,
                        fonts = bundledFonts,
                        selectedFamily = selectedFamily,
                        onSelect = onSelect,
                        onDelete = onDelete,
                        modifier = Modifier.widthIn(max = Dimens.contentMaxWidth),
                    )
                }
            }
            if (systemFonts.isNotEmpty()) {
                item {
                    FontGroup(
                        titleRes = R.string.subtitle_font_section_system,
                        fonts = systemFonts,
                        selectedFamily = selectedFamily,
                        onSelect = onSelect,
                        onDelete = onDelete,
                        modifier = Modifier.widthIn(max = Dimens.contentMaxWidth),
                    )
                }
            }
            if (importedFonts.isNotEmpty()) {
                item {
                    FontGroup(
                        titleRes = R.string.subtitle_font_section_imported,
                        fonts = importedFonts,
                        selectedFamily = selectedFamily,
                        onSelect = onSelect,
                        onDelete = onDelete,
                        modifier = Modifier.widthIn(max = Dimens.contentMaxWidth),
                    )
                }
            }
            item {
                Card(
                    onClick = onImport,
                    modifier = Modifier.widthIn(max = Dimens.contentMaxWidth),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(MaterialTheme.spacings.medium),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painter = painterResource(CoreR.drawable.ic_plus),
                            contentDescription = null,
                        )
                        Spacer(modifier = Modifier.width(MaterialTheme.spacings.default))
                        Text(
                            text = stringResource(R.string.subtitle_font_import),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }
            item {
                Text(
                    text = stringResource(R.string.subtitle_font_note),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.widthIn(max = Dimens.contentMaxWidth),
                )
            }
        }
    }
}

/** A titled card of selectable font rows, matching the settings cards' look. */
@Composable
private fun FontGroup(
    @StringRes titleRes: Int,
    fonts: List<SubtitleFonts.Font>,
    selectedFamily: String,
    onSelect: (SubtitleFonts.Font) -> Unit,
    onDelete: (SubtitleFonts.Font) -> Unit,
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
                    onDelete = onDelete,
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
    onDelete: (SubtitleFonts.Font) -> Unit,
) {
    val fontFamily =
        remember(font.file) {
            font.file?.let { file ->
                runCatching { FontFamily(Typeface.createFromFile(file)) }.getOrNull()
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
                contentDescription = stringResource(R.string.subtitle_font_selected),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        if (font.removable) {
            IconButton(onClick = { onDelete(font) }) {
                Icon(
                    painter = painterResource(CoreR.drawable.ic_trash),
                    contentDescription = stringResource(R.string.subtitle_font_delete),
                )
            }
        }
    }
}

@PreviewScreenSizes
@Composable
private fun SubtitleFontScreenLayoutPreview() {
    FindroidTheme {
        SubtitleFontScreenLayout(
            fonts =
                listOf(
                    SubtitleFonts.Font("sans-serif", "Default (Roboto)", file = null),
                    SubtitleFonts.Font("Geist", "Geist", file = null),
                    SubtitleFonts.Font("Geist Mono", "Geist Mono", file = null),
                ),
            selectedFamily = "sans-serif",
            onBack = {},
            onSelect = {},
            onDelete = {},
            onImport = {},
        )
    }
}
