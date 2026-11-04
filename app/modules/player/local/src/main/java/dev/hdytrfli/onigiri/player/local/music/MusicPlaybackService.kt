package dev.hdytrfli.onigiri.player.local.music

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ShuffleOrder.DefaultShuffleOrder
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.AndroidEntryPoint
import java.util.UUID

/**
 * Owns music playback while the app is in the background. Media3's service keeps the ExoPlayer,
 * media session and notification alive independently of any activity.
 */
@AndroidEntryPoint
class MusicPlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null

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
                (session.player as? ExoPlayer)?.setShuffleOrder(DefaultShuffleOrder(order, seed))
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
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
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }
}
