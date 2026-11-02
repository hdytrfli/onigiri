package dev.hdytrfli.onigiri.feature.film.presentation.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.hdytrfli.onigiri.core.R as CoreR
import dev.hdytrfli.onigiri.ui.theme.FindroidTheme

@Composable
fun DownloadedBadge(modifier: Modifier = Modifier) {
    BaseBadge(modifier = modifier) {
        Icon(
            painter = painterResource(CoreR.drawable.ic_download),
            contentDescription = "",
            modifier = Modifier.size(16.dp).align(Alignment.Center),
        )
    }
}

@Composable
@Preview
private fun DownloadedBadgePreview() {
    FindroidTheme { DownloadedBadge() }
}
