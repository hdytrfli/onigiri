package dev.hdytrfli.onigiri

import android.app.AppOpsManager
import android.app.PictureInPictureParams
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
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
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.PathInterpolator
import android.widget.AbsListView
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ListView
import android.widget.Space
import android.widget.TextView
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.ui.DefaultTimeBar
import androidx.media3.ui.PlayerControlView
import androidx.media3.ui.PlayerView
import coil3.load
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint
import dev.hdytrfli.onigiri.core.R as CoreR
import dev.hdytrfli.onigiri.databinding.ActivityPlayerBinding
import dev.hdytrfli.onigiri.feature.film.presentation.mediadetail.MediaDetailState
import dev.hdytrfli.onigiri.feature.film.presentation.mediadetail.MediaDetailViewModel
import dev.hdytrfli.onigiri.models.FindroidEpisode
import dev.hdytrfli.onigiri.player.local.presentation.PlayerEvents
import dev.hdytrfli.onigiri.player.local.presentation.PlayerViewModel
import dev.hdytrfli.onigiri.presentation.player.MediaDetailContent
import dev.hdytrfli.onigiri.presentation.player.MediaDetailHeader
import dev.hdytrfli.onigiri.presentation.player.PlayerEpisodeAdapter
import dev.hdytrfli.onigiri.presentation.player.PlayerMenuAdapter
import dev.hdytrfli.onigiri.presentation.player.PlayerMenuItem
import dev.hdytrfli.onigiri.presentation.player.PlayerStatsPanel
import dev.hdytrfli.onigiri.presentation.player.SubtitleOffsetControl
import dev.hdytrfli.onigiri.presentation.player.formatSubtitleOffset
import dev.hdytrfli.onigiri.settings.domain.AppPreferences
import dev.hdytrfli.onigiri.ui.theme.FindroidTheme
import dev.hdytrfli.onigiri.ui.utils.BackdropRatios
import dev.hdytrfli.onigiri.ui.utils.LocalBackdropRatios
import dev.hdytrfli.onigiri.utils.PlayerGestureHelper
import dev.hdytrfli.onigiri.utils.PreviewScrubListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import kotlin.math.max
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

var isControlsLocked: Boolean = false
var isVolumeBrightnessLocked: Boolean = false

private val playerSpeeds = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f)
private val playerSpeedTexts = listOf("0.5x", "0.75x", "1x", "1.25x", "1.5x", "1.75x", "2x")

/** How long the player overlay takes to fade in or out. */
private const val OVERLAY_FADE_DURATION_MS = 300L

/** How far the stats panel slides down when the controls row is showing: 48dp row + 12dp gap. */
private const val STATS_SHOWN_OFFSET_DP = 60f

/** Material's standard easing: the overlay settles into place rather than stopping dead. */
private val OverlayFadeInInterpolator = PathInterpolator(0.4f, 0f, 0.2f, 1f)

/** Material's accelerate easing: the overlay gets out of the way instead of lingering. */
private val OverlayFadeOutInterpolator = PathInterpolator(0.4f, 0f, 1f, 1f)

@AndroidEntryPoint
class PlayerActivity : BasePlayerActivity() {

    @Inject lateinit var appPreferences: AppPreferences

    lateinit var binding: ActivityPlayerBinding
    private var playerGestureHelper: PlayerGestureHelper? = null
    override val viewModel: PlayerViewModel by viewModels()
    private val detailViewModel: MediaDetailViewModel by viewModels()
    private var previewScrubListener: PreviewScrubListener? = null
    private var wasZoom: Boolean = false
    private var skipButtonTimeoutExpired: Boolean = true

    /** Whether the stats overlay is switched on, and whether PiP is temporarily hiding it. */
    private val showStats = mutableStateOf(false)
    private val statsSuppressed = mutableStateOf(false)

    /** Whether the controls overlay is showing; drives the stats panel's slide. */
    private val playerControlsVisible = mutableStateOf(true)

