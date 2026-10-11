package dev.jdtech.jellyfin

import android.app.AppOpsManager
import android.app.PictureInPictureParams
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Rect
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.util.Rational
import android.view.SurfaceView
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ListView
import android.widget.Space
import android.widget.TextView
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.C
import androidx.media3.ui.DefaultTimeBar
import androidx.media3.ui.PlayerControlView
import androidx.media3.ui.PlayerView
import coil3.load
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.databinding.ActivityPlayerBinding
import dev.jdtech.jellyfin.models.FindroidEpisode
import dev.jdtech.jellyfin.player.local.presentation.PlayerEvents
import dev.jdtech.jellyfin.player.local.presentation.PlayerViewModel
import dev.jdtech.jellyfin.presentation.player.PlayerEpisodeAdapter
import dev.jdtech.jellyfin.presentation.player.PlayerMenuAdapter
import dev.jdtech.jellyfin.presentation.player.PlayerMenuItem
import dev.jdtech.jellyfin.settings.domain.AppPreferences
import dev.jdtech.jellyfin.utils.PlayerGestureHelper
import dev.jdtech.jellyfin.utils.PreviewScrubListener
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

var isControlsLocked: Boolean = false
var isVolumeBrightnessLocked: Boolean = false

private val playerSpeeds = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f)
private val playerSpeedTexts = listOf("0.5x", "0.75x", "1x", "1.25x", "1.5x", "1.75x", "2x")

@AndroidEntryPoint
class PlayerActivity : BasePlayerActivity() {

    @Inject lateinit var appPreferences: AppPreferences

    lateinit var binding: ActivityPlayerBinding
    private var playerGestureHelper: PlayerGestureHelper? = null
    override val viewModel: PlayerViewModel by viewModels()
    private var previewScrubListener: PreviewScrubListener? = null
    private var wasZoom: Boolean = false
    private var skipButtonTimeoutExpired: Boolean = true

    private lateinit var skipSegmentButton: Button

    private val isPipSupported by lazy {
        // Check if device has PiP feature
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
            return@lazy false
        }

