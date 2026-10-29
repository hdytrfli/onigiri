package dev.jdtech.jellyfin.presentation.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.presentation.components.AppButton
import dev.jdtech.jellyfin.presentation.components.AppButtonVariant
import dev.jdtech.jellyfin.presentation.components.AppIconButton
import kotlin.math.roundToInt

/** What the reader activity is showing while the publication opens. */
data class ReaderUiState(
    val title: String = "",
    val overview: String = "",
    val loading: Boolean = true,
    val error: String? = null,
    val page: Int = 0,
    val pageCount: Int = 0,
) {
    val progress: Float
        get() =
            if (pageCount > 1) {
                ((page - 1).toFloat() / (pageCount - 1)).coerceIn(0f, 1f)
            } else {
                0f
            }
}

private val ReaderScrim =
    Brush.verticalGradient(
        0f to Color(0x33000000),
        1f to Color(0xDD000000),
    )

/**
 * The reader's chrome, in the player's language: a top bar with back and the two reader actions,
 * and a bottom bar carrying the book's identity, description and page scrubber. [controlsVisible]
 * is owned by the activity so a tap on the page toggles both bars with one fade.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderOverlay(
    state: ReaderUiState,
    controlsVisible: Boolean,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onSeek: (Int) -> Unit,
    onInteraction: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            state.loading ->
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = state.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 32.dp),
                    )
                    CircularProgressIndicator()
                }

            state.error != null ->
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(text = state.error, style = MaterialTheme.typography.bodyLarge)
                    AppButton(onClick = onBack) { Text("Close") }
                }

            else -> {
                AnimatedVisibility(
                    visible = controlsVisible,
                    enter = fadeIn(tween(300)),
                    exit = fadeOut(tween(300)),
                ) {
                    // The player dims the content behind its controls; the top buttons need that
                    // contrast even though the gradient itself lives at the bottom.
                    Box(
                        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f))
                    )
                }
                AnimatedVisibility(
                    visible = controlsVisible,
                    enter = fadeIn(tween(300)),
                    exit = fadeOut(tween(300)),
                    modifier = Modifier.align(Alignment.TopCenter),
                ) {
                    Row(
                        modifier =
                            Modifier.fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AppIconButton(onClick = onBack, variant = AppButtonVariant.Ghost) {
                            Icon(
                                painter = painterResource(CoreR.drawable.ic_arrow_left),
                                contentDescription = "Back",
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        AppIconButton(onClick = onOpenSettings, variant = AppButtonVariant.Ghost) {
                            Icon(
                                painter = painterResource(CoreR.drawable.ic_bolt),
                                contentDescription = "Reader settings",
                            )
                        }
                    }
                }

                AnimatedVisibility(
                    visible = controlsVisible,
                    enter = fadeIn(tween(300)),
                    exit = fadeOut(tween(300)),
                    modifier = Modifier.align(Alignment.BottomCenter),
                ) {
                    Column(
                        modifier =
                            Modifier.fillMaxWidth()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.Transparent, Color(0xDD000000))
                                    )
                                )
                                .navigationBarsPadding()
                                .padding(horizontal = 24.dp, vertical = 16.dp)
                    ) {
                        Text(
                            text = state.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (state.overview.isNotBlank()) {
                            Text(
                                text = state.overview,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.72f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = state.page.toString(),
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.8f),
                            )
                            Slider(
                                value = state.progress,
                                onValueChange = { value ->
                                    if (state.pageCount > 1) {
                                        onInteraction()
                                        onSeek((value * (state.pageCount - 1)).roundToInt() + 1)
                                    }
                                },
                                colors =
                                    SliderDefaults.colors(
                                        thumbColor = Color.White,
                                        activeTrackColor = Color.White,
                                        inactiveTrackColor = Color(0x33FFFFFF),
                                        activeTickColor = Color.Transparent,
                                        inactiveTickColor = Color.Transparent,
                                    ),
                                thumb = { ReaderSliderThumb() },
                                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                                enabled = state.pageCount > 1,
                            )
                            Text(
                                text = state.pageCount.toString(),
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.8f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReaderSliderThumb() {
    Box(
        modifier =
            Modifier.width(4.dp)
                .height(16.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White)
    )
}