    private lateinit var skipSegmentButton: Button
    private lateinit var playerTimeRight: TextView

    private val detailItemId = mutableStateOf<UUID?>(null)
    private val detailItemIsEpisode = mutableStateOf(false)

    private val hidePlayerOverlayMessageAction = Runnable {
        binding.progressScrubberLayout.visibility = View.GONE
    }

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

    /** How long the overlay stays up, from the player settings; 0 means it never hides itself. */
    private var overlayTimeoutMs = 5000L

    private val hidePlayerControlsAction = Runnable { hidePlayerControls() }
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
        // How long the overlay stays up before hiding itself ("Never" is stored as -1). The fade
        // does the hiding, so media3's own timeout is off and this drives it instead.
        overlayTimeoutMs =
            (appPreferences.getValue(appPreferences.playerControlsTimeout).toIntOrNull() ?: 5000)
                .coerceAtLeast(0)
                .toLong()
        binding.playerView.controllerShowTimeoutMs = 0
        binding.playerView.setControllerVisibilityListener(
            PlayerView.ControllerVisibilityListener { visibility ->
                playerControlsVisible.value = visibility != View.GONE
                positionStatsPanel(controlsVisible = visibility != View.GONE)
                if (visibility == View.GONE) {
                    hideSystemUI()
                } else {
                    // Every show fades, whether it came from a tap or from media3 itself.
                    fadePlayerControlsIn()
                    schedulePlayerControlsHide()
                }
            }
        )
        // The controller starts out visible in the layout, which is not a visibility change, so the
        // clock for its first hide is started here.
        schedulePlayerControlsHide()
        viewModel.player.addListener(controlsClockListener)

        val playerControls = binding.playerView.findViewById<View>(R.id.player_controls)
        val lockedControls = binding.playerView.findViewById<View>(R.id.locked_player_view)

        isControlsLocked = false
        isVolumeBrightnessLocked = false

        // The player always hides the system bars, so it only ever reserves the display cutout,
        // never the status-bar height.
        configureInsets(playerControls)
        configureInsets(lockedControls)

        playerGestureHelper =
            PlayerGestureHelper(
                appPreferences,
                this,
                binding.playerView,
                getSystemService(AUDIO_SERVICE) as AudioManager,
            )

        binding.playerView.findViewById<View>(R.id.back_button).setOnClickListener {
            finishPlayback()
        }

        val videoNameTextView = binding.playerView.findViewById<TextView>(R.id.video_name)
        val videoLogoImageView = binding.playerView.findViewById<ImageView>(R.id.video_logo)
        // Title logo sizing from settings (player width fraction), capped at a fixed 16:9 box.
        val playerWidthFraction =
            appPreferences.getValue(appPreferences.titlePlayerWidth).toFloatOrNull() ?: 0.2f
        val logoMaxWidth = (resources.displayMetrics.widthPixels * playerWidthFraction).toInt()
        videoLogoImageView.maxWidth = logoMaxWidth
        videoLogoImageView.maxHeight = (logoMaxWidth * 9f / 16f).toInt()
        // Items without a logo fall back to the plain name; it follows the same width setting so
        // the
        // preference always has an effect.
        videoNameTextView.maxWidth = logoMaxWidth
        val videoDescriptionTextView =
            binding.playerView.findViewById<TextView>(R.id.video_description)
        val videoEpisodeNameTextView =
            binding.playerView.findViewById<TextView>(R.id.video_episode_name)

        // Tap the description to expand/collapse it.
        var descriptionExpanded = false
        videoDescriptionTextView.setOnClickListener {
            descriptionExpanded = !descriptionExpanded
            videoDescriptionTextView.maxLines = if (descriptionExpanded) Int.MAX_VALUE else 2
        }

