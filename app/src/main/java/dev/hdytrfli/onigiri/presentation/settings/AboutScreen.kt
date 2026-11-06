package dev.hdytrfli.onigiri.presentation.settings

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import dev.hdytrfli.onigiri.BuildConfig
import dev.hdytrfli.onigiri.R
import dev.hdytrfli.onigiri.core.R as CoreR
import dev.hdytrfli.onigiri.settings.R as SettingsR
import dev.hdytrfli.onigiri.ui.components.AppOptionRow
import dev.hdytrfli.onigiri.ui.components.LocalRootPageContentInset
import dev.hdytrfli.onigiri.ui.components.PageContent
import dev.hdytrfli.onigiri.ui.components.PageTopBar
import dev.hdytrfli.onigiri.ui.components.TopBarPageShell
import dev.hdytrfli.onigiri.ui.theme.FindroidTheme
import dev.hdytrfli.onigiri.ui.theme.spacings

private const val OUR_GITHUB_URL = "https://github.com/hdytrfli/onigiri"
private const val FINDROID_URL = "https://github.com/jarnedemeulemeester/findroid"
private const val JELLYFIN_URL = "https://jellyfin.org"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(navigateBack: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    val openLink = { url: String -> runCatching { uriHandler.openUri(url) } }

    TopBarPageShell(
        topBar = {
            PageTopBar(title = stringResource(SettingsR.string.about), onBack = navigateBack)
        }
    ) {
        PageContent {
            Column(
                modifier =
                    Modifier.fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(
                            top = LocalRootPageContentInset.current + MaterialTheme.spacings.large,
                            bottom = MaterialTheme.spacings.large,
                        ),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    painter = painterResource(CoreR.drawable.ic_banner),
                    contentDescription = stringResource(CoreR.string.app_name),
                    modifier = Modifier.size(112.dp).clip(MaterialTheme.shapes.extraLarge),
                )
                Spacer(Modifier.height(MaterialTheme.spacings.medium))
                Text(
                    text = stringResource(CoreR.string.app_name),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Spacer(Modifier.height(MaterialTheme.spacings.extraSmall))
                Text(
                    text = stringResource(R.string.about_version, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(MaterialTheme.spacings.medium))
                Text(
                    text = stringResource(R.string.about_description),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(MaterialTheme.spacings.large))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ),
                ) {
                    AboutLinkRow(
                        title = stringResource(R.string.about_our_github),
                        icon = CoreR.drawable.ic_github,
                        onClick = { openLink(OUR_GITHUB_URL) },
                    )
                    HorizontalDivider()
                    AboutLinkRow(
                        title = stringResource(R.string.about_findroid),
                        icon = CoreR.drawable.ic_logo,
                        onClick = { openLink(FINDROID_URL) },
                    )
                    HorizontalDivider()
                    AboutLinkRow(
                        title = stringResource(R.string.about_jellyfin),
                        icon = CoreR.drawable.ic_globe,
                        onClick = { openLink(JELLYFIN_URL) },
                    )
                }
                Spacer(Modifier.height(MaterialTheme.spacings.medium))
                Text(
                    text = stringResource(R.string.about_fork_note),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun AboutLinkRow(
    title: String,
    @DrawableRes icon: Int,
    onClick: () -> Unit,
) {
    AppOptionRow(
        text = title,
        onClick = onClick,
        showCheck = false,
        iconDrawableId = icon,
        trailingContent = {
            Icon(
                painter = painterResource(CoreR.drawable.ic_arrow_up_right),
                contentDescription = stringResource(R.string.about_open_link),
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
    )
}

@Composable
@PreviewScreenSizes
private fun AboutScreenPreview() {
    FindroidTheme { AboutScreen(navigateBack = {}) }
}
