package dev.hdytrfli.onigiri.presentation.player

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.hdytrfli.onigiri.MusicPlayerActivity

/**
 * Collects the continuously ticking playback state inside the mini bar, keeping the rest of the
 * navigation shell still while progress advances.
 */
@Composable
fun MiniMusicPlayerHost(viewModel: MusicPlayerViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    MiniMusicPlayer(
        state = state,
        onOpen = { context.startActivity(Intent(context, MusicPlayerActivity::class.java)) },
        onPlayPause = viewModel::togglePlayPause,
        onNext = viewModel::next,
        modifier = modifier,
    )
}