        val playerMenuButton = binding.playerView.findViewById<ImageButton>(R.id.btn_player_menu)
        val btnEpisodes = binding.playerView.findViewById<ImageButton>(R.id.btn_episodes)
        val btnInfo = binding.playerView.findViewById<ImageButton>(R.id.btn_info)
        playerTimeRight = binding.playerView.findViewById(R.id.player_time_right)
        skipSegmentButton = binding.playerView.findViewById(R.id.btn_skip_segment)
        val pipButton = binding.playerView.findViewById<ImageButton>(R.id.btn_pip)
        val statsButton = binding.playerView.findViewById<ImageButton>(R.id.btn_stats)
        val lockButton = binding.playerView.findViewById<ImageButton>(R.id.btn_lockview)
        val gestureLockButton = binding.playerView.findViewById<ImageButton>(R.id.btn_gesture_lock)
        val unlockButton = binding.playerView.findViewById<ImageButton>(R.id.btn_unlock)

        // Sheets are 30% of the screen width, but never narrower than 400dp.
        val sheetWidth =
            max(
                (resources.displayMetrics.widthPixels * 0.30f).toInt(),
                (400 * resources.displayMetrics.density).toInt(),
            )

        // Episodes sheet
        val episodesOverlay = findViewById<FrameLayout>(R.id.player_episodes_overlay)
        val episodesScrim = findViewById<View>(R.id.player_episodes_scrim)
        val episodesPanel = findViewById<View>(R.id.player_episodes_panel)
        val episodesList = findViewById<ListView>(R.id.player_episodes_list)
        episodesPanel.layoutParams = episodesPanel.layoutParams.apply { width = sheetWidth }

        // Header (backdrop + title + metadata + description) and the "Other episodes" label scroll
        // with the list, so they don't eat the space left for the episodes.
        val episodesHeaderComposeView = findViewById<ComposeView>(R.id.player_episodes_header)
        val episodesTitle = findViewById<TextView>(R.id.player_episodes_title)
        episodesList.addScrollableHeader(episodesHeaderComposeView)
        episodesList.addScrollableHeader(episodesTitle)
        setMediaHeaderContent(episodesHeaderComposeView)

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

        // Media detail sheet
        val detailOverlay = findViewById<FrameLayout>(R.id.player_detail_overlay)
        val detailScrim = findViewById<View>(R.id.player_detail_scrim)
        val detailPanel = findViewById<View>(R.id.player_detail_panel)
        detailPanel.layoutParams = detailPanel.layoutParams.apply { width = sheetWidth }

        val detailComposeView = findViewById<ComposeView>(R.id.player_detail_compose)
        setDetailComposeContent(detailComposeView) { MediaDetailContent(it) }

        detailScrim.setOnClickListener { hideOptionsSheet(detailOverlay, detailPanel, detailScrim) }
        btnInfo.setOnClickListener { showOptionsSheet(detailOverlay, detailPanel, detailScrim) }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    var lastLogo: String? = null
                    var lastOverview: String? = null
                    var lastEpisodes: List<FindroidEpisode> = emptyList()
                    var lastEpisodeId: UUID? = null
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