        // Check if PiP is enabled for the app
        val appOps = getSystemService(APP_OPS_SERVICE) as AppOpsManager?
        appOps?.checkOpNoThrow(
            AppOpsManager.OPSTR_PICTURE_IN_PICTURE,
            Process.myUid(),
            packageName,
        ) == AppOpsManager.MODE_ALLOWED
    }

    private val handler = Handler(Looper.getMainLooper())
    private val skipButtonTimeout = Runnable {
        if (!binding.playerView.isControllerFullyVisible) {
            skipSegmentButton.isVisible = false
            skipButtonTimeoutExpired = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val itemId = UUID.fromString(intent.extras!!.getString("itemId"))
        val itemKind = intent.extras!!.getString("itemKind")
        val startFromBeginning = intent.extras!!.getBoolean("startFromBeginning")

        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        binding.playerView.player = viewModel.player
        binding.playerView.setControllerVisibilityListener(
            PlayerView.ControllerVisibilityListener { visibility ->
                if (visibility == View.GONE) {
                    hideSystemUI()
                }
            }
        )

        val playerControls = binding.playerView.findViewById<View>(R.id.player_controls)
        val lockedControls = binding.playerView.findViewById<View>(R.id.locked_player_view)

        isControlsLocked = false

        configureInsets(playerControls)
        configureInsets(lockedControls)

        if (appPreferences.getValue(appPreferences.playerGestures)) {
            playerGestureHelper =
                PlayerGestureHelper(
                    appPreferences,
                    this,
                    binding.playerView,
                    getSystemService(AUDIO_SERVICE) as AudioManager,
                )
        }

        binding.playerView.findViewById<View>(R.id.back_button).setOnClickListener {
            finishPlayback()
        }

        val videoNameTextView = binding.playerView.findViewById<TextView>(R.id.video_name)
        val videoLogoImageView = binding.playerView.findViewById<ImageView>(R.id.video_logo)
        // Title logo sizing from settings (player width fraction and max width:height ratio).
        val playerWidthFraction =
            appPreferences.getValue(appPreferences.titlePlayerWidth).toFloatOrNull() ?: 0.2f
        val logoMaxWidth = (resources.displayMetrics.widthPixels * playerWidthFraction).toInt()
        val logoRatio =
            appPreferences.getValue(appPreferences.titleImageRatio).toFloatOrNull() ?: 2f
        videoLogoImageView.maxWidth = logoMaxWidth
        videoLogoImageView.maxHeight = (logoMaxWidth / logoRatio).toInt()
        // Apply the naive white overlay when enabled.
        videoLogoImageView.colorFilter =
            if (appPreferences.getValue(appPreferences.naiveTitleOverlay)) {
                PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
            } else {
                null
            }
        val videoDescriptionTextView =
            binding.playerView.findViewById<TextView>(R.id.video_description)

        // Tap the description to expand/collapse it.
        var descriptionExpanded = false
        videoDescriptionTextView.setOnClickListener {
            descriptionExpanded = !descriptionExpanded
            videoDescriptionTextView.maxLines = if (descriptionExpanded) Int.MAX_VALUE else 2
        }

        val playerMenuButton = binding.playerView.findViewById<ImageButton>(R.id.btn_player_menu)
        val btnEpisodes = binding.playerView.findViewById<ImageButton>(R.id.btn_episodes)
        skipSegmentButton = binding.playerView.findViewById(R.id.btn_skip_segment)
        val pipButton = binding.playerView.findViewById<ImageButton>(R.id.btn_pip)
        val lockButton = binding.playerView.findViewById<ImageButton>(R.id.btn_lockview)
        val gestureLockButton = binding.playerView.findViewById<ImageButton>(R.id.btn_gesture_lock)
        val unlockButton = binding.playerView.findViewById<ImageButton>(R.id.btn_unlock)

        // Episodes sheet
        val episodesOverlay = findViewById<FrameLayout>(R.id.player_episodes_overlay)
        val episodesScrim = findViewById<View>(R.id.player_episodes_scrim)
        val episodesPanel = findViewById<View>(R.id.player_episodes_panel)
        val episodesList = findViewById<ListView>(R.id.player_episodes_list)

        val episodeAdapter =
            PlayerEpisodeAdapter(this) { episode ->
                viewModel.selectEpisode(episode.id)
                hideOptionsSheet(episodesOverlay, episodesPanel, episodesScrim)
            }
        episodesList.adapter = episodeAdapter
        episodesScrim.setOnClickListener {
            hideOptionsSheet(episodesOverlay, episodesPanel, episodesScrim)
        }
        btnEpisodes.setOnClickListener {
            showOptionsSheet(episodesOverlay, episodesPanel, episodesScrim)
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    var lastLogo: String? = null
                    var lastOverview: String? = null
                    var lastEpisodes: List<FindroidEpisode> = emptyList()
                    viewModel.uiState.collect { uiState ->
                        Timber.d("$uiState")
                        uiState.apply {
                            // Title (logo image when available, else text)
                            if (currentItemLogo != lastLogo) {
                                lastLogo = currentItemLogo
                                if (currentItemLogo != null) {
                                    videoNameTextView.isVisible = false
                                    videoLogoImageView.isVisible = true
                                    videoLogoImageView.load(currentItemLogo)
                                } else {
                                    videoNameTextView.isVisible = true
                                    videoLogoImageView.isVisible = false
                                }
                            }
                            videoNameTextView.text = currentItemTitle

                            // Description
                            if (currentItemOverview != lastOverview) {
                                lastOverview = currentItemOverview
                                descriptionExpanded = false
                                videoDescriptionTextView.maxLines = 2
                            }
                            videoDescriptionTextView.text = currentItemOverview
                            videoDescriptionTextView.isVisible =
                                !currentItemOverview.isNullOrBlank()

                            // Media segment
                            currentSegment?.let { segment ->
                                // Skip Button - text
                                skipSegmentButton.text = getString(currentSkipButtonStringRes)
                                // Skip Button - visibility
                                skipSegmentButton.isVisible = !isInPictureInPictureMode
                                if (skipSegmentButton.isVisible) {
                                    skipButtonTimeoutExpired = false
                                    handler.removeCallbacks(skipButtonTimeout)
                                    handler.postDelayed(
                                        skipButtonTimeout,
                                        viewModel.segmentsSkipButtonDuration * 1000,
                                    )
                                }
                                // Skip Button - onClick
                                skipSegmentButton.setOnClickListener {
                                    viewModel.skipSegment(segment)
                                    skipSegmentButton.isVisible = false
                                }
                            } ?: run { skipSegmentButton.isVisible = false }

                            binding.playerView.setControllerVisibilityListener(
                                PlayerView.ControllerVisibilityListener { visibility ->
                                    if (skipButtonTimeoutExpired && currentSegment != null) {
                                        skipSegmentButton.visibility = visibility
                                    }
                                }
                            )

                            // Trickplay
                            previewScrubListener?.let { it.currentTrickplay = currentTrickplay }

                            playerGestureHelper?.let { it.currentTrickplay = currentTrickplay }

                            // Chapters
                            val playerControlView =
                                findViewById<PlayerControlView>(R.id.exo_controller)
                            if (currentChapters.isNotEmpty()) {
                                val numOfChapters = currentChapters.size
                                playerControlView.setExtraAdGroupMarkers(
                                    LongArray(numOfChapters) { index ->
                                        currentChapters[index].startPosition
                                    },
                                    BooleanArray(numOfChapters) { false },
                                )
                            } else {
                                playerControlView.setExtraAdGroupMarkers(null, null)
                            }

                            // File Loaded
                            if (fileLoaded) {
                                playerMenuButton.isEnabled = true
                                playerMenuButton.imageAlpha = 255
                                lockButton.isEnabled = true
                                lockButton.imageAlpha = 255
                                gestureLockButton.isEnabled = true
                                gestureLockButton.imageAlpha = 255
                                btnEpisodes.isEnabled = true
                                btnEpisodes.imageAlpha = 255
                                pipButton.isEnabled = true
                                pipButton.imageAlpha = 255
                            }

                            // Episodes
                            btnEpisodes.isVisible = currentSeasonEpisodes.isNotEmpty()
                            if (currentSeasonEpisodes != lastEpisodes) {
                                lastEpisodes = currentSeasonEpisodes
                                episodeAdapter.submit(currentSeasonEpisodes)
                            }
                        }
                    }
                }

                launch {
                    viewModel.eventsChannelFlow.collect { event ->
                        when (event) {
                            is PlayerEvents.NavigateBack -> finishPlayback()
                            is PlayerEvents.IsPlayingChanged -> {
                                if (event.isPlaying) {
                                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                                } else {
                                    window.clearFlags(
                                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                                    )
                                }

                                if (appPreferences.getValue(appPreferences.playerPipGesture)) {
                                    try {
                                        setPictureInPictureParams(pipParams(event.isPlaying))
                                    } catch (_: IllegalArgumentException) {}
                                }
                            }
                        }
                    }
                }

                launch {
                    while (true) {
                        viewModel.updatePlaybackProgress()
                        delay(5000L)
                    }
                }

                if (
                    appPreferences.getValue(appPreferences.playerMediaSegmentsSkipButton) ||
                        appPreferences.getValue(appPreferences.playerMediaSegmentsAutoSkip)
                ) {
                    launch {
                        while (true) {
                            viewModel.updateCurrentSegment()
                            delay(1000L)
                        }
                    }
                }
            }
        }

        playerMenuButton.isEnabled = false
        playerMenuButton.imageAlpha = 75

        lockButton.isEnabled = false
        lockButton.imageAlpha = 75

        gestureLockButton.isEnabled = false
        gestureLockButton.imageAlpha = 75

        btnEpisodes.isEnabled = false
        btnEpisodes.imageAlpha = 75

        if (isPipSupported) {
            pipButton.isEnabled = false
            pipButton.imageAlpha = 75
        } else {
            val pipSpace = binding.playerView.findViewById<Space>(R.id.space_pip)
            pipButton.isVisible = false
            pipSpace.isVisible = false
        }

        val optionsOverlay = findViewById<FrameLayout>(R.id.player_options_overlay)
        val optionsScrim = findViewById<View>(R.id.player_options_scrim)
        val optionsPanel = findViewById<View>(R.id.player_options_panel)
        val optionsTabs = findViewById<TabLayout>(R.id.player_options_tabs)
        val optionsList = findViewById<ListView>(R.id.player_options_list)

        val menuAdapter =
            PlayerMenuAdapter(this) { position ->
                when (optionsTabs.selectedTabPosition) {
                    0 -> viewModel.switchToTrack(C.TRACK_TYPE_AUDIO, position - 1)
                    1 -> viewModel.switchToTrack(C.TRACK_TYPE_TEXT, position - 1)
                    else -> viewModel.selectSpeed(playerSpeeds[position])
                }
                hideOptionsSheet(optionsOverlay, optionsPanel, optionsScrim)
            }
        optionsList.adapter = menuAdapter
        optionsTabs.addTab(optionsTabs.newTab().setText(R.string.player_menu_tab_audio))
        optionsTabs.addTab(optionsTabs.newTab().setText(R.string.player_menu_tab_subtitles))
        optionsTabs.addTab(optionsTabs.newTab().setText(R.string.player_menu_tab_speed))
        optionsTabs.addOnTabSelectedListener(
            object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab) {
                    menuAdapter.submit(optionsItems(tab.position))
                }

                override fun onTabUnselected(tab: TabLayout.Tab) = Unit

                override fun onTabReselected(tab: TabLayout.Tab) = Unit
            }
        )
        optionsScrim.setOnClickListener {
            hideOptionsSheet(optionsOverlay, optionsPanel, optionsScrim)
        }

        playerMenuButton.setOnClickListener {
            menuAdapter.submit(optionsItems(optionsTabs.selectedTabPosition))
            showOptionsSheet(optionsOverlay, optionsPanel, optionsScrim)
        }

        val exoPlayerControlView = findViewById<FrameLayout>(R.id.player_controls)
        val lockedLayout = findViewById<FrameLayout>(R.id.locked_player_view)

        gestureLockButton.isVisible = appPreferences.getValue(appPreferences.playerGesturesVB)
        gestureLockButton.setOnClickListener {
            isVolumeBrightnessLocked = !isVolumeBrightnessLocked
            gestureLockButton.setImageResource(
                if (isVolumeBrightnessLocked) CoreR.drawable.ic_circle_off
                else CoreR.drawable.ic_circle
            )
        }

        lockButton.setOnClickListener {
            exoPlayerControlView.visibility = View.GONE
            lockedLayout.visibility = View.VISIBLE
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
            isControlsLocked = true
        }

        unlockButton.setOnClickListener {
            exoPlayerControlView.visibility = View.VISIBLE
            lockedLayout.visibility = View.GONE
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            isControlsLocked = false
            isVolumeBrightnessLocked = false
            gestureLockButton.setImageResource(CoreR.drawable.ic_circle)
        }

        pipButton.setOnClickListener { pictureInPicture() }

        // Set marker color
        val timeBar = binding.playerView.findViewById<DefaultTimeBar>(R.id.exo_progress)
        timeBar.setAdMarkerColor(Color.WHITE)

        if (appPreferences.getValue(appPreferences.playerTrickplay)) {
            val imagePreview = binding.playerView.findViewById<ImageView>(R.id.image_preview)
            previewScrubListener = PreviewScrubListener(imagePreview, timeBar, viewModel.player)

            timeBar.addListener(previewScrubListener!!)
        }

        viewModel.initializePlayer(
            itemId = itemId,
            itemKind = itemKind ?: "",
            startFromBeginning = startFromBeginning,
        )
        hideSystemUI()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        val itemId = UUID.fromString(intent.extras!!.getString("itemId"))
        val itemKind = intent.extras!!.getString("itemKind")
        val startFromBeginning = intent.extras!!.getBoolean("startFromBeginning")

        viewModel.initializePlayer(
            itemId = itemId,
            itemKind = itemKind ?: "",
            startFromBeginning = startFromBeginning,
        )
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S &&
                appPreferences.getValue(appPreferences.playerPipGesture) &&
                viewModel.player.isPlaying &&
                !isControlsLocked
        ) {
            pictureInPicture()
        }
    }

    private fun finishPlayback() {
        try {
            viewModel.player.clearVideoSurfaceView(
                binding.playerView.videoSurfaceView as SurfaceView
            )
        } catch (e: Exception) {
            Timber.e(e)
        }
        handler.removeCallbacks(skipButtonTimeout)
        finish()
    }

    private fun pipParams(
        enableAutoEnter: Boolean = viewModel.player.isPlaying
    ): PictureInPictureParams {
        val displayAspectRatio = Rational(binding.playerView.width, binding.playerView.height)

        val aspectRatio =
            binding.playerView.player?.videoSize?.let {
                Rational(
                    it.width.coerceAtMost((it.height * 2.39f).toInt()),
                    it.height.coerceAtMost((it.width * 2.39f).toInt()),
                )
            }

        val sourceRectHint =
            if (displayAspectRatio < aspectRatio!!) {
                val space =
                    ((binding.playerView.height -
                            (binding.playerView.width.toFloat() / aspectRatio.toFloat())) / 2)
                        .toInt()
                Rect(
                    0,
                    space,
                    binding.playerView.width,
                    (binding.playerView.width.toFloat() / aspectRatio.toFloat()).toInt() + space,
                )
            } else {
                val space =
                    ((binding.playerView.width -
                            (binding.playerView.height.toFloat() * aspectRatio.toFloat())) / 2)
                        .toInt()
                Rect(
                    space,
                    0,
                    (binding.playerView.height.toFloat() * aspectRatio.toFloat()).toInt() + space,
                    binding.playerView.height,
                )
            }

        val builder =
            PictureInPictureParams.Builder()
                .setAspectRatio(aspectRatio)
                .setSourceRectHint(sourceRectHint)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setAutoEnterEnabled(enableAutoEnter)
        }

        return builder.build()
    }

    private fun pictureInPicture() {
        if (!isPipSupported) {
            return
        }

        try {
            enterPictureInPictureMode(pipParams())
        } catch (_: IllegalArgumentException) {}
    }

    private fun optionsItems(tab: Int): List<PlayerMenuItem> =
        when (tab) {
            0 -> trackItems(C.TRACK_TYPE_AUDIO)
            1 -> trackItems(C.TRACK_TYPE_TEXT)
            else ->
                playerSpeeds.mapIndexed { index, speed ->
                    PlayerMenuItem(
                        title = playerSpeedTexts[index],
                        subtitle = null,
                        selected = speed == viewModel.playbackSpeed,
                    )
                }
        }

    private fun trackItems(type: @C.TrackType Int): List<PlayerMenuItem> {
        val groups =
            viewModel.player.currentTracks.groups.filter { it.type == type && it.isSupported }

        val items = mutableListOf<PlayerMenuItem>()
        items.add(
            PlayerMenuItem(
                title = getString(CoreR.string.none),
                subtitle = null,
                selected = groups.none { it.isSelected },
            )
        )
        groups.forEachIndexed { index, group ->
            val format = group.mediaTrackGroup.getFormat(0)
            items.add(
                PlayerMenuItem(
                    title = format.label ?: format.language ?: "Track ${index + 1}",
                    subtitle = format.codecs,
                    selected = group.isSelected,
                )
            )
        }
        return items
    }

    private fun showOptionsSheet(overlay: View, panel: View, scrim: View) {
        overlay.visibility = View.VISIBLE
        panel.post {
            panel.translationX = panel.width.toFloat()
            panel.animate().translationX(0f).setDuration(200L).start()
        }
        scrim.alpha = 0f
        scrim.animate().alpha(1f).setDuration(200L).start()
    }

    private fun hideOptionsSheet(overlay: View, panel: View, scrim: View) {
        panel
            .animate()
            .translationX(panel.width.toFloat())
            .setDuration(200L)
            .withEndAction { overlay.visibility = View.GONE }
            .start()
        scrim.animate().alpha(0f).setDuration(200L).start()
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration,
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        viewModel.isInPictureInPictureMode = isInPictureInPictureMode
        when (isInPictureInPictureMode) {
            true -> {
                binding.playerView.useController = false
                skipSegmentButton.isVisible = false

                wasZoom = playerGestureHelper?.isZoomEnabled == true
                playerGestureHelper?.updateZoomMode(false)

                // Brightness mode Auto
                window.attributes =
                    window.attributes.apply {
                        screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                    }
            }

            false -> {
                binding.playerView.useController = true
                playerGestureHelper?.updateZoomMode(wasZoom)

                // Override auto brightness
                if (
                    appPreferences.getValue(appPreferences.playerGesturesVB) &&
                        appPreferences.getValue(appPreferences.playerGesturesBrightnessRemember)
                ) {
                    window.attributes =
                        window.attributes.apply {
                            screenBrightness =
                                appPreferences.getValue(appPreferences.playerBrightness)
                        }
                }
            }
        }
    }
}
