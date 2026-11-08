package dev.hdytrfli.onigiri.presentation.reader

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SliderState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.hdytrfli.onigiri.core.R as CoreR
import dev.hdytrfli.onigiri.feature.film.presentation.components.OverviewText
import dev.hdytrfli.onigiri.ui.components.AppButton
import dev.hdytrfli.onigiri.ui.components.AppButtonVariant
import dev.hdytrfli.onigiri.ui.components.AppIconButton
import dev.hdytrfli.onigiri.ui.components.AppSeekBar
import dev.hdytrfli.onigiri.ui.components.ReservedWidthText
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
    preciseSeeking: Boolean,
    onBack: () -> Unit,
    onOpenContents: () -> Unit,
    onOpenSettings: () -> Unit,
    onSeek: (Int) -> Unit,
    onInteraction: () -> Unit,
) {
    var scrubValue by remember { mutableStateOf<Float?>(null) }
    var isScrubbing by remember { mutableStateOf(false) }
    val sliderState = remember { SliderState(state.progress) }
    sliderState.value = scrubValue ?: state.progress
    LaunchedEffect(state.page) { if (!isScrubbing) scrubValue = null }
    val displayedPage =
        scrubValue?.let { value ->
            if (state.pageCount > 1) {
                (value * (state.pageCount - 1)).roundToInt() + 1
            } else {
                state.page
            }
        } ?: state.page

    val pageDigits = state.pageCount.toString().length.coerceAtLeast(1)

    Box(modifier = Modifier.fillMaxSize()) {
        when {
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
                    // One gradient for the whole reader chrome: light at the top, darker at the
                    // bottom where the title and scrubber sit.
                    Box(modifier = Modifier.fillMaxSize().background(ReaderScrim))
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
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AppIconButton(onClick = onBack, variant = AppButtonVariant.Ghost) {
                            Icon(
                                painter = painterResource(CoreR.drawable.ic_arrow_left),
                                contentDescription = "Back",
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        AppIconButton(onClick = onOpenContents, variant = AppButtonVariant.Ghost) {
                            Icon(
                                painter = painterResource(CoreR.drawable.ic_list),
                                contentDescription = "Contents",
                            )
                        }
                        AppIconButton(onClick = onOpenSettings, variant = AppButtonVariant.Ghost) {
                            Icon(
                                painter = painterResource(CoreR.drawable.ic_lucide_settings),
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
                                .navigationBarsPadding()
                                .padding(horizontal = 24.dp, vertical = 16.dp)
                    ) {
                        Text(
                            text = state.title,
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (state.overview.isNotBlank()) {
                            OverviewText(
                                text = state.overview,
                                maxCollapsedLines = 2,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            ReservedWidthText(
                                text = displayedPage.toString().padStart(pageDigits, '0'),
                                referenceText = state.pageCount.toString(),
                                style =
                                    MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 14.sp,
                                        color = Color.White,
                                    ),
                                color = Color.White,
                            )
                            AppSeekBar(
                                fraction = scrubValue ?: state.progress,
                                enabled = state.pageCount > 1,
                                preciseSeeking = preciseSeeking,
                                onSeekStart = {
                                    isScrubbing = true
                                    onInteraction()
                                },
                                onSeekFinished = { isScrubbing = false },
                                onSeek = { value ->
                                    scrubValue = value
                                    onSeek((value * (state.pageCount - 1)).roundToInt() + 1)
                                },
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = state.pageCount.toString().padStart(pageDigits, '0'),
                                style =
                                    MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 14.sp,
                                        color = Color.White,
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
private fun ReaderSliderThumb() {
    Box(
        modifier =
            Modifier.width(4.dp)
                .height(16.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White)
    )
}
