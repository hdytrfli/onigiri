package dev.hdytrfli.onigiri.player.local.music

import android.app.PendingIntent
import android.content.Intent
import android.media.audiofx.LoudnessEnhancer
import android.os.Bundle
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ShuffleOrder.DefaultShuffleOrder
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.AndroidEntryPoint
import dev.hdytrfli.onigiri.settings.domain.AppPreferences
import java.util.UUID
import javax.inject.Inject
import kotlin.math.min
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Owns music playback while the app is in the background. Media3's service keeps the ExoPlayer,
 * media session and notification alive independently of any activity.
 */
private data class AudioPreferences(
    val gapless: Boolean,
    val crossfade: Boolean,
    val crossfadeSeconds: Int,
    val normalization: Boolean,
)

@AndroidEntryPoint
class MusicPlaybackService : MediaSessionService() {
    @Inject lateinit var appPreferences: AppPreferences

    private var mediaSession: MediaSession? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var crossfadeEnabled = false
    private var crossfadeDurationMs = 5_000L
    private var normalizationEnabled = false
    private var normalizer: LoudnessEnhancer? = null
    private var trackStartPosition = 0L

    private val sessionCallback =
        object : MediaSession.Callback {
            override fun onConnect(
                session: MediaSession,
                controller: MediaSession.ControllerInfo,
            ): MediaSession.ConnectionResult {
                val connection = super.onConnect(session, controller)
                val commands =
                    connection.availableSessionCommands
                        .buildUpon()
                        .add(MusicSessionCommands.SetShuffleOrder)
                        .build()
                return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                    .setAvailableSessionCommands(commands)
                    .build()
            }

            override fun onCustomCommand(
                session: MediaSession,
                controller: MediaSession.ControllerInfo,
                customCommand: SessionCommand,
                args: Bundle,
            ): ListenableFuture<SessionResult> {
                if (customCommand.customAction != MusicSessionCommands.ACTION_SET_SHUFFLE_ORDER) {
                    return super.onCustomCommand(session, controller, customCommand, args)
                }
                val order =
                    args.getIntArray(MusicSessionCommands.EXTRA_SHUFFLE_ORDER)
                        ?: return Futures.immediateFuture(
                            SessionResult(SessionResult.RESULT_ERROR_BAD_VALUE)
                        )
                val seed =
                    args.getLong(
                        MusicSessionCommands.EXTRA_SHUFFLE_SEED,
                        System.currentTimeMillis(),
                    )
                (session.player as? ExoPlayer)?.let { player ->
                    applyShuffleOrderWhenReady(player, order, seed)
                }
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
        }

    private fun applyShuffleOrderWhenReady(
        player: ExoPlayer,
        order: IntArray,
        seed: Long,
    ) {
        if (player.mediaItemCount == order.size) {
            player.setShuffleOrder(DefaultShuffleOrder(order, seed))
            return
        }
        val listener =
            object : Player.Listener {
                override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                    if (player.mediaItemCount != order.size) return
                    player.removeListener(this)
                    player.setShuffleOrder(DefaultShuffleOrder(order, seed))
                }
            }
        player.addListener(listener)
    }

    override fun onCreate() {
        super.onCreate()

        val player =
            ExoPlayer.Builder(this)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                        .build(),
                    true,
                )
                .setHandleAudioBecomingNoisy(true)
                .setWakeMode(C.WAKE_MODE_NETWORK)
                .build()

        serviceScope.launch {
            combine(
                    appPreferences.observe(appPreferences.musicGapless),
                    appPreferences.observe(appPreferences.musicCrossfade),
                    appPreferences.observe(appPreferences.musicCrossfadeDuration),
                    appPreferences.observe(appPreferences.musicAudioNormalization),
                ) { gapless, crossfade, duration, normalization ->
                    AudioPreferences(
                        gapless = gapless,
                        crossfade = crossfade,
                        crossfadeSeconds = duration.toIntOrNull() ?: 5,
                        normalization = normalization,
                    )
                }
                .collect { prefs ->
                    player.pauseAtEndOfMediaItems = !prefs.gapless
                    crossfadeEnabled = prefs.crossfade
                    crossfadeDurationMs = prefs.crossfadeSeconds.coerceAtLeast(1) * 1_000L
                    normalizationEnabled = prefs.normalization
                    if (!prefs.crossfade) player.volume = 1f
                }
        }

        player.addListener(
            object : Player.Listener {
                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    trackStartPosition = 0L
                    if (crossfadeEnabled) player.volume = 0f
                }
            }
        )

        serviceScope.launch {
            while (isActive) {
                delay(100)
                if (crossfadeEnabled && player.isPlaying) {
                    val duration = player.duration
                    val position = player.currentPosition
                    if (duration > 0L && duration != C.TIME_UNSET) {
                        val fadeIn =
                            ((position - trackStartPosition).toFloat() / crossfadeDurationMs)
                                .coerceIn(0f, 1f)
                        val fadeOut =
                            ((duration - position).toFloat() / crossfadeDurationMs).coerceIn(0f, 1f)
                        player.volume = min(fadeIn, fadeOut)
                    } else {
                        player.volume = 1f
                    }
                } else if (!crossfadeEnabled && player.volume != 1f) {
                    player.volume = 1f
                }

                if (normalizationEnabled) {
                    if (normalizer == null && player.audioSessionId != C.AUDIO_SESSION_ID_UNSET) {
                        runCatching {
                            normalizer =
                                LoudnessEnhancer(player.audioSessionId).apply {
                                    setTargetGain(300)
                                    enabled = true
                                }
                        }
                    }
                } else {
                    normalizer?.release()
                    normalizer = null
                }
            }
        }

        val sessionActivity =
            PendingIntent.getActivity(
                this,
                0,
                Intent()
                    .setClassName(packageName, "dev.hdytrfli.onigiri.MusicPlayerActivity")
                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

        mediaSession =
            MediaSession.Builder(this, player)
                .setId("music-${UUID.randomUUID()}")
                .setSessionActivity(sessionActivity)
                .setCallback(sessionCallback)
                .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        serviceScope.cancel()
        normalizer?.release()
        normalizer = null
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }
}
