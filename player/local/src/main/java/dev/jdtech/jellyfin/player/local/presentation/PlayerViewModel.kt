package dev.jdtech.jellyfin.player.local.presentation

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.models.FindroidEpisode
import dev.jdtech.jellyfin.models.FindroidSegment
import dev.jdtech.jellyfin.models.FindroidSegmentType
import dev.jdtech.jellyfin.player.core.domain.models.PlayerChapter
import dev.jdtech.jellyfin.player.core.domain.models.PlayerItem
import dev.jdtech.jellyfin.player.core.domain.models.Trickplay
import dev.jdtech.jellyfin.player.local.R
import dev.jdtech.jellyfin.player.local.domain.PlaylistManager
import dev.jdtech.jellyfin.player.local.domain.TrackSelectionStore
import dev.jdtech.jellyfin.player.local.mpv.MPVPlayer
import dev.jdtech.jellyfin.repository.JellyfinRepository
import dev.jdtech.jellyfin.settings.domain.AppPreferences
import dev.jdtech.jellyfin.settings.domain.Constants
import java.util.UUID
import javax.inject.Inject
import kotlin.math.ceil
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ItemFields
import timber.log.Timber

@HiltViewModel
class PlayerViewModel
@Inject
constructor(
    private val application: Application,
    private val playlistManager: PlaylistManager,
    private val repository: JellyfinRepository,
    private val appPreferences: AppPreferences,
    private val trackSelectionStore: TrackSelectionStore,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel(), Player.Listener {
    val player: Player

    private val _uiState =
        MutableStateFlow(
            UiState(
                currentItemTitle = "",
                currentSegment = null,
                currentSkipButtonStringRes = R.string.player_controls_skip_intro,
                currentTrickplay = null,
                currentChapters = emptyList(),
                fileLoaded = false,
            )
        )
    val uiState = _uiState.asStateFlow()

    private val eventsChannel = Channel<PlayerEvents>()
    val eventsChannelFlow = eventsChannel.receiveAsFlow()

    data class UiState(
        val currentItemTitle: String,
        val currentSegment: FindroidSegment?,
        val currentSkipButtonStringRes: Int,
        val currentTrickplay: Trickplay?,
        val currentChapters: List<PlayerChapter>,
        val fileLoaded: Boolean,
        val currentItemLogo: String? = null,
        val currentItemOverview: String? = null,
        val currentEpisodeName: String? = null,
        val currentItemId: UUID? = null,
        val currentItemIsEpisode: Boolean = false,
        val currentSeasonEpisodes: List<FindroidEpisode> = emptyList(),
        val currentEpisodeId: UUID? = null,
    )

    private var items: MutableList<PlayerItem> = mutableListOf()

    var playWhenReady = true
    private var currentMediaItemIndex = savedStateHandle["mediaItemIndex"] ?: 0
    private var playbackPosition: Long = savedStateHandle["position"] ?: 0
    private var currentMediaItemSegments: List<FindroidSegment> = emptyList()

    // Segments preferences
    var segmentsSkipButton: Boolean = false
    private var segmentsSkipButtonTypes: Set<String> = emptySet()
    var segmentsSkipButtonDuration: Long = 0L
    var segmentsAutoSkip: Boolean = false
    private var segmentsAutoSkipTypes: Set<String> = emptySet()
    private var segmentsAutoSkipMode: String = "always"

    var playbackSpeed: Float = 1f

    /** When on, play any subtitle if none matches the preferred subtitle language. */
    var alwaysPlaySubtitles: Boolean = false

    /** Subtitle timing offset in seconds (positive = subtitles appear later). */
    var subtitleDelaySeconds: Float = 0f
        private set

    fun setSubtitleDelay(seconds: Float) {
        subtitleDelaySeconds = seconds
        (player as? MPVPlayer)?.setSubtitleDelay(seconds.toDouble())
        val itemId = currentItemId()
        val scope = currentTrackScope()
        if (itemId != null && scope != null) {
            trackSelectionStore.setSubtitleOffset(scope, itemId, seconds)
        }
    }

    /** Restores the subtitle offset remembered for the current item, if any. */
    private fun applyRememberedSubtitleOffset() {
        val itemId = currentItemId() ?: return
        val scope = currentTrackScope() ?: return
        val offset = trackSelectionStore.getSubtitleOffset(scope, itemId)
        if (offset.isNaN()) return
        subtitleDelaySeconds = offset
        (player as? MPVPlayer)?.setSubtitleDelay(offset.toDouble())
    }

    /** The item whose remembered track selection was already applied, so it isn't re-applied. */
    private var appliedSelectionItemId: String? = null

    var isInPictureInPictureMode: Boolean = false

    init {
        alwaysPlaySubtitles = appPreferences.getValue(appPreferences.playerAlwaysPlaySubtitles)

        segmentsSkipButton = appPreferences.getValue(appPreferences.playerMediaSegmentsSkipButton)
        segmentsSkipButtonTypes =
            appPreferences.getValue(appPreferences.playerMediaSegmentsSkipButtonType)
        segmentsSkipButtonDuration =
            appPreferences.getValue(appPreferences.playerMediaSegmentsSkipButtonDuration)
        segmentsAutoSkip = appPreferences.getValue(appPreferences.playerMediaSegmentsAutoSkip)
        segmentsAutoSkipTypes =
            appPreferences.getValue(appPreferences.playerMediaSegmentsAutoSkipType)
        segmentsAutoSkipMode =
            appPreferences.getValue(appPreferences.playerMediaSegmentsAutoSkipMode)

        val audioAttributes =
            AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .setUsage(C.USAGE_MEDIA)
                .build()

        val trackSelectionParameters =
            TrackSelectionParameters.Builder(application)
                .setPreferredAudioLanguage(
                    appPreferences.getValue(appPreferences.preferredAudioLanguage)
                )
                .setPreferredTextLanguage(
                    appPreferences.getValue(appPreferences.preferredSubtitleLanguage)
                )
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                .build()

        val subOptions =
            dev.jdtech.jellyfin.settings.domain.MpvSubtitleOptions.buildOptions(appPreferences)
        player =
            MPVPlayer.Builder(application)
                .setAudioAttributes(audioAttributes, true)
                .setTrackSelectionParameters(trackSelectionParameters)
                .setSeekBackIncrementMs(appPreferences.getValue(appPreferences.playerSeekBackInc))
                .setSeekForwardIncrementMs(
                    appPreferences.getValue(appPreferences.playerSeekForwardInc)
                )
                .setPauseAtEndOfMediaItems(true)
                .setVideoOutput(appPreferences.getValue(appPreferences.playerMpvVo))
                .setAudioOutput(appPreferences.getValue(appPreferences.playerMpvAo))
                .setHwDec(appPreferences.getValue(appPreferences.playerMpvHwdec))
                .setSubFontSize(subOptions.getValue("sub-font-size"))
                .setSubColor(subOptions.getValue("sub-color"))
                .setSubPos(subOptions.getValue("sub-pos"))
                .setSubUseMargins(subOptions.getValue("sub-use-margins"))
                .setSubBold(subOptions.getValue("sub-bold"))
                .setSubItalic(subOptions.getValue("sub-italic"))
                .setSubBorderSize(subOptions.getValue("sub-outline-size"))
                .setSubBorderColor(subOptions.getValue("sub-outline-color"))
                .setSubBackColor(subOptions.getValue("sub-back-color"))
                .setSubBorderStyle(subOptions.getValue("sub-border-style"))
                .setSubAssOverride(subOptions.getValue("sub-ass-override"))
                .setSubShadowOffset(subOptions.getValue("sub-shadow-offset"))
                .build()
    }

    fun initializePlayer(itemId: UUID, itemKind: String, startFromBeginning: Boolean) {
        player.addListener(this)

        // A different item is about to play: drop the previous item's episode list so the episodes
        // sheet can't show stale data (e.g. when replacing the media via PiP).
        _uiState.update {
            it.copy(
                currentSeasonEpisodes = emptyList(),
                currentEpisodeId = null,
                currentItemIsEpisode = false,
            )
        }

        viewModelScope.launch {
            val startItem =
                try {
                    playlistManager.getInitialItem(
                        itemId = itemId,
                        itemKind = BaseItemKind.fromName(itemKind),
                        mediaSourceIndex = null,
                        startFromBeginning = startFromBeginning,
                    )
                } catch (e: Exception) {
                    Timber.e(e)
                    Toast.makeText(application, e.localizedMessage, Toast.LENGTH_LONG).show()
                    null
                }

            if (startItem == null) {
                Timber.e("No start item, stopping player initialization")
                return@launch
            }

            items = listOfNotNull(startItem).toMutableList()
            currentMediaItemIndex = items.indexOf(startItem)

            val mediaItems = mutableListOf<MediaItem>()
            try {
                for (item in items) {
                    mediaItems.add(item.toMediaItem())
                }
            } catch (e: Exception) {
                Timber.e(e)
            }

            val startPosition =
                if (playbackPosition == 0L) {
                    items.getOrNull(currentMediaItemIndex)?.playbackPosition ?: C.TIME_UNSET
                } else {
                    playbackPosition
                }

            player.setMediaItems(mediaItems, 0, startPosition)
            player.prepare()
            player.play()
        }
    }

    private fun PlayerItem.toMediaItem(): MediaItem {
        val streamUrl = mediaSourceUri
        val mediaSubtitles = externalSubtitles.map { externalSubtitle ->
            MediaItem.SubtitleConfiguration.Builder(externalSubtitle.uri)
                .setLabel(
                    externalSubtitle.title.ifBlank { application.getString(R.string.external) }
                )
                .setMimeType(externalSubtitle.mimeType)
                .setLanguage(externalSubtitle.language)
                .build()
        }

        Timber.d("Stream url: $streamUrl")
        val mediaItem =
            MediaItem.Builder()
                .setMediaId(itemId.toString())
                .setUri(streamUrl)
                .setMediaMetadata(MediaMetadata.Builder().setTitle(name).build())
                .setSubtitleConfigurations(mediaSubtitles)
                .build()

        return mediaItem
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun releasePlayer() {
        val mediaId = player.currentMediaItem?.mediaId
        val position = player.currentPosition
        val duration = player.duration
        GlobalScope.launch {
            delay(200L)
            try {
                if (mediaId != null && duration != C.TIME_UNSET) {
                    Timber.d("Sending playback stop")
                    repository.postPlaybackStop(
                        UUID.fromString(mediaId),
                        position.times(10000),
                        position.div(duration.toFloat()).times(100).toInt(),
                    )
                }
            } catch (e: Exception) {
                Timber.e(e)
            }
        }

        _uiState.update { it.copy(currentTrickplay = null) }
        playWhenReady = false
        playbackPosition = 0L
        currentMediaItemIndex = 0
        player.removeListener(this)
        player.release()
    }

    fun updatePlaybackProgress() {
        Timber.d("Updating playback progress")
        viewModelScope.launch(Dispatchers.Main) {
            savedStateHandle["position"] = player.currentPosition
            if (player.currentMediaItem != null && player.currentMediaItem!!.mediaId.isNotEmpty()) {
                val itemId = UUID.fromString(player.currentMediaItem!!.mediaId)
                try {
                    repository.postPlaybackProgress(
                        itemId,
                        player.currentPosition.times(10000),
                        !player.isPlaying,
                    )
                } catch (e: Exception) {
                    Timber.e(e)
                }
            }
        }
    }

    fun updateCurrentSegment() {
        Timber.d("Updating current segment")
        viewModelScope.launch(Dispatchers.Main) {
            if (currentMediaItemSegments.isEmpty()) {
                return@launch
            }

            val milliSeconds = player.currentPosition

            // Get current segment, - 100 milliseconds to avoid showing button after segment ends
            val currentSegment = currentMediaItemSegments.find { segment ->
                milliSeconds in segment.startTicks..<(segment.endTicks - 100L)
            }

            if (currentSegment == null) {
                // Remove button if not pressed and there is no current segment
                if (_uiState.value.currentSegment != null) {
                    _uiState.update { it.copy(currentSegment = null) }
                }
                return@launch
            }

            Timber.tag("SegmentInfo").d("currentSegment: %s", currentSegment)

            if (
                segmentsAutoSkip &&
                    segmentsAutoSkipTypes.contains(currentSegment.type.toString()) &&
                    (segmentsAutoSkipMode == Constants.PlayerMediaSegmentsAutoSkip.ALWAYS ||
                        (segmentsAutoSkipMode == Constants.PlayerMediaSegmentsAutoSkip.PIP &&
                            isInPictureInPictureMode))
            ) {
                // Auto Skip segment
                skipSegment(currentSegment)
            } else if (segmentsSkipButtonTypes.contains(currentSegment.type.toString())) {
                // Skip Button segment
                _uiState.update {
                    it.copy(
                        currentSegment = currentSegment,
                        currentSkipButtonStringRes = getSkipButtonTextStringId(currentSegment),
                    )
                }
            } else {
                _uiState.update { it.copy(currentSegment = null) }
            }
        }
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        Timber.d("Playing MediaItem: ${mediaItem?.mediaId}")
        appliedSelectionItemId = null
        subtitleDelaySeconds = 0f
        (player as? MPVPlayer)?.setSubtitleDelay(0.0)
        applyRememberedSubtitleOffset()
        savedStateHandle["mediaItemIndex"] = player.currentMediaItemIndex
        viewModelScope.launch {
            try {
                items
                    .first { it.itemId.toString() == player.currentMediaItem?.mediaId }
                    .let { item ->
                        val itemTitle =
                            if (item.parentIndexNumber != null && item.indexNumber != null) {
                                if (item.indexNumberEnd == null) {
                                    "S${item.parentIndexNumber}:E${item.indexNumber} - ${item.name}"
                                } else {
                                    "S${item.parentIndexNumber}:E${item.indexNumber}-${item.indexNumberEnd} - ${item.name}"
                                }
                            } else {
                                item.name
                            }
                        val episodeName =
                            if (item.parentIndexNumber != null && item.indexNumber != null) {
                                application.getString(
                                    R.string.episode_name,
                                    item.indexNumber,
                                    item.name,
                                )
                            } else {
                                null
                            }
                        _uiState.update {
                            it.copy(
                                currentItemTitle = itemTitle,
                                currentItemLogo = item.logoUri,
                                currentItemOverview = item.overview,
                                currentEpisodeName = episodeName,
                                currentItemId = item.itemId,
                                currentItemIsEpisode = item.parentIndexNumber != null,
                                // Movies have no episode list; clear the previous series' one.
                                currentSeasonEpisodes =
                                    if (item.parentIndexNumber != null) {
                                        it.currentSeasonEpisodes
                                    } else {
                                        emptyList()
                                    },
                                currentEpisodeId =
                                    if (item.parentIndexNumber != null) it.currentEpisodeId
                                    else null,
                                currentSegment = null,
                                currentChapters = item.chapters,
                                fileLoaded = false,
                            )
                        }

                        if (item.parentIndexNumber != null) {
                            loadSeasonEpisodes(item.itemId)
                        }

                        repository.postPlaybackStart(item.itemId)

                        if (segmentsSkipButton || segmentsAutoSkip) {
                            getSegments(item.itemId)
                        }

                        if (appPreferences.getValue(appPreferences.playerTrickplay)) {
                            getTrickplay(item)
                        }

                        playlistManager.setCurrentMediaItemIndex(item.itemId)

                        val previousItem = playlistManager.getPreviousPlayerItem()
                        if (previousItem != null) {
                            items.add(player.currentMediaItemIndex, previousItem)
                            player.addMediaItem(
                                player.currentMediaItemIndex,
                                previousItem.toMediaItem(),
                            )
                        }

                        val nextItem = playlistManager.getNextPlayerItem()
                        if (nextItem != null) {
                            items.add(player.currentMediaItemIndex + 1, nextItem)
                            player.addMediaItem(
                                player.currentMediaItemIndex + 1,
                                nextItem.toMediaItem(),
                            )
                        }

                        Timber.tag("PlayerItems").d(items.map { it.indexNumber }.toString())
                    }
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }

    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
        // Report playback stopped for current item and transition to the next one
        if (
            !playWhenReady &&
                reason == Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM &&
                player.playbackState == Player.STATE_READY
        ) {
            viewModelScope.launch {
                val mediaId = player.currentMediaItem?.mediaId
                val position = player.currentPosition
                val duration = player.duration
                try {
                    repository.postPlaybackStop(
                        UUID.fromString(mediaId),
                        position.times(10000),
                        position.div(duration.toFloat()).times(100).toInt(),
                    )
                } catch (e: Exception) {
                    Timber.e(e)
                }
                player.seekToNextMediaItem()
                player.play()
            }
        }
    }

    override fun onTracksChanged(tracks: Tracks) {
        applyRememberedTrackSelection(tracks)
        autoSelectSubtitles(tracks)
    }

    /**
     * When "always play subtitles" is on, selects the first available subtitle if none is selected
     * and the user has no remembered choice for this item.
     */
    private fun autoSelectSubtitles(tracks: Tracks) {
        if (!alwaysPlaySubtitles) return
        val itemId = currentItemId()
        val scope = currentTrackScope()
        if (
            itemId != null &&
                scope != null &&
                trackSelectionStore.getText(scope, itemId) != TrackSelectionStore.UNSET
        ) {
            return
        }
        val hasSelectedText =
            tracks.groups.any { group -> group.type == C.TRACK_TYPE_TEXT && group.isSelected }
        if (hasSelectedText) return
        val textGroup =
            tracks.groups.firstOrNull { group ->
                group.type == C.TRACK_TYPE_TEXT && group.isSupported
            }
        if (textGroup != null) {
            player.trackSelectionParameters =
                player.trackSelectionParameters
                    .buildUpon()
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                    .setOverrideForType(TrackSelectionOverride(textGroup.mediaTrackGroup, 0))
                    .build()
        }
    }

    /**
     * Restores the audio/subtitle track the user chose for this item the last time it was played,
     * once per item. An explicit choice wins over the preferred-language defaults.
     */
    private fun applyRememberedTrackSelection(tracks: Tracks) {
        val itemId = currentItemId() ?: return
        if (appliedSelectionItemId == itemId.toString()) return
        val scope = currentTrackScope() ?: return

        val storedAudio = trackSelectionStore.getAudio(scope, itemId)
        val storedText = trackSelectionStore.getText(scope, itemId)
        if (storedAudio == TrackSelectionStore.UNSET && storedText == TrackSelectionStore.UNSET) {
            appliedSelectionItemId = itemId.toString()
            return
        }

        val parameters = player.trackSelectionParameters.buildUpon()

        if (storedAudio != TrackSelectionStore.UNSET) {
            applyTrackSelection(parameters, tracks, C.TRACK_TYPE_AUDIO, storedAudio)
        }
        if (storedText != TrackSelectionStore.UNSET) {
            applyTrackSelection(parameters, tracks, C.TRACK_TYPE_TEXT, storedText)
        }

        player.trackSelectionParameters = parameters.build()
        appliedSelectionItemId = itemId.toString()
    }

    /** Applies [index] (or [TrackSelectionStore.DISABLED]) for [trackType] to [parameters]. */
    private fun applyTrackSelection(
        parameters: TrackSelectionParameters.Builder,
        tracks: Tracks,
        trackType: @C.TrackType Int,
        index: Int,
    ) {
        if (index == TrackSelectionStore.DISABLED) {
            parameters.clearOverridesOfType(trackType).setTrackTypeDisabled(trackType, true)
            return
        }
        val group =
            tracks.groups.filter { it.type == trackType && it.isSupported }.getOrNull(index)
                ?: return
        parameters
            .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, 0))
            .setTrackTypeDisabled(trackType, false)
    }

    private fun currentItemId(): UUID? =
        player.currentMediaItem?.mediaId?.let { runCatching { UUID.fromString(it) }.getOrNull() }

    /**
     * Identity of the active server + user, used to scope the remembered track choices so they are
     * dropped when either changes.
     */
    private fun currentTrackScope(): String? {
        val serverId = appPreferences.getValue(appPreferences.currentServer) ?: return null
        val userId = runCatching { repository.getUserId() }.getOrNull() ?: return null
        return "$serverId:$userId"
    }

    override fun onPlaybackStateChanged(state: Int) {
        var stateString = "UNKNOWN_STATE             -"
        when (state) {
            Player.STATE_IDLE -> {
                stateString = "Player.STATE_IDLE      -"
            }
            Player.STATE_BUFFERING -> {
                stateString = "Player.STATE_BUFFERING -"
            }
            Player.STATE_READY -> {
                stateString = "Player.STATE_READY     -"
                _uiState.update { it.copy(fileLoaded = true) }
            }
            Player.STATE_ENDED -> {
                stateString = "Player.STATE_ENDED     -"
                eventsChannel.trySend(PlayerEvents.NavigateBack)
            }
        }
        Timber.d("Changed player state to $stateString")
    }

    override fun onCleared() {
        super.onCleared()
        Timber.d("Clearing Player ViewModel")
        releasePlayer()
    }

    fun switchToTrack(trackType: @C.TrackType Int, index: Int) {
        // Remember the choice for this item so it is restored the next time it plays.
        val scope = currentTrackScope()
        val itemId = currentItemId()
        if (scope != null && itemId != null) {
            when (trackType) {
                C.TRACK_TYPE_AUDIO -> trackSelectionStore.setAudio(scope, itemId, index)
                C.TRACK_TYPE_TEXT -> trackSelectionStore.setText(scope, itemId, index)
            }
        }
        // Index -1 equals disable track
        if (index == -1) {
            player.trackSelectionParameters =
                player.trackSelectionParameters
                    .buildUpon()
                    .clearOverridesOfType(trackType)
                    .setTrackTypeDisabled(trackType, true)
                    .build()
        } else {
            player.trackSelectionParameters =
                player.trackSelectionParameters
                    .buildUpon()
                    .setOverrideForType(
                        TrackSelectionOverride(
                            player.currentTracks.groups
                                .filter { it.type == trackType && it.isSupported }[index]
                                .mediaTrackGroup,
                            0,
                        )
                    )
                    .setTrackTypeDisabled(trackType, false)
                    .build()
        }
    }

    fun selectSpeed(speed: Float) {
        player.setPlaybackSpeed(speed)
        playbackSpeed = speed
    }

    private suspend fun loadSeasonEpisodes(itemId: UUID) {
        try {
            val episode = repository.getEpisode(itemId)
            val episodes =
                repository.getEpisodes(
                    seriesId = episode.seriesId,
                    seasonId = episode.seasonId,
                    fields = listOf(ItemFields.OVERVIEW),
                )
            _uiState.update { it.copy(currentSeasonEpisodes = episodes, currentEpisodeId = itemId) }
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    fun selectEpisode(itemId: UUID) {
        val existingIndex = items.indexOfFirst { it.itemId == itemId }
        if (existingIndex >= 0) {
            player.seekTo(existingIndex, C.TIME_UNSET)
            return
        }

        // The playlist is built lazily (current item plus its neighbours only), so resolve the
        // requested episode on demand and insert it in season order before seeking to it.
        viewModelScope.launch {
            val playerItem = playlistManager.getPlayerItem(itemId) ?: return@launch
            val targetOrder = playlistManager.getItemIndex(itemId)

            var insertIndex = items.size
            for (i in items.indices) {
                if (playlistManager.getItemIndex(items[i].itemId) > targetOrder) {
                    insertIndex = i
                    break
                }
            }

            items.add(insertIndex, playerItem)
            player.addMediaItem(insertIndex, playerItem.toMediaItem())
            player.seekTo(insertIndex, C.TIME_UNSET)
        }
    }

    private suspend fun getSegments(itemId: UUID) {
        try {
            currentMediaItemSegments = repository.getSegments(itemId)
        } catch (e: Exception) {
            currentMediaItemSegments = emptyList()
            Timber.e(e)
        }
    }

    private suspend fun getTrickplay(item: PlayerItem) {
        val trickplayInfo = item.trickplayInfo ?: return
        Timber.d("Trickplay Resolution: ${trickplayInfo.width}")

        withContext(Dispatchers.Default) {
            val maxIndex =
                ceil(
                        trickplayInfo.thumbnailCount
                            .toDouble()
                            .div(trickplayInfo.tileWidth * trickplayInfo.tileHeight)
                    )
                    .toInt()
            val bitmaps = mutableListOf<Bitmap>()

            for (i in 0..maxIndex) {
                repository.getTrickplayData(item.itemId, trickplayInfo.width, i)?.let { byteArray ->
                    val fullBitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
                    for (offsetY in
                        0..<trickplayInfo.height * trickplayInfo.tileHeight step
                            trickplayInfo.height) {
                        for (offsetX in
                            0..<trickplayInfo.width * trickplayInfo.tileWidth step
                                trickplayInfo.width) {
                            val bitmap =
                                Bitmap.createBitmap(
                                    fullBitmap,
                                    offsetX,
                                    offsetY,
                                    trickplayInfo.width,
                                    trickplayInfo.height,
                                )
                            bitmaps.add(bitmap)
                        }
                    }
                }
            }
            _uiState.update {
                it.copy(currentTrickplay = Trickplay(trickplayInfo.interval, bitmaps))
            }
        }
    }

    fun skipSegment(segment: FindroidSegment) {
        if (shouldSkipToNextEpisode(segment)) {
            player.seekToNextMediaItem()
        } else {
            player.seekTo(segment.endTicks)
        }
        _uiState.update { it.copy(currentSegment = null) }
    }

    // Check if the outro segment's end time is within n milliseconds of the player's total duration
    private fun shouldSkipToNextEpisode(segment: FindroidSegment): Boolean {
        return if (segment.type == FindroidSegmentType.OUTRO && player.hasNextMediaItem()) {
            val segmentEndTimeMillis = segment.endTicks
            val playerDurationMillis = player.duration
            val thresholdMillis =
                playerDurationMillis -
                    appPreferences.getValue(appPreferences.playerMediaSegmentsNextEpisodeThreshold)

            segmentEndTimeMillis > thresholdMillis
        } else {
            false
        }
    }

    private fun getSkipButtonTextStringId(segment: FindroidSegment): Int {
        return when (shouldSkipToNextEpisode(segment)) {
            true -> R.string.player_controls_next_episode
            false ->
                when (segment.type) {
                    FindroidSegmentType.INTRO -> R.string.player_controls_skip_intro
                    FindroidSegmentType.OUTRO -> R.string.player_controls_skip_outro
                    FindroidSegmentType.RECAP -> R.string.player_controls_skip_recap
                    FindroidSegmentType.COMMERCIAL -> R.string.player_controls_skip_commercial
                    FindroidSegmentType.PREVIEW -> R.string.player_controls_skip_preview
                    else -> R.string.player_controls_skip_unknown
                }
        }
    }

    /**
     * Get chapters of current item
     *
     * @return list of [PlayerChapter]
     */
    private fun getChapters(): List<PlayerChapter> {
        return uiState.value.currentChapters
    }

    /**
     * Get the index of the current chapter
     *
     * @return the index of the current chapter
     */
    private fun getCurrentChapterIndex(): Int? {
        val chapters = getChapters()

        for (i in chapters.indices.reversed()) {
            if (chapters[i].startPosition < player.currentPosition) {
                return i
            }
        }

        return null
    }

    /**
     * Get the index of the next chapter
     *
     * @return the index of the next chapter
     */
    private fun getNextChapterIndex(): Int? {
        val chapters = getChapters()
        val currentChapterIndex = getCurrentChapterIndex() ?: return null

        return minOf(chapters.size - 1, currentChapterIndex + 1)
    }

    /**
     * Get the index of the previous chapter. Only use this for seeking as it will return the
     * current chapter when player position is more than 5 seconds past the start of the chapter
     *
     * @return the index of the previous chapter
     */
    private fun getPreviousChapterIndex(): Int? {
        val chapters = getChapters()
        val currentChapterIndex = getCurrentChapterIndex() ?: return null

        // Return current chapter when more than 5 seconds past chapter start
        if (player.currentPosition > chapters[currentChapterIndex].startPosition + 5000L) {
            return currentChapterIndex
        }

        return maxOf(0, currentChapterIndex - 1)
    }

    fun isLastChapter(): Boolean =
        getChapters().let { chapters -> getCurrentChapterIndex() == chapters.size - 1 }

    /**
     * Seek to chapter
     *
     * @param [chapterIndex] the index of the chapter to seek to
     * @return the [PlayerChapter] which has been sought to
     */
    private fun seekToChapter(chapterIndex: Int): PlayerChapter? {
        return getChapters().getOrNull(chapterIndex)?.also { chapter ->
            player.seekTo(chapter.startPosition)
        }
    }

    /**
     * Seek to the next chapter
     *
     * @return the [PlayerChapter] which has been sought to
     */
    fun seekToNextChapter(): PlayerChapter? {
        return getNextChapterIndex()?.let { seekToChapter(it) }
    }

    /**
     * Seek to the previous chapter Will seek to start of current chapter if player position is more
     * than 5 seconds past start of chapter
     *
     * @return the [PlayerChapter] which has been sought to
     */
    fun seekToPreviousChapter(): PlayerChapter? {
        return getPreviousChapterIndex()?.let { seekToChapter(it) }
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        super.onIsPlayingChanged(isPlaying)
        eventsChannel.trySend(PlayerEvents.IsPlayingChanged(isPlaying))
    }
}

sealed interface PlayerEvents {
    data object NavigateBack : PlayerEvents

    data class IsPlayingChanged(val isPlaying: Boolean) : PlayerEvents
}
