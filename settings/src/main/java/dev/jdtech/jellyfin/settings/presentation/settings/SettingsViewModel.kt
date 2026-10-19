package dev.jdtech.jellyfin.settings.presentation.settings

import android.content.Context
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.jdtech.jellyfin.settings.R
import dev.jdtech.jellyfin.settings.domain.AppPreferences
import dev.jdtech.jellyfin.settings.domain.SubtitleFonts
import dev.jdtech.jellyfin.settings.presentation.models.OptionIcon
import dev.jdtech.jellyfin.settings.presentation.models.PreferenceAction
import dev.jdtech.jellyfin.settings.presentation.models.PreferenceAppLanguage
import dev.jdtech.jellyfin.settings.presentation.models.PreferenceCategory
import dev.jdtech.jellyfin.settings.presentation.models.PreferenceFileEdit
import dev.jdtech.jellyfin.settings.presentation.models.PreferenceGroup
import dev.jdtech.jellyfin.settings.presentation.models.PreferenceIntInput
import dev.jdtech.jellyfin.settings.presentation.models.PreferenceLongInput
import dev.jdtech.jellyfin.settings.presentation.models.PreferenceMultiSelect
import dev.jdtech.jellyfin.settings.presentation.models.PreferenceSelect
import dev.jdtech.jellyfin.settings.presentation.models.PreferenceStepper
import dev.jdtech.jellyfin.settings.presentation.models.PreferenceSwitch
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel
@Inject
constructor(
    private val appPreferences: AppPreferences,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    private val _state = MutableStateFlow(SettingsState())
    val state = _state.asStateFlow()

    private val eventsChannel = Channel<SettingsEvent>()
    val events = eventsChannel.receiveAsFlow()

    // Remembered so dependent selects can re-evaluate visibility after they update their flags.
    private var lastIndexes: List<Int> = emptyList()

    private val topLevelPreferences =
        listOf(
            PreferenceGroup(
                preferences =
                    listOf(
                        PreferenceCategory(
                            nameStringResource = R.string.settings_category_language,
                            descriptionStringRes = R.string.settings_category_language_summary,
                            iconDrawableId = R.drawable.ic_languages,
                            onClick = {
                                viewModelScope.launch {
                                    eventsChannel.send(
                                        SettingsEvent.NavigateToSettings(
                                            listOf(it.nameStringResource)
                                        )
                                    )
                                }
                            },
                            nestedPreferenceGroups =
                                listOf(
                                    PreferenceGroup(
                                        preferences =
                                            listOf(
                                                PreferenceAppLanguage(
                                                    nameStringResource = R.string.app_language,
                                                    descriptionStringRes =
                                                        R.string.app_language_summary,
                                                    enabled =
                                                        Build.VERSION.SDK_INT >=
                                                            Build.VERSION_CODES.TIRAMISU,
                                                )
                                            )
                                    ),
                                    PreferenceGroup(
                                        preferences =
                                            listOf(
                                                PreferenceSelect(
                                                    nameStringResource =
                                                        R.string.settings_preferred_audio_language,
                                                    descriptionStringRes =
                                                        R.string
                                                            .settings_preferred_audio_language_summary,
                                                    optionIcon = OptionIcon.Audio,
                                                    backendPreference =
                                                        appPreferences.preferredAudioLanguage,
                                                    options = R.array.languages,
                                                    optionValues = R.array.languages_values,
                                                    optionsIncludeNull = true,
                                                ),
                                                PreferenceSelect(
                                                    nameStringResource =
                                                        R.string
                                                            .settings_preferred_subtitle_language,
                                                    descriptionStringRes =
                                                        R.string
                                                            .settings_preferred_subtitle_language_summary,
                                                    optionIcon = OptionIcon.Subtitle,
                                                    backendPreference =
                                                        appPreferences.preferredSubtitleLanguage,
                                                    options = R.array.languages,
                                                    optionValues = R.array.languages_values,
                                                    optionsIncludeNull = true,
                                                ),
                                                PreferenceSwitch(
                                                    nameStringResource =
                                                        R.string.always_play_subtitles,
                                                    descriptionStringRes =
                                                        R.string.always_play_subtitles_summary,
                                                    backendPreference =
                                                        appPreferences.playerAlwaysPlaySubtitles,
                                                ),
                                            )
                                    ),
                                ),
                        )
                    )
            ),
            PreferenceGroup(
                preferences =
                    listOf(
                        PreferenceCategory(
                            nameStringResource = R.string.settings_category_interface,
                            descriptionStringRes = R.string.settings_category_interface_summary,
                            iconDrawableId = R.drawable.ic_layout_dashboard,
                            onClick = {
                                viewModelScope.launch {
                                    eventsChannel.send(
                                        SettingsEvent.NavigateToSettings(
                                            listOf(it.nameStringResource)
                                        )
                                    )
                                }
                            },
                            nestedPreferenceGroups =
                                listOf(
                                    PreferenceGroup(
                                        nameStringResource = R.string.settings_category_appearance,
                                        preferences =
                                            listOf(
                                                PreferenceSwitch(
                                                    nameStringResource = R.string.hide_status_bar,
                                                    descriptionStringRes =
                                                        R.string.hide_status_bar_summary,
                                                    backendPreference =
                                                        appPreferences.hideStatusBar,
                                                ),
                                                PreferenceSwitch(
                                                    nameStringResource = R.string.offline_mode,
                                                    descriptionStringRes =
                                                        R.string.offline_mode_summary,
                                                    onClick = {
                                                        viewModelScope.launch {
                                                            eventsChannel.send(
                                                                SettingsEvent.RestartActivity
                                                            )
                                                        }
                                                    },
                                                    backendPreference = appPreferences.offlineMode,
                                                ),
                                                PreferenceSwitch(
                                                    nameStringResource = R.string.confirm_exit,
                                                    descriptionStringRes =
                                                        R.string.confirm_exit_summary,
                                                    backendPreference = appPreferences.confirmExit,
                                                ),
                                                PreferenceSwitch(
                                                    nameStringResource = R.string.pref_player_stats,
                                                    descriptionStringRes =
                                                        R.string.pref_player_stats_summary,
                                                    backendPreference = appPreferences.playerStats,
                                                ),
                                            ),
                                    ),
                                    PreferenceGroup(
                                        nameStringResource = R.string.settings_group_layout_grid,
                                        preferences =
                                            listOf(
                                                PreferenceStepper(
                                                    nameStringResource =
                                                        R.string.grid_poster_portrait,
                                                    descriptionStringRes =
                                                        R.string.grid_poster_portrait_summary,
                                                    backendPreference =
                                                        appPreferences.gridPosterColumnsPortrait,
                                                ),
                                                PreferenceStepper(
                                                    nameStringResource =
                                                        R.string.grid_poster_landscape,
                                                    descriptionStringRes =
                                                        R.string.grid_poster_landscape_summary,
                                                    backendPreference =
                                                        appPreferences.gridPosterColumnsLandscape,
                                                ),
                                                PreferenceStepper(
                                                    nameStringResource =
                                                        R.string.grid_thumbnail_portrait,
                                                    descriptionStringRes =
                                                        R.string.grid_thumbnail_portrait_summary,
                                                    backendPreference =
                                                        appPreferences.gridThumbnailColumnsPortrait,
                                                ),
                                                PreferenceStepper(
                                                    nameStringResource =
                                                        R.string.grid_thumbnail_landscape,
                                                    descriptionStringRes =
                                                        R.string.grid_thumbnail_landscape_summary,
                                                    backendPreference =
                                                        appPreferences
                                                            .gridThumbnailColumnsLandscape,
                                                ),
                                                PreferenceSelect(
                                                    nameStringResource = R.string.grid_spacing,
                                                    descriptionStringRes =
                                                        R.string.grid_spacing_summary,
                                                    optionIcon = OptionIcon.Layout,
                                                    backendPreference = appPreferences.gridSpacing,
                                                    options = R.array.grid_spacing_options,
                                                    optionValues = R.array.grid_spacing_values,
                                                ),
                                            ),
                                    ),
                                    PreferenceGroup(
                                        nameStringResource = R.string.backdrop,
                                        preferences =
                                            listOf(
                                                PreferenceSelect(
                                                    nameStringResource =
                                                        R.string.backdrop_portrait_ratio,
                                                    descriptionStringRes =
                                                        R.string.backdrop_portrait_ratio_summary,
                                                    optionIcon = OptionIcon.Image,
                                                    backendPreference =
                                                        appPreferences.backdropPortraitRatio,
                                                    options = R.array.backdrop_ratios,
                                                    optionValues = R.array.backdrop_ratios_values,
                                                ),
                                                PreferenceSelect(
                                                    nameStringResource =
                                                        R.string.backdrop_landscape_ratio,
                                                    descriptionStringRes =
                                                        R.string.backdrop_landscape_ratio_summary,
                                                    optionIcon = OptionIcon.Image,
                                                    backendPreference =
                                                        appPreferences.backdropLandscapeRatio,
                                                    options = R.array.backdrop_ratios,
                                                    optionValues = R.array.backdrop_ratios_values,
                                                ),
                                            ),
                                    ),
                                    PreferenceGroup(
                                        nameStringResource = R.string.title_image,
                                        preferences =
                                            listOf(
                                                PreferenceSelect(
                                                    nameStringResource =
                                                        R.string.title_portrait_width,
                                                    descriptionStringRes =
                                                        R.string.title_portrait_width_summary,
                                                    optionIcon = OptionIcon.Text,
                                                    backendPreference =
                                                        appPreferences.titlePortraitWidth,
                                                    options = R.array.title_width_options,
                                                    optionValues = R.array.title_width_values,
                                                ),
                                                PreferenceSelect(
                                                    nameStringResource =
                                                        R.string.title_landscape_width,
                                                    descriptionStringRes =
                                                        R.string.title_landscape_width_summary,
                                                    optionIcon = OptionIcon.Text,
                                                    backendPreference =
                                                        appPreferences.titleLandscapeWidth,
                                                    options = R.array.title_width_options_low,
                                                    optionValues = R.array.title_width_values_low,
                                                ),
                                                PreferenceSelect(
                                                    nameStringResource =
                                                        R.string.title_player_width,
                                                    descriptionStringRes =
                                                        R.string.title_player_width_summary,
                                                    optionIcon = OptionIcon.Text,
                                                    backendPreference =
                                                        appPreferences.titlePlayerWidth,
                                                    options = R.array.title_width_options_low,
                                                    optionValues = R.array.title_width_values_low,
                                                ),
                                                PreferenceSelect(
                                                    nameStringResource = R.string.title_image_ratio,
                                                    descriptionStringRes =
                                                        R.string.title_image_ratio_summary,
                                                    optionIcon = OptionIcon.Image,
                                                    backendPreference =
                                                        appPreferences.titleImageRatio,
                                                    options = R.array.title_ratio_options,
                                                    optionValues = R.array.title_ratio_values,
                                                ),
                                            ),
                                    ),
                                    PreferenceGroup(
                                        nameStringResource = R.string.home,
                                        preferences =
                                            listOf(
                                                PreferenceSwitch(
                                                    nameStringResource = R.string.home_suggestions,
                                                    descriptionStringRes =
                                                        R.string.home_suggestions_summary,
                                                    backendPreference =
                                                        appPreferences.homeSuggestions,
                                                ),
                                                PreferenceSwitch(
                                                    nameStringResource =
                                                        R.string.home_continue_watching,
                                                    descriptionStringRes =
                                                        R.string.home_continue_watching_summary,
                                                    backendPreference =
                                                        appPreferences.homeContinueWatching,
                                                ),
                                                PreferenceSwitch(
                                                    nameStringResource = R.string.home_next_up,
                                                    descriptionStringRes =
                                                        R.string.home_next_up_summary,
                                                    backendPreference = appPreferences.homeNextUp,
                                                ),
                                                PreferenceSwitch(
                                                    nameStringResource = R.string.home_latest,
                                                    descriptionStringRes =
                                                        R.string.home_latest_summary,
                                                    backendPreference = appPreferences.homeLatest,
                                                ),
                                            ),
                                    ),
                                    PreferenceGroup(
                                        preferences =
                                            listOf(
                                                PreferenceSwitch(
                                                    nameStringResource = R.string.extra_info,
                                                    descriptionStringRes =
                                                        R.string.extra_info_summary,
                                                    backendPreference =
                                                        appPreferences.displayExtraInfo,
                                                )
                                            )
                                    ),
                                ),
                        )
                    )
            ),
            PreferenceGroup(
                preferences =
                    listOf(
                        PreferenceCategory(
                            nameStringResource = R.string.settings_category_player,
                            descriptionStringRes = R.string.settings_category_player_summary,
                            iconDrawableId = R.drawable.ic_play,
                            onClick = {
                                viewModelScope.launch {
                                    eventsChannel.send(
                                        SettingsEvent.NavigateToSettings(
                                            listOf(it.nameStringResource)
                                        )
                                    )
                                }
                            },
                            nestedPreferenceGroups =
                                listOf(
                                    PreferenceGroup(
                                        nameStringResource = R.string.player_options,
                                        preferences =
                                            listOf(
                                                PreferenceCategory(
                                                    nameStringResource = R.string.mpv_options,
                                                    descriptionStringRes =
                                                        R.string.mpv_options_summary,
                                                    onClick = {
                                                        viewModelScope.launch {
                                                            eventsChannel.send(
                                                                SettingsEvent.NavigateToSettings(
                                                                    listOf(
                                                                        R.string
                                                                            .settings_category_player,
                                                                        it.nameStringResource,
                                                                    )
                                                                )
                                                            )
                                                        }
                                                    },
                                                    nestedPreferenceGroups =
                                                        listOf(
                                                            PreferenceGroup(
                                                                preferences =
                                                                    listOf(
                                                                        PreferenceSelect(
                                                                            nameStringResource =
                                                                                R.string
                                                                                    .pref_player_mpv_hwdec,
                                                                            descriptionStringRes =
                                                                                R.string
                                                                                    .pref_player_mpv_hwdec_summary,
                                                                            optionIcon =
                                                                                OptionIcon.Playback,
                                                                            backendPreference =
                                                                                appPreferences
                                                                                    .playerMpvHwdec,
                                                                            options =
                                                                                R.array
                                                                                    .mpv_hwdec_options,
                                                                            optionValues =
                                                                                R.array.mpv_hwdec,
                                                                        ),
                                                                        PreferenceSelect(
                                                                            nameStringResource =
                                                                                R.string
                                                                                    .pref_player_mpv_vo,
                                                                            descriptionStringRes =
                                                                                R.string
                                                                                    .pref_player_mpv_vo_summary,
                                                                            optionIcon =
                                                                                OptionIcon.Video,
                                                                            backendPreference =
                                                                                appPreferences
                                                                                    .playerMpvVo,
                                                                            options =
                                                                                R.array
                                                                                    .mpv_vo_options,
                                                                            optionValues =
                                                                                R.array.mpv_vos,
                                                                        ),
                                                                        PreferenceSelect(
                                                                            nameStringResource =
                                                                                R.string
                                                                                    .pref_player_mpv_ao,
                                                                            descriptionStringRes =
                                                                                R.string
                                                                                    .pref_player_mpv_ao_summary,
                                                                            optionIcon =
                                                                                OptionIcon.Audio,
                                                                            backendPreference =
                                                                                appPreferences
                                                                                    .playerMpvAo,
                                                                            options =
                                                                                R.array
                                                                                    .mpv_ao_options,
                                                                            optionValues =
                                                                                R.array.mpv_aos,
                                                                        ),
                                                                    )
                                                            ),
                                                            PreferenceGroup(
                                                                nameStringResource =
                                                                    R.string.advanced,
                                                                preferences =
                                                                    listOf(
                                                                        PreferenceFileEdit(
                                                                            nameStringResource =
                                                                                R.string
                                                                                    .edit_file_title,
                                                                            descriptionStringRes =
                                                                                R.string
                                                                                    .pref_player_mpv_edit_conf_summary,
                                                                            filePath =
                                                                                "mpv/mpv.conf",
                                                                            onClick = {
                                                                                viewModelScope
                                                                                    .launch {
                                                                                        eventsChannel
                                                                                            .send(
                                                                                                SettingsEvent
                                                                                                    .NavigateToSettingsFileEdit(
                                                                                                        it
                                                                                                            .filePath
                                                                                                    )
                                                                                            )
                                                                                    }
                                                                            },
                                                                        ),
                                                                        PreferenceFileEdit(
                                                                            nameStringResource =
                                                                                R.string
                                                                                    .edit_file_title,
                                                                            descriptionStringRes =
                                                                                R.string
                                                                                    .pref_player_mpv_edit_input_summary,
                                                                            filePath =
                                                                                "mpv/input.conf",
                                                                            onClick = {
                                                                                viewModelScope
                                                                                    .launch {
                                                                                        eventsChannel
                                                                                            .send(
                                                                                                SettingsEvent
                                                                                                    .NavigateToSettingsFileEdit(
                                                                                                        it
                                                                                                            .filePath
                                                                                                    )
                                                                                            )
                                                                                    }
                                                                            },
                                                                        ),
                                                                    ),
                                                            ),
                                                        ),
                                                )
                                            ),
                                    ),
                                    PreferenceGroup(
                                        nameStringResource = R.string.player_time_display,
                                        preferences =
                                            listOf(
                                                PreferenceSelect(
                                                    nameStringResource =
                                                        R.string.pref_player_time_display,
                                                    descriptionStringRes =
                                                        R.string.pref_player_time_display_summary,
                                                    optionIcon = OptionIcon.Clock,
                                                    backendPreference =
                                                        appPreferences.playerTimeDisplay,
                                                    onUpdate = { value ->
                                                        appPreferences.setValue(
                                                            appPreferences.playerTimeShowFinish,
                                                            value == "finish",
                                                        )
                                                        // Re-evaluate visibility now that the flag
                                                        // changed (the screen reloads before this).
                                                        loadPreferences(lastIndexes)
                                                    },
                                                    options = R.array.player_time_display_options,
                                                    optionValues =
                                                        R.array.player_time_display_values,
                                                ),
                                                PreferenceSelect(
                                                    nameStringResource =
                                                        R.string.pref_player_time_end_format,
                                                    descriptionStringRes =
                                                        R.string
                                                            .pref_player_time_end_format_summary,
                                                    dependencies =
                                                        listOf(appPreferences.playerTimeShowFinish),
                                                    hideWhenDisabled = true,
                                                    optionIcon = OptionIcon.Clock,
                                                    backendPreference =
                                                        appPreferences.playerTimeEndFormat,
                                                    options =
                                                        R.array.player_time_end_format_options,
                                                    optionValues =
                                                        R.array.player_time_end_format_values,
                                                ),
                                            ),
                                    ),
                                    PreferenceGroup(
                                        nameStringResource = R.string.gestures,
                                        preferences =
                                            listOf(
                                                PreferenceSwitch(
                                                    nameStringResource =
                                                        R.string.player_gestures_volume,
                                                    descriptionStringRes =
                                                        R.string.player_gestures_volume_summary,
                                                    backendPreference =
                                                        appPreferences.playerGesturesVolume,
                                                ),
                                                PreferenceSwitch(
                                                    nameStringResource =
                                                        R.string.player_gestures_brightness,
                                                    descriptionStringRes =
                                                        R.string.player_gestures_brightness_summary,
                                                    backendPreference =
                                                        appPreferences.playerGesturesBrightness,
                                                ),
                                                PreferenceSwitch(
                                                    nameStringResource =
                                                        R.string.player_gestures_zoom,
                                                    descriptionStringRes =
                                                        R.string.player_gestures_zoom_summary,
                                                    backendPreference =
                                                        appPreferences.playerGesturesZoom,
                                                ),
                                                PreferenceSwitch(
                                                    nameStringResource =
                                                        R.string.player_dynamic_zoom,
                                                    descriptionStringRes =
                                                        R.string.player_dynamic_zoom_summary,
                                                    dependencies =
                                                        listOf(appPreferences.playerGesturesZoom),
                                                    backendPreference =
                                                        appPreferences.playerGesturesDynamicZoom,
                                                ),
                                                PreferenceSwitch(
                                                    nameStringResource =
                                                        R.string.player_gestures_seek,
                                                    descriptionStringRes =
                                                        R.string.player_gestures_seek_summary,
                                                    backendPreference =
                                                        appPreferences.playerGesturesSeek,
                                                ),
                                                PreferenceSwitch(
                                                    nameStringResource =
                                                        R.string.player_gestures_chapter_skip,
                                                    descriptionStringRes =
                                                        R.string
                                                            .player_gestures_chapter_skip_summary,
                                                    backendPreference =
                                                        appPreferences.playerGesturesChapterSkip,
                                                ),
                                                PreferenceSwitch(
                                                    nameStringResource =
                                                        R.string.player_start_maximized,
                                                    descriptionStringRes =
                                                        R.string.player_start_maximized_summary,
                                                    backendPreference =
                                                        appPreferences.playerGesturesStartMaximized,
                                                ),
                                                PreferenceSwitch(
                                                    nameStringResource =
                                                        R.string.player_gestures_double_tap,
                                                    descriptionStringRes =
                                                        R.string.player_gestures_double_tap_summary,
                                                    backendPreference =
                                                        appPreferences.playerGesturesDoubleTap,
                                                ),
                                            ),
                                    ),
                                    PreferenceGroup(
                                        nameStringResource = R.string.player_overlay,
                                        preferences =
                                            listOf(
                                                PreferenceSelect(
                                                    nameStringResource =
                                                        R.string.player_controls_timeout,
                                                    descriptionStringRes =
                                                        R.string.player_controls_timeout_summary,
                                                    optionIcon = OptionIcon.Clock,
                                                    backendPreference =
                                                        appPreferences.playerControlsTimeout,
                                                    options =
                                                        R.array.player_controls_timeout_options,
                                                    optionValues =
                                                        R.array.player_controls_timeout_values,
                                                )
                                            ),
                                    ),
                                    PreferenceGroup(
                                        nameStringResource = R.string.seeking,
                                        preferences =
                                            listOf(
                                                PreferenceLongInput(
                                                    nameStringResource =
                                                        R.string.seek_back_increment,
                                                    descriptionStringRes =
                                                        R.string.seek_back_increment_summary,
                                                    backendPreference =
                                                        appPreferences.playerSeekBackInc,
                                                    suffixRes = R.string.ms,
                                                ),
                                                PreferenceLongInput(
                                                    nameStringResource =
                                                        R.string.seek_forward_increment,
                                                    descriptionStringRes =
                                                        R.string.seek_forward_increment_summary,
                                                    backendPreference =
                                                        appPreferences.playerSeekForwardInc,
                                                    suffixRes = R.string.ms,
                                                ),
                                                PreferenceSwitch(
                                                    nameStringResource =
                                                        R.string.pref_player_chapter_markers,
                                                    descriptionStringRes =
                                                        R.string
                                                            .pref_player_chapter_markers_summary,
                                                    backendPreference =
                                                        appPreferences.playerChapterMarkers,
                                                ),
                                            ),
                                    ),
                                    PreferenceGroup(
                                        nameStringResource = R.string.media_segments,
                                        preferences =
                                            listOf(
                                                PreferenceSwitch(
                                                    nameStringResource =
                                                        R.string
                                                            .pref_player_media_segments_skip_button,
                                                    descriptionStringRes =
                                                        R.string
                                                            .pref_player_media_segments_skip_button_summary,
                                                    backendPreference =
                                                        appPreferences
                                                            .playerMediaSegmentsSkipButton,
                                                ),
                                                PreferenceMultiSelect(
                                                    nameStringResource =
                                                        R.string
                                                            .pref_player_media_segments_skip_button_type,
                                                    descriptionStringRes =
                                                        R.string
                                                            .pref_player_media_segments_skip_button_type_summary,
                                                    dependencies =
                                                        listOf(
                                                            appPreferences
                                                                .playerMediaSegmentsSkipButton
                                                        ),
                                                    optionIcon = OptionIcon.Segment,
                                                    backendPreference =
                                                        appPreferences
                                                            .playerMediaSegmentsSkipButtonType,
                                                    options = R.array.media_segments_type,
                                                    optionValues =
                                                        R.array.media_segments_type_values,
                                                ),
                                                PreferenceLongInput(
                                                    nameStringResource =
                                                        R.string
                                                            .pref_player_media_segments_skip_button_duration,
                                                    descriptionStringRes =
                                                        R.string
                                                            .pref_player_media_segments_skip_button_duration_summary,
                                                    dependencies =
                                                        listOf(
                                                            appPreferences
                                                                .playerMediaSegmentsSkipButton
                                                        ),
                                                    backendPreference =
                                                        appPreferences
                                                            .playerMediaSegmentsSkipButtonDuration,
                                                    suffixRes = R.string.seconds,
                                                ),
                                                PreferenceSwitch(
                                                    nameStringResource =
                                                        R.string
                                                            .pref_player_media_segments_auto_skip,
                                                    descriptionStringRes =
                                                        R.string
                                                            .pref_player_media_segments_auto_skip_summary,
                                                    backendPreference =
                                                        appPreferences.playerMediaSegmentsAutoSkip,
                                                ),
                                                PreferenceSelect(
                                                    nameStringResource =
                                                        R.string
                                                            .pref_player_media_segments_auto_skip_mode,
                                                    descriptionStringRes =
                                                        R.string
                                                            .pref_player_media_segments_auto_skip_mode_summary,
                                                    dependencies =
                                                        listOf(
                                                            appPreferences
                                                                .playerMediaSegmentsAutoSkip
                                                        ),
                                                    optionIcon = OptionIcon.Segment,
                                                    backendPreference =
                                                        appPreferences
                                                            .playerMediaSegmentsAutoSkipMode,
                                                    options = R.array.media_segments_auto_skip,
                                                    optionValues =
                                                        R.array.media_segments_auto_skip_values,
                                                ),
                                                PreferenceMultiSelect(
                                                    nameStringResource =
                                                        R.string
                                                            .pref_player_media_segments_auto_skip_type,
                                                    descriptionStringRes =
                                                        R.string
                                                            .pref_player_media_segments_auto_skip_type_summary,
                                                    dependencies =
                                                        listOf(
                                                            appPreferences
                                                                .playerMediaSegmentsAutoSkip
                                                        ),
                                                    optionIcon = OptionIcon.Segment,
                                                    backendPreference =
                                                        appPreferences
                                                            .playerMediaSegmentsAutoSkipType,
                                                    options = R.array.media_segments_type,
                                                    optionValues =
                                                        R.array.media_segments_type_values,
                                                ),
                                                PreferenceLongInput(
                                                    nameStringResource =
                                                        R.string
                                                            .pref_player_media_segments_next_episode_threshold,
                                                    descriptionStringRes =
                                                        R.string
                                                            .pref_player_media_segments_next_episode_threshold_summary,
                                                    backendPreference =
                                                        appPreferences
                                                            .playerMediaSegmentsNextEpisodeThreshold,
                                                    suffixRes = R.string.ms,
                                                ),
                                            ),
                                    ),
                                    PreferenceGroup(
                                        nameStringResource = R.string.trickplay,
                                        preferences =
                                            listOf(
                                                PreferenceSwitch(
                                                    nameStringResource =
                                                        R.string.pref_player_trickplay,
                                                    descriptionStringRes =
                                                        R.string.pref_player_trickplay_summary,
                                                    backendPreference =
                                                        appPreferences.playerTrickplay,
                                                ),
                                                PreferenceSwitch(
                                                    nameStringResource =
                                                        R.string
                                                            .pref_player_gestures_seek_trickplay,
                                                    descriptionStringRes =
                                                        R.string
                                                            .pref_player_gestures_seek_trickplay_summary,
                                                    dependencies =
                                                        listOf(appPreferences.playerTrickplay),
                                                    backendPreference =
                                                        appPreferences.playerGesturesSeekTrickplay,
                                                ),
                                            ),
                                    ),
                                    PreferenceGroup(
                                        nameStringResource = R.string.picture_in_picture,
                                        preferences =
                                            listOf(
                                                PreferenceSwitch(
                                                    nameStringResource =
                                                        R.string.picture_in_picture_gesture,
                                                    descriptionStringRes =
                                                        R.string.picture_in_picture_gesture_summary,
                                                    backendPreference =
                                                        appPreferences.playerPipGesture,
                                                )
                                            ),
                                    ),
                                ),
                        ),
                        PreferenceCategory(
                            nameStringResource = R.string.mpv_subtitles,
                            descriptionStringRes = R.string.mpv_subtitles_summary,
                            iconDrawableId = R.drawable.ic_closed_caption,
                            onClick = {
                                viewModelScope.launch {
                                    eventsChannel.send(
                                        SettingsEvent.NavigateToSettings(
                                            listOf(it.nameStringResource)
                                        )
                                    )
                                }
                            },
                            nestedPreferenceGroups =
                                listOf(
                                    PreferenceGroup(
                                        nameStringResource = R.string.mpv_sub_group_text,
                                        preferences =
                                            listOf(
                                                PreferenceSelect(
                                                    nameStringResource =
                                                        R.string.pref_player_mpv_sub_font_size,
                                                    descriptionStringRes =
                                                        R.string
                                                            .pref_player_mpv_sub_font_size_summary,
                                                    optionIcon = OptionIcon.Subtitle,
                                                    backendPreference =
                                                        appPreferences.playerMpvSubFontSize,
                                                    options = R.array.mpv_sub_font_size,
                                                    optionValues = R.array.mpv_sub_font_size,
                                                ),
                                                PreferenceAction(
                                                    nameStringResource =
                                                        R.string.pref_player_mpv_sub_font,
                                                    descriptionStringRes =
                                                        R.string.pref_player_mpv_sub_font_summary,
                                                    backendPreference =
                                                        appPreferences.playerMpvSubFont,
                                                    formatValue = { subtitleFontDisplayName(it) },
                                                    onClick = {
                                                        viewModelScope.launch {
                                                            eventsChannel.send(
                                                                SettingsEvent.NavigateToSubtitleFont
                                                            )
                                                        }
                                                    },
                                                ),
                                                PreferenceSelect(
                                                    nameStringResource =
                                                        R.string.pref_player_mpv_sub_font_weight,
                                                    descriptionStringRes =
                                                        R.string
                                                            .pref_player_mpv_sub_font_weight_summary,
                                                    optionIcon = OptionIcon.Subtitle,
                                                    backendPreference =
                                                        appPreferences.playerMpvSubFontWeight,
                                                    options = R.array.mpv_sub_font_weights,
                                                    optionValues =
                                                        R.array.mpv_sub_font_weight_values,
                                                ),
                                                PreferenceSelect(
                                                    nameStringResource =
                                                        R.string.pref_player_mpv_sub_color,
                                                    descriptionStringRes =
                                                        R.string.pref_player_mpv_sub_color_summary,
                                                    optionIcon = OptionIcon.Subtitle,
                                                    backendPreference =
                                                        appPreferences.playerMpvSubColor,
                                                    options = R.array.mpv_sub_colors,
                                                    optionValues = R.array.mpv_sub_colors_values,
                                                ),
                                                PreferenceSelect(
                                                    nameStringResource =
                                                        R.string.pref_player_mpv_sub_ass_override,
                                                    descriptionStringRes =
                                                        R.string
                                                            .pref_player_mpv_sub_ass_override_summary,
                                                    optionIcon = OptionIcon.Subtitle,
                                                    backendPreference =
                                                        appPreferences.playerMpvSubAssOverride,
                                                    options = R.array.mpv_sub_ass_override,
                                                    optionValues =
                                                        R.array.mpv_sub_ass_override_values,
                                                ),
                                            ),
                                    ),
                                    PreferenceGroup(
                                        nameStringResource = R.string.mpv_sub_group_outline,
                                        preferences =
                                            listOf(
                                                PreferenceSwitch(
                                                    nameStringResource =
                                                        R.string.pref_player_mpv_sub_border_enabled,
                                                    descriptionStringRes =
                                                        R.string
                                                            .pref_player_mpv_sub_border_enabled_summary,
                                                    backendPreference =
                                                        appPreferences.playerMpvSubBorderEnabled,
                                                ),
                                                PreferenceSelect(
                                                    nameStringResource =
                                                        R.string.pref_player_mpv_sub_border_style,
                                                    descriptionStringRes =
                                                        R.string
                                                            .pref_player_mpv_sub_border_style_summary,
                                                    dependencies =
                                                        listOf(
                                                            appPreferences.playerMpvSubBorderEnabled
                                                        ),
                                                    hideWhenDisabled = true,
                                                    optionIcon = OptionIcon.Subtitle,
                                                    backendPreference =
                                                        appPreferences.playerMpvSubBorderStyle,
                                                    options = R.array.mpv_sub_border_style,
                                                    optionValues =
                                                        R.array.mpv_sub_border_style_values,
                                                ),
                                                PreferenceSelect(
                                                    nameStringResource =
                                                        R.string.pref_player_mpv_sub_border_size,
                                                    descriptionStringRes =
                                                        R.string
                                                            .pref_player_mpv_sub_border_size_summary,
                                                    dependencies =
                                                        listOf(
                                                            appPreferences.playerMpvSubBorderEnabled
                                                        ),
                                                    hideWhenDisabled = true,
                                                    optionIcon = OptionIcon.Subtitle,
                                                    backendPreference =
                                                        appPreferences.playerMpvSubBorderSize,
                                                    options = R.array.mpv_sub_border_size,
                                                    optionValues = R.array.mpv_sub_border_size,
                                                ),
                                                PreferenceSelect(
                                                    nameStringResource =
                                                        R.string.pref_player_mpv_sub_border_color,
                                                    descriptionStringRes =
                                                        R.string
                                                            .pref_player_mpv_sub_border_color_summary,
                                                    dependencies =
                                                        listOf(
                                                            appPreferences.playerMpvSubBorderEnabled
                                                        ),
                                                    hideWhenDisabled = true,
                                                    optionIcon = OptionIcon.Subtitle,
                                                    backendPreference =
                                                        appPreferences.playerMpvSubBorderColor,
                                                    options = R.array.mpv_sub_colors,
                                                    optionValues = R.array.mpv_sub_colors_values,
                                                ),
                                                PreferenceSelect(
                                                    nameStringResource =
                                                        R.string.pref_player_mpv_sub_border_opacity,
                                                    descriptionStringRes =
                                                        R.string
                                                            .pref_player_mpv_sub_border_opacity_summary,
                                                    dependencies =
                                                        listOf(
                                                            appPreferences.playerMpvSubBorderEnabled
                                                        ),
                                                    hideWhenDisabled = true,
                                                    optionIcon = OptionIcon.Subtitle,
                                                    backendPreference =
                                                        appPreferences.playerMpvSubBorderOpacity,
                                                    options = R.array.mpv_sub_border_opacity,
                                                    optionValues =
                                                        R.array.mpv_sub_border_opacity_values,
                                                ),
                                            ),
                                    ),
                                    PreferenceGroup(
                                        nameStringResource = R.string.mpv_sub_group_position,
                                        preferences =
                                            listOf(
                                                PreferenceSelect(
                                                    nameStringResource =
                                                        R.string.pref_player_mpv_sub_pos,
                                                    descriptionStringRes =
                                                        R.string.pref_player_mpv_sub_pos_summary,
                                                    optionIcon = OptionIcon.Subtitle,
                                                    backendPreference =
                                                        appPreferences.playerMpvSubPosition,
                                                    options = R.array.mpv_sub_pos,
                                                    optionValues = R.array.mpv_sub_pos,
                                                ),
                                                PreferenceSwitch(
                                                    nameStringResource =
                                                        R.string.pref_player_mpv_sub_use_margins,
                                                    descriptionStringRes =
                                                        R.string
                                                            .pref_player_mpv_sub_use_margins_summary,
                                                    backendPreference =
                                                        appPreferences.playerMpvSubUseMargins,
                                                ),
                                            ),
                                    ),
                                ),
                        ),
                    )
            ),
            PreferenceGroup(
                preferences =
                    listOf(
                        PreferenceCategory(
                            nameStringResource = R.string.users,
                            descriptionStringRes = R.string.users_summary,
                            iconDrawableId = R.drawable.ic_user,
                            onClick = {
                                viewModelScope.launch {
                                    eventsChannel.send(SettingsEvent.NavigateToUsers)
                                }
                            },
                        )
                    )
            ),
            PreferenceGroup(
                preferences =
                    listOf(
                        PreferenceCategory(
                            nameStringResource = R.string.settings_category_servers,
                            descriptionStringRes = R.string.settings_category_servers_summary,
                            iconDrawableId = R.drawable.ic_server,
                            onClick = {
                                viewModelScope.launch {
                                    eventsChannel.send(SettingsEvent.NavigateToServers)
                                }
                            },
                        )
                    )
            ),
            PreferenceGroup(
                preferences =
                    listOf(
                        PreferenceCategory(
                            nameStringResource = R.string.title_download,
                            descriptionStringRes = R.string.title_download_summary,
                            iconDrawableId = R.drawable.ic_download,
                            onClick = {
                                viewModelScope.launch {
                                    eventsChannel.send(
                                        SettingsEvent.NavigateToSettings(
                                            listOf(it.nameStringResource)
                                        )
                                    )
                                }
                            },
                            nestedPreferenceGroups =
                                listOf(
                                    PreferenceGroup(
                                        preferences =
                                            listOf(
                                                PreferenceSwitch(
                                                    nameStringResource =
                                                        R.string.download_mobile_data,
                                                    descriptionStringRes =
                                                        R.string.download_mobile_data_summary,
                                                    backendPreference =
                                                        appPreferences.downloadOverMobileData,
                                                ),
                                                PreferenceSwitch(
                                                    nameStringResource = R.string.download_roaming,
                                                    descriptionStringRes =
                                                        R.string.download_roaming_summary,
                                                    dependencies =
                                                        listOf(
                                                            appPreferences.downloadOverMobileData
                                                        ),
                                                    backendPreference =
                                                        appPreferences.downloadWhenRoaming,
                                                ),
                                            )
                                    )
                                ),
                        )
                    )
            ),
            PreferenceGroup(
                preferences =
                    listOf(
                        PreferenceCategory(
                            nameStringResource = R.string.settings_category_network,
                            descriptionStringRes = R.string.settings_category_network_summary,
                            iconDrawableId = R.drawable.ic_network,
                            onClick = {
                                viewModelScope.launch {
                                    eventsChannel.send(
                                        SettingsEvent.NavigateToSettings(
                                            listOf(it.nameStringResource)
                                        )
                                    )
                                }
                            },
                            nestedPreferenceGroups =
                                listOf(
                                    PreferenceGroup(
                                        preferences =
                                            listOf(
                                                PreferenceLongInput(
                                                    nameStringResource =
                                                        R.string.settings_request_timeout,
                                                    descriptionStringRes =
                                                        R.string.settings_request_timeout_summary,
                                                    backendPreference =
                                                        appPreferences.requestTimeout,
                                                    suffixRes = R.string.ms,
                                                ),
                                                PreferenceLongInput(
                                                    nameStringResource =
                                                        R.string.settings_connect_timeout,
                                                    descriptionStringRes =
                                                        R.string.settings_connect_timeout_summary,
                                                    backendPreference =
                                                        appPreferences.connectTimeout,
                                                    suffixRes = R.string.ms,
                                                ),
                                                PreferenceLongInput(
                                                    nameStringResource =
                                                        R.string.settings_socket_timeout,
                                                    descriptionStringRes =
                                                        R.string.settings_socket_timeout_summary,
                                                    backendPreference =
                                                        appPreferences.socketTimeout,
                                                    suffixRes = R.string.ms,
                                                ),
                                            )
                                    )
                                ),
                        )
                    )
            ),
            PreferenceGroup(
                preferences =
                    listOf(
                        PreferenceCategory(
                            nameStringResource = R.string.settings_category_cache,
                            descriptionStringRes = R.string.settings_category_cache_summary,
                            iconDrawableId = R.drawable.ic_hard_drive,
                            onClick = {
                                viewModelScope.launch {
                                    eventsChannel.send(
                                        SettingsEvent.NavigateToSettings(
                                            listOf(it.nameStringResource)
                                        )
                                    )
                                }
                            },
                            nestedPreferenceGroups =
                                listOf(
                                    PreferenceGroup(
                                        preferences =
                                            listOf(
                                                PreferenceSwitch(
                                                    nameStringResource =
                                                        R.string.settings_use_cache_title,
                                                    descriptionStringRes =
                                                        R.string.settings_use_cache_summary,
                                                    backendPreference = appPreferences.imageCache,
                                                ),
                                                PreferenceIntInput(
                                                    nameStringResource =
                                                        R.string.settings_cache_size,
                                                    descriptionStringRes =
                                                        R.string.settings_cache_size_message,
                                                    dependencies =
                                                        listOf(appPreferences.imageCache),
                                                    backendPreference =
                                                        appPreferences.imageCacheSize,
                                                    suffixRes = R.string.mb,
                                                ),
                                            )
                                    )
                                ),
                        )
                    )
            ),
            PreferenceGroup(
                preferences =
                    listOf(
                        PreferenceCategory(
                            nameStringResource = R.string.about,
                            descriptionStringRes = R.string.about_summary,
                            iconDrawableId = R.drawable.ic_info,
                            onClick = {
                                viewModelScope.launch {
                                    eventsChannel.send(SettingsEvent.NavigateToAbout)
                                }
                            },
                        )
                    )
            ),
        )

    /**
     * The value shown in the subtitle-font row: the display name of the selected family, falling
     * back to the raw family name for fonts whose display name is the family itself.
     */
    private fun subtitleFontDisplayName(family: String?): String? =
        when (family) {
            null -> null
            SubtitleFonts.DEFAULT_FAMILY -> context.getString(R.string.subtitle_font_default)
            else -> family
        }

    fun loadPreferences(indexes: List<Int> = emptyList()) {
        lastIndexes = indexes
        viewModelScope.launch {
            val isMpvBackend = true

            // Hide the whole mpv section (and its subtitle page) unless mpv is
            // the active player backend.
            val isHiddenMpvDestination =
                indexes.contains(R.string.mpv_options) || indexes.contains(R.string.mpv_subtitles)
            if (!isMpvBackend && isHiddenMpvDestination) {
                _state.emit(_state.value.copy(preferenceGroups = emptyList()))
                return@launch
            }

            val isRootLevel = indexes.isEmpty() || indexes.contains(R.string.title_settings)

            var preferences = topLevelPreferences

            // Show preferences based on the name of the parent
            for (index in indexes) {
                // If index is root (Settings) don't search for category
                if (index == R.string.title_settings) {
                    break
                }
                val preference =
                    preferences
                        .flatMap { it.preferences }
                        .filterIsInstance<PreferenceCategory>()
                        .find { it.nameStringResource == index }
                if (preference != null) {
                    preferences = preference.nestedPreferenceGroups
                }
            }

            // Update all (visible) preferences with there current values
            preferences =
                preferences
                    .map { preferenceGroup ->
                        preferenceGroup.copy(
                            preferences =
                                preferenceGroup.preferences
                                    .filter {
                                        if (!isMpvBackend && it is PreferenceCategory) {
                                            it.nameStringResource != R.string.mpv_options &&
                                                it.nameStringResource != R.string.mpv_subtitles
                                        } else {
                                            true
                                        }
                                    }
                                    .filter { preference ->
                                        !preference.hideWhenDisabled ||
                                            preference.dependencies.all {
                                                appPreferences.getValue(it)
                                            }
                                    }
                                    .map { preference ->
                                        when (preference) {
                                            is PreferenceSwitch -> {
                                                preference.copy(
                                                    enabled =
                                                        preference.enabled &&
                                                            preference.dependencies.all {
                                                                appPreferences.getValue(it)
                                                            },
                                                    value =
                                                        appPreferences.getValue(
                                                            preference.backendPreference
                                                        ),
                                                )
                                            }
                                            is PreferenceSelect -> {
                                                preference.copy(
                                                    enabled =
                                                        preference.enabled &&
                                                            preference.dependencies.all {
                                                                appPreferences.getValue(it)
                                                            },
                                                    value =
                                                        appPreferences.getValue(
                                                            preference.backendPreference
                                                        ),
                                                )
                                            }
                                            is PreferenceMultiSelect -> {
                                                preference.copy(
                                                    enabled =
                                                        preference.enabled &&
                                                            preference.dependencies.all {
                                                                appPreferences.getValue(it)
                                                            },
                                                    value =
                                                        appPreferences.getValue(
                                                            preference.backendPreference
                                                        ),
                                                )
                                            }
                                            is PreferenceStepper -> {
                                                preference.copy(
                                                    enabled =
                                                        preference.enabled &&
                                                            preference.dependencies.all {
                                                                appPreferences.getValue(it)
                                                            },
                                                    value =
                                                        appPreferences.getValue(
                                                            preference.backendPreference
                                                        ),
                                                )
                                            }
                                            is PreferenceIntInput -> {
                                                preference.copy(
                                                    enabled =
                                                        preference.enabled &&
                                                            preference.dependencies.all {
                                                                appPreferences.getValue(it)
                                                            },
                                                    value =
                                                        appPreferences.getValue(
                                                            preference.backendPreference
                                                        ),
                                                )
                                            }
                                            is PreferenceLongInput -> {
                                                preference.copy(
                                                    enabled =
                                                        preference.enabled &&
                                                            preference.dependencies.all {
                                                                appPreferences.getValue(it)
                                                            },
                                                    value =
                                                        appPreferences.getValue(
                                                            preference.backendPreference
                                                        ),
                                                )
                                            }
                                            is PreferenceAction -> {
                                                preference.copy(
                                                    enabled =
                                                        preference.enabled &&
                                                            preference.dependencies.all {
                                                                appPreferences.getValue(it)
                                                            },
                                                    value =
                                                        preference.formatValue(
                                                            appPreferences.getValue(
                                                                preference.backendPreference
                                                            )
                                                        ),
                                                )
                                            }
                                            else -> preference
                                        }
                                    }
                        )
                    }
                    .filter { it.preferences.isNotEmpty() }

            if (isRootLevel) {
                preferences = groupTopLevel(preferences)
            }

            _state.emit(_state.value.copy(preferenceGroups = preferences))
        }
    }

    /**
     * Groups the flat root-level entries into context sections (General, Playback, Server, Storage,
     * Network) so the main settings page reads as clear groups instead of a list of separate cards.
     */
    private fun groupTopLevel(groups: List<PreferenceGroup>): List<PreferenceGroup> {
        fun headerFor(group: PreferenceGroup): Int? {
            val preference = group.preferences.firstOrNull()
            return when {
                preference is PreferenceSwitch -> R.string.settings_group_general
                preference !is PreferenceCategory -> null
                preference.nameStringResource == R.string.settings_category_language ||
                    preference.nameStringResource == R.string.settings_category_interface ->
                    R.string.settings_group_general
                preference.nameStringResource == R.string.settings_category_player ->
                    R.string.settings_group_playback
                preference.nameStringResource == R.string.users ||
                    preference.nameStringResource == R.string.settings_category_servers ->
                    R.string.settings_group_server
                preference.nameStringResource == R.string.title_download ||
                    preference.nameStringResource == R.string.settings_category_cache ->
                    R.string.settings_group_storage
                preference.nameStringResource == R.string.settings_category_network ->
                    R.string.settings_group_network
                else -> null
            }
        }

        val result = mutableListOf<PreferenceGroup>()
        val indexByHeader = mutableMapOf<Int, Int>()
        for (group in groups) {
            val header = headerFor(group)
            if (header == null) {
                result.add(group)
                continue
            }
            val existingIndex = indexByHeader[header]
            if (existingIndex == null) {
                indexByHeader[header] = result.size
                result.add(
                    PreferenceGroup(nameStringResource = header, preferences = group.preferences)
                )
            } else {
                result[existingIndex] =
                    result[existingIndex].copy(
                        preferences = result[existingIndex].preferences + group.preferences
                    )
            }
        }
        return result
    }

    fun onAction(action: SettingsAction) {
        when (action) {
            is SettingsAction.OnUpdate -> {
                when (action.preference) {
                    is PreferenceSwitch ->
                        appPreferences.setValue(
                            action.preference.backendPreference,
                            action.preference.value,
                        )
                    is PreferenceSelect ->
                        appPreferences.setValue(
                            action.preference.backendPreference,
                            action.preference.value,
                        )
                    is PreferenceMultiSelect ->
                        appPreferences.setValue(
                            action.preference.backendPreference,
                            action.preference.value,
                        )
                    is PreferenceStepper ->
                        appPreferences.setValue(
                            action.preference.backendPreference,
                            action.preference.value,
                        )
                    is PreferenceIntInput ->
                        appPreferences.setValue(
                            action.preference.backendPreference,
                            action.preference.value,
                        )
                    is PreferenceLongInput ->
                        appPreferences.setValue(
                            action.preference.backendPreference,
                            action.preference.value,
                        )
                }
            }
            else -> Unit
        }
    }
}
