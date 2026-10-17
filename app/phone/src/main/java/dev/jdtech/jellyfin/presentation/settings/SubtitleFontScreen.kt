package dev.jdtech.jellyfin.presentation.settings

import android.graphics.Typeface
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
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

/** mpv's `sub-border-style` value that draws an opaque box behind the text. */
private const val BORDER_STYLE_OPAQUE_BOX = "opaque-box"

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
        style = state.style,
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
    style: SubtitleFontStyle,
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
            item {
                SubtitlePreviewCard(
                    font = fonts.firstOrNull { it.family == selectedFamily },
                    style = style,
                    modifier = Modifier.widthIn(max = Dimens.contentMaxWidth),
                )
            }
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

/**
 * Preview of the subtitle style over a dark 16:9 box. mpv sizes its subtitles against a 720px-tall
 * frame, so font/border sizes are scaled by the box height; colours are applied as-is (the border
 * colour fills an opaque box, or casts an outline-like shadow otherwise).
 */
@Composable
private fun SubtitlePreviewCard(
    font: SubtitleFonts.Font?,
    style: SubtitleFontStyle,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val sampleText = stringResource(R.string.subtitle_font_preview_sample)

    val fontFamily =
        remember(font?.file) {
            font?.file?.let { file ->
                runCatching { FontFamily(Typeface.createFromFile(file)) }.getOrNull()
            } ?: FontFamily.SansSerif
        }

    val subtitleColor =
        remember(style.color) { parseSubtitleColor(style.color) }
            ?: MaterialTheme.colorScheme.onSurface
    val outlineColor = remember(style.borderColor) { parseSubtitleColor(style.borderColor) }

    BoxWithConstraints(
        modifier =
            modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceContainerLowest),
        contentAlignment = Alignment.Center,
    ) {
        val frameHeight = maxWidth.value * 9f / 16f

        val textSize =
            with(density) {
                (style.fontSize.toFloatOrNull() ?: 28f)
                    .div(720f)
                    .times(frameHeight)
                    .coerceIn(6f, frameHeight * 0.5f)
                    .dp
                    .toSp()
            }
        val outlineSize =
            (style.borderSize.toFloatOrNull() ?: 0f)
                .div(720f)
                .times(frameHeight)
                .coerceIn(0f, frameHeight * 0.15f)

        val opaqueBoxColor =
            if (style.borderEnabled && style.borderStyle == BORDER_STYLE_OPAQUE_BOX) {
                outlineColor
            } else {
                null
            }

        Text(
            text = sampleText,
            style =
                TextStyle(
                    fontFamily = fontFamily,
                    fontWeight = if (style.bold) FontWeight.Bold else FontWeight.Normal,
                    fontStyle = if (style.italic) FontStyle.Italic else FontStyle.Normal,
                    fontSize = textSize,
                    color = subtitleColor,
                    shadow =
                        if (style.borderEnabled && outlineColor != null && opaqueBoxColor == null) {
                            Shadow(
                                color = outlineColor,
                                offset = Offset.Zero,
                                blurRadius = with(density) { outlineSize.dp.toPx() },
                            )
                        } else {
                            null
                        },
                ),
            textAlign = TextAlign.Center,
            modifier =
                Modifier.padding(MaterialTheme.spacings.medium)
                    .then(
                        if (opaqueBoxColor != null) {
                            Modifier.background(opaqueBoxColor, MaterialTheme.shapes.extraSmall)
                                .padding(
                                    horizontal = outlineSize.dp,
                                    vertical = (outlineSize / 2f).dp,
                                )
                        } else {
                            Modifier
                        }
                    ),
        )
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

/** Parses an mpv `#RRGGBB`/`#AARRGGBB` colour into a Compose [Color], or null when malformed. */
private fun parseSubtitleColor(value: String): Color? {
    val hex = value.removePrefix("#")
    val argb =
        when (hex.length) {
            6 -> "FF$hex"
            8 -> hex
            else -> return null
        }
    return runCatching { Color(argb.toLong(16)) }.getOrNull()
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
            style =
                SubtitleFontStyle(
                    bold = false,
                    italic = false,
                    color = "#FFFFFFFF",
                    fontSize = "28",
                    borderEnabled = true,
                    borderStyle = BORDER_STYLE_OPAQUE_BOX,
                    borderColor = "#FF000000",
                    borderSize = "3",
                ),
            onBack = {},
            onSelect = {},
            onDelete = {},
            onImport = {},
        )
    }
}
