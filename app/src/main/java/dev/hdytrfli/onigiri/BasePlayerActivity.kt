package dev.hdytrfli.onigiri

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding
import androidx.media3.session.MediaSession
import dev.hdytrfli.onigiri.player.local.presentation.PlayerViewModel
import java.util.UUID

abstract class BasePlayerActivity : AppCompatActivity() {

    abstract val viewModel: PlayerViewModel

    private lateinit var mediaSession: MediaSession
    private var wasPip: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }

    override fun onStart() {
        super.onStart()

        // Media3 requires every live session to have a unique ID. The app can now have the music
        // service's session and the video player's session alive at the same time.
        mediaSession =
            MediaSession.Builder(this, viewModel.player).setId("video-${UUID.randomUUID()}").build()
    }

    override fun onResume() {
        super.onResume()

        if (wasPip) {
            wasPip = false
        } else {
            val shouldPlay = viewModel.playWhenReady
            if (viewModel.player.playWhenReady != shouldPlay) {
                viewModel.player.playWhenReady = shouldPlay
            }
        }
        hideSystemUI()
    }

    override fun onPause() {
        super.onPause()

        if (isInPictureInPictureMode) {
            wasPip = true
        } else {
            viewModel.playWhenReady = viewModel.player.playWhenReady
            if (viewModel.player.playWhenReady) viewModel.player.pause()
            viewModel.updatePlaybackProgress()
        }
    }

    override fun onStop() {
        super.onStop()

        mediaSession.release()

        if (wasPip) {
            finish()
        }
    }

    protected fun hideSystemUI() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }

        window.attributes.layoutInDisplayCutoutMode =
            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
    }

    /**
     * Reserves the display cutout vertically for the player's controls. The player hides the system
     * bars and uses the full screen width - the video and controls run under a side camera cutout
     * instead of leaving a dead strip - so only the top/bottom cutout insets are padded.
     */
    protected fun configureInsets(playerControls: View) {
        playerControls.setOnApplyWindowInsetsListener { _, windowInsets ->
            val cutout = windowInsets.displayCutout
            playerControls.updatePadding(
                left = 0,
                top = cutout?.safeInsetTop ?: 0,
                right = 0,
                bottom = cutout?.safeInsetBottom ?: 0,
            )
            return@setOnApplyWindowInsetsListener windowInsets
        }
    }
}