                            // Episode name (episodes only), shown above the overview
                            videoEpisodeNameTextView.isVisible = !currentEpisodeName.isNullOrBlank()
                            videoEpisodeNameTextView.text = currentEpisodeName

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
                                btnInfo.isEnabled = true
                                btnInfo.imageAlpha = 255
                                pipButton.isEnabled = true
                                pipButton.imageAlpha = 255
                                statsButton.isEnabled = true
                                statsButton.imageAlpha = 255
                            }

                            // Episodes
                            btnEpisodes.isVisible = currentSeasonEpisodes.isNotEmpty()
                            if (
                                currentSeasonEpisodes != lastEpisodes ||
                                    currentEpisodeId != lastEpisodeId
                            ) {
                                lastEpisodes = currentSeasonEpisodes
                                lastEpisodeId = currentEpisodeId
                                episodeAdapter.submit(currentSeasonEpisodes, currentEpisodeId)
                            }

                            // Media detail sheet follows the current item.
                            detailItemId.value = currentItemId
                            detailItemIsEpisode.value = currentItemIsEpisode
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

                launch {
                    while (true) {
                        updatePlayerTimeRight()
                        delay(1000L)
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

        btnInfo.isEnabled = false
        btnInfo.imageAlpha = 75

        statsButton.isEnabled = false
        statsButton.imageAlpha = 75

        if (isPipSupported) {
            pipButton.isEnabled = false
            pipButton.imageAlpha = 75
        } else {
            val pipSpace = binding.playerView.findViewById<Space>(R.id.space_pip)
            pipButton.isVisible = false
            pipSpace.isVisible = false
        }

        if (!appPreferences.getValue(appPreferences.playerStats)) {
            val statsSpace = binding.playerView.findViewById<Space>(R.id.space_stats)
            statsButton.isVisible = false
            statsSpace.isVisible = false
        }

        val optionsOverlay = findViewById<FrameLayout>(R.id.player_options_overlay)
        val optionsScrim = findViewById<View>(R.id.player_options_scrim)
        val optionsPanel = findViewById<View>(R.id.player_options_panel)
        optionsPanel.layoutParams = optionsPanel.layoutParams.apply { width = sheetWidth }
        val optionsTabs = findViewById<TabLayout>(R.id.player_options_tabs)
        val optionsList = findViewById<ListView>(R.id.player_options_list)
        val optionsOffset = findViewById<ComposeView>(R.id.player_options_offset)

        // Header (backdrop + title + metadata + description) and the tab row scroll with the list,
        // so they don't eat the space left for the audio/subtitle/speed items.
        val optionsHeaderComposeView = findViewById<ComposeView>(R.id.player_options_header)
        optionsList.addScrollableHeader(optionsHeaderComposeView)
        optionsList.addScrollableHeader(optionsTabs)
        optionsList.addScrollableHeader(optionsOffset)
        setMediaHeaderContent(optionsHeaderComposeView)

        lateinit var menuAdapter: PlayerMenuAdapter
        menuAdapter =
            PlayerMenuAdapter(this) { position ->
                when (optionsTabs.selectedTabPosition) {
                    0 -> viewModel.switchToTrack(C.TRACK_TYPE_AUDIO, position - 1)
                    1 -> viewModel.switchToTrack(C.TRACK_TYPE_TEXT, position - 1)
                    2 -> viewModel.selectSpeed(playerSpeeds[position])
                }
                // Keep the sheet open so several changes can be made in a row. Reflect the choice
                // immediately, then resync with the player once it has applied the change.
                menuAdapter.select(position)
                optionsList.postDelayed(
                    { menuAdapter.submit(optionsItems(optionsTabs.selectedTabPosition)) },
                    250L,
                )
            }
        optionsList.adapter = menuAdapter
        optionsTabs.addTab(optionsTabs.newTab().setText(R.string.player_menu_tab_audio))
        optionsTabs.addTab(optionsTabs.newTab().setText(R.string.player_menu_tab_subtitles))
        optionsTabs.addTab(optionsTabs.newTab().setText(R.string.player_menu_tab_speed))
        optionsTabs.addTab(optionsTabs.newTab().setText(R.string.player_menu_tab_offset))
        val offsetTabSelected = androidx.compose.runtime.mutableStateOf(false)
        optionsOffset.setContent {
            FindroidTheme {
                if (offsetTabSelected.value) {
                    SubtitleOffsetControl(
                        initialDelaySeconds = viewModel.subtitleDelaySeconds,
                        onDelayChange = { seconds ->
                            viewModel.setSubtitleDelay(seconds)
                            showPlayerOverlayMessage(
                                getString(
                                    R.string.player_subtitle_offset_value,
                                    formatSubtitleOffset(seconds),
                                )
                            )
                        },
                    )
                }
            }
        }
        optionsTabs.addOnTabSelectedListener(
            object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab) {
                    offsetTabSelected.value = tab.position == 3
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

        gestureLockButton.isVisible =
            appPreferences.getValue(appPreferences.playerGesturesVolume) ||
                appPreferences.getValue(appPreferences.playerGesturesBrightness) ||
                appPreferences.getValue(appPreferences.playerGesturesZoom)
        gestureLockButton.setOnClickListener {
            isVolumeBrightnessLocked = !isVolumeBrightnessLocked
            gestureLockButton.setImageResource(
                if (isVolumeBrightnessLocked) CoreR.drawable.ic_circle_off
                else CoreR.drawable.ic_circle
            )
            showPlayerOverlayMessage(
                getString(
                    if (isVolumeBrightnessLocked) R.string.player_gestures_locked
                    else R.string.player_gestures_unlocked
                )
            )
        }

        lockButton.setOnClickListener {
            exoPlayerControlView.visibility = View.GONE
            lockedLayout.visibility = View.VISIBLE
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
            isControlsLocked = true
            showPlayerOverlayMessage(getString(R.string.player_screen_locked))
        }

        unlockButton.setOnClickListener {
            exoPlayerControlView.visibility = View.VISIBLE
            lockedLayout.visibility = View.GONE
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            isControlsLocked = false
            isVolumeBrightnessLocked = false
            gestureLockButton.setImageResource(CoreR.drawable.ic_circle)
            showPlayerOverlayMessage(getString(R.string.player_screen_unlocked))
        }

        pipButton.setOnClickListener { pictureInPicture() }

        // Playback statistics: mpv's own counters in a panel below the top row of controls. The
        // panel runs its own polling loop, so mpv is only read while the panel is on screen.
        val statsComposeView = findViewById<ComposeView>(R.id.player_stats_compose)
        statsComposeView.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnDetachedFromWindow
        )
        statsComposeView.setContent {
            val streamInfo by viewModel.streamInfo.collectAsStateWithLifecycle()
            FindroidTheme {
                if (showStats.value && !statsSuppressed.value) {
                    PlayerStatsPanel(
                        loadStats = { viewModel.player.playbackStats() },
                        streamInfo = streamInfo,
                        onRefreshStream = { viewModel.refreshStreamInfo() },
                    )
                }
            }
        }
        statsButton.setOnClickListener {
            showStats.value = !showStats.value
            statsComposeView.isVisible = showStats.value
            // Toggling the panel on while the controls are up places it below them immediately.
            positionStatsPanel(playerControlsVisible.value, animate = false)
        }
        // The controls start out visible, so the panel starts below them without a slide.
        positionStatsPanel(controlsVisible = true, animate = false)

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

    /** Sets up [composeView] with the shared media header content. */
    private fun setMediaHeaderContent(composeView: ComposeView) {
        setDetailComposeContent(composeView) { MediaDetailHeader(it) }
    }

    /**
     * Shared Compose setup for the player's media sheets: provides the theme, backdrop ratios and
     * the loaded media detail, then renders [content].
     */
    private fun setDetailComposeContent(
        composeView: ComposeView,
        content: @Composable (MediaDetailState) -> Unit,
    ) {
        composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        composeView.setContent {
            val detailState by detailViewModel.state.collectAsStateWithLifecycle()
            val itemId = detailItemId.value
            val isEpisode = detailItemIsEpisode.value
            LaunchedEffect(itemId, isEpisode) {
                if (itemId != null) {
                    detailViewModel.load(itemId, isEpisode)
                }
            }
            FindroidTheme {
                CompositionLocalProvider(
                    LocalBackdropRatios provides
                        BackdropRatios(
                            portrait =
                                appPreferences
                                    .getValue(appPreferences.backdropPortraitRatio)
                                    .toFloat(),
                            landscape =
                                appPreferences
                                    .getValue(appPreferences.backdropLandscapeRatio)
                                    .toFloat(),
                            playerSheet =
                                appPreferences
                                    .getValue(appPreferences.playerSheetBackdropRatio)
                                    .toFloatOrNull() ?: (4f / 3f),
                        )
                ) {
                    content(detailState)
                }
            }
        }
    }

    private fun optionsItems(tab: Int): List<PlayerMenuItem> =
        when (tab) {
            0 -> trackItems(C.TRACK_TYPE_AUDIO, CoreR.drawable.ic_music)
            1 -> trackItems(C.TRACK_TYPE_TEXT, CoreR.drawable.ic_case_sensitive)
            2 ->
                playerSpeeds.mapIndexed { index, speed ->
                    PlayerMenuItem(
                        title = playerSpeedTexts[index],
                        subtitle =
                            getString(
                                when {
                                    speed < 1f -> R.string.speed_slower
                                    speed > 1f -> R.string.speed_faster
                                    else -> R.string.speed_normal
                                }
                            ),
                        selected = speed == viewModel.playbackSpeed,
                        iconRes = CoreR.drawable.ic_gauge,
                    )
                }
            else -> emptyList()
        }

    private fun trackItems(type: @C.TrackType Int, iconRes: Int): List<PlayerMenuItem> {
        val groups =
            viewModel.player.currentTracks.groups.filter { it.type == type && it.isSupported }

        val items = mutableListOf<PlayerMenuItem>()
        items.add(
            PlayerMenuItem(
                title = getString(CoreR.string.none),
                subtitle =
                    getString(
                        if (type == C.TRACK_TYPE_AUDIO) R.string.track_none_audio
                        else R.string.track_none_subtitle
                    ),
                selected = groups.none { it.isSelected },
                iconRes = iconRes,
            )
        )
        groups.forEachIndexed { index, group ->
            val format = group.mediaTrackGroup.getFormat(0)
            val language = languageDisplayName(format.language)
            // Prefer the structured language name; fall back to the source label only when the
            // language is unknown.
            val title = language ?: format.label ?: "Track ${index + 1}"
            // The badge shows the simplified track format, which is always available.
            val badge = prettyCodec(format.codecs)
            // The source label carries details we cannot derive (e.g. "Songs only", "External").
            // Shown only when the language is known, since the label becomes the title otherwise;
            // falls back to a placeholder so every row keeps its second line.
            val label =
                format.label?.takeIf { it.isNotBlank() && language != null }
                    ?: getString(R.string.track_unknown)
            items.add(
                PlayerMenuItem(
                    title = title,
                    subtitle = label,
                    selected = group.isSelected,
                    iconRes = iconRes,
                    badge = badge,
                )
            )
        }
        return items
    }

    /**
     * Turns an ISO language code from the media metadata (e.g. "eng") into a display name (e.g.
     * "English"). Returns null for missing/unknown codes so callers can fall back.
     */
    private fun languageDisplayName(code: String?): String? {
        if (code.isNullOrBlank() || code.equals("und", ignoreCase = true)) return null
        val name = Locale.forLanguageTag(code).displayLanguage
        return if (name.isBlank() || name.equals(code, ignoreCase = true)) null else name
    }

    /**
     * Simplifies a raw codec string from the media (e.g. "hdmv_pgs_subtitle") into a short
     * lowercase label for the track badge (e.g. "pgs"). Unknown codecs are passed through.
     */
    private fun prettyCodec(codec: String?): String? {
        val value = codec?.trim()?.lowercase()?.takeIf { it.isNotEmpty() } ?: return null
        return when (value) {
            "hdmv_pgs_subtitle",
            "pgssub" -> "pgs"
            "subrip" -> "srt"
            "ssa" -> "ass"
            "dvd_subtitle",
            "dvdsub" -> "vobsub"
            "eia_608" -> "cc"
            "mp4a" -> "aac"
            else -> value
        }
    }

    private fun showOptionsSheet(overlay: View, panel: View, scrim: View) {
        // Position the panel off-screen before its first visible frame. Doing this in a post()
        // briefly draws it at translationX = 0 (the "already open" flash) before the animation.
        panel.animate().cancel()
        val sheetWidth = if (panel.layoutParams.width > 0) panel.layoutParams.width else panel.width
        panel.translationX = sheetWidth.toFloat()
        overlay.visibility = View.VISIBLE
        panel.animate().translationX(0f).setDuration(200L).start()
        hidePlayerControls()
    }

    private fun hideOptionsSheet(overlay: View, panel: View, scrim: View) {
        panel
            .animate()
            .translationX(panel.width.toFloat())
            .setDuration(200L)
            .withEndAction { overlay.visibility = View.GONE }
            .start()
        // Leave the overlay hidden once the sheet closes; just restore its alpha so the next tap
        // brings it back in.
        val controller = binding.playerView.findViewById<View>(R.id.exo_controller)
        controller?.animate()?.cancel()
        controller?.alpha = 1f
        binding.playerView.hideController()
    }

    /**
     * Fades the player overlay (title, seek bar, buttons) in, and restarts its hide clock. Every
     * show goes through here so the overlay always arrives the same way.
     */
    internal fun showPlayerControls() {
        binding.playerView.showController()
        playerControlsVisible.value = true
        positionStatsPanel(controlsVisible = true)
        // media3 reports visibility changes only, and an overlay that is already visible does not
        // change, so the clock is restarted here as well as from the listener.
        schedulePlayerControlsHide()
    }

    /**
     * Slides the stats panel between its resting spot (12dp from the corner) and the spot below the
     * 48dp controls row (12 + 48 + 12dp). Animating the view's own translation keeps the panel
     * inside the parent instead of clipping it like a Compose offset would.
     */
    private fun positionStatsPanel(controlsVisible: Boolean, animate: Boolean = true) {
        val statsPanel = findViewById<View>(R.id.player_stats_compose) ?: return
        val target =
            if (controlsVisible) STATS_SHOWN_OFFSET_DP * resources.displayMetrics.density else 0f
        statsPanel.animate().cancel()
        if (!animate) {
            statsPanel.translationY = target
            return
        }
        statsPanel
            .animate()
            .translationY(target)
            .setDuration(OVERLAY_FADE_DURATION_MS)
            .setInterpolator(OverlayFadeInInterpolator)
            .start()
    }

    /**
     * Fades the overlay in, from nothing, so its first visible frame is not a full-opacity flash.
     */
    private fun fadePlayerControlsIn() {
        val controller = overlayController() ?: return
        controller.animate().cancel()
        controller.alpha = 0f
        controller
            .animate()
            .alpha(1f)
            .setDuration(OVERLAY_FADE_DURATION_MS)
            .setInterpolator(OverlayFadeInInterpolator)
            .start()
    }

    /**
     * Fades the player overlay out and only then hides it, so it goes slowly enough to read as one
     * movement. Used by the hide gesture and when a sheet takes the screen.
     */
    internal fun hidePlayerControls() {
        val controller = overlayController() ?: return
        handler.removeCallbacks(hidePlayerControlsAction)
        controller.animate().cancel()
        playerControlsVisible.value = false
        positionStatsPanel(controlsVisible = false)
        controller
            .animate()
            .alpha(0f)
            .setDuration(OVERLAY_FADE_DURATION_MS)
            .setInterpolator(OverlayFadeOutInterpolator)
            .withEndAction {
                binding.playerView.hideController()
                // Ready for the next fade in, which starts from nothing again.
                controller.alpha = 1f
            }
            .start()
    }

    /** Restarts the overlay's hide clock, for the interactions that should keep it up. */
    internal fun keepPlayerControlsVisible() = schedulePlayerControlsHide()

    private fun schedulePlayerControlsHide() {
        handler.removeCallbacks(hidePlayerControlsAction)
        if (overlayTimeoutMs > 0) {
            handler.postDelayed(hidePlayerControlsAction, overlayTimeoutMs)
        }
    }

    /**
     * Play and pause arrive from anywhere - a headset button, the keyboard, the media session - and
     * media3 shows the overlay for them without any touch. Restart the clock on those, so an
     * overlay that appears for a pause still leaves on its own.
     */
    private val controlsClockListener =
        object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (binding.playerView.isControllerFullyVisible) schedulePlayerControlsHide()
            }
        }

    /** The whole overlay, faded in and out as one piece. */
    private fun overlayController(): View? = binding.playerView.findViewById(R.id.exo_controller)

    /**
     * Reparents [view] into this list as a non-selectable header so it scrolls together with the
     * rows. Must be called before the adapter is set.
     */
    private fun ListView.addScrollableHeader(view: View) {
        (view.parent as? ViewGroup)?.removeView(view)
        view.layoutParams =
            AbsListView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        addHeaderView(view, null, false)
    }

    /** Briefly shows [message] in the same pill used for the seek/speed-up overlay. */
    internal fun showPlayerOverlayMessage(message: String) {
        binding.progressScrubberTrickplay.visibility = View.GONE
        binding.progressScrubberText.text = message
        binding.progressScrubberLayout.visibility = View.VISIBLE
        binding.progressScrubberLayout.removeCallbacks(hidePlayerOverlayMessageAction)
        binding.progressScrubberLayout.postDelayed(hidePlayerOverlayMessageAction, 1500L)
    }

    private fun updatePlayerTimeRight() {
        val duration = viewModel.player.duration
        if (duration <= 0 || duration == C.TIME_UNSET) {
            playerTimeRight.text = ""
            return
        }

        val position = viewModel.player.currentPosition
        val remaining = (duration - position).coerceAtLeast(0)

        playerTimeRight.text =
            if (appPreferences.getValue(appPreferences.playerTimeDisplay) == "finish") {
                when (appPreferences.getValue(appPreferences.playerTimeEndFormat)) {
                    "24" ->
                        getString(
                            R.string.player_time_finished_at,
                            formatClockTime(System.currentTimeMillis() + remaining, true),
                        )
                    "12" ->
                        getString(
                            R.string.player_time_finished_at,
                            formatClockTime(System.currentTimeMillis() + remaining, false),
                        )
                    else -> {
                        val totalMinutes = ((remaining + 59_999) / 60_000).coerceAtLeast(0)
                        getString(R.string.player_time_finished_in, totalMinutes)
                    }
                }
            } else {
                formatPlayerTime(duration)
            }
    }

    private fun formatPlayerTime(timeMs: Long): String {
        val totalSeconds = (timeMs / 1000).coerceAtLeast(0)
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }

    private fun formatClockTime(timeMs: Long, use24Hour: Boolean): String {
        val pattern = if (use24Hour) "HH:mm" else "h:mma"
        return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(timeMs))
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration,
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        viewModel.isInPictureInPictureMode = isInPictureInPictureMode
        // The stats box is unreadable at PiP size, so it follows the controls out of the way.
        statsSuppressed.value = isInPictureInPictureMode
        when (isInPictureInPictureMode) {
            true -> {
                binding.playerView.useController = false
                skipSegmentButton.isVisible = false

                wasZoom = playerGestureHelper?.isZoomEnabled == true
                playerGestureHelper?.updateZoomMode(false)
            }

            false -> {
                binding.playerView.useController = true
                playerGestureHelper?.updateZoomMode(wasZoom)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.player.removeListener(controlsClockListener)
        // These locks are process-wide; reset them so a fresh player starts unlocked and the
        // buttons' icons match the state.
        isControlsLocked = false
        isVolumeBrightnessLocked = false
    }
}
