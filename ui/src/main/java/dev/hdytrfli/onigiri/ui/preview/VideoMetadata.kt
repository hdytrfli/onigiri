package dev.hdytrfli.onigiri.ui.preview

import dev.hdytrfli.onigiri.models.AudioChannel
import dev.hdytrfli.onigiri.models.AudioCodec
import dev.hdytrfli.onigiri.models.DisplayProfile
import dev.hdytrfli.onigiri.models.Resolution
import dev.hdytrfli.onigiri.models.VideoCodec
import dev.hdytrfli.onigiri.models.VideoMetadata

val dummyVideoMetadata =
    VideoMetadata(
        size = 1000000000,
        videoTracks = emptyList(),
        audioTracks = emptyList(),
        subtitleTracks = emptyList(),
        resolution = listOf(Resolution.HD),
        videoCodecs = listOf(VideoCodec.AV1),
        displayProfiles = listOf(DisplayProfile.HDR10),
        audioChannels = listOf(AudioChannel.CH_5_1),
        audioCodecs = listOf(AudioCodec.OPUS),
        isAtmos = listOf(false),
    )
