package dev.hdytrfli.onigiri.settings.presentation.settings

import android.content.Context
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.hdytrfli.onigiri.settings.R
import dev.hdytrfli.onigiri.settings.domain.AppPreferences
import dev.hdytrfli.onigiri.settings.domain.SubtitleFonts
import dev.hdytrfli.onigiri.settings.presentation.models.OptionIcon
import dev.hdytrfli.onigiri.settings.presentation.models.PreferenceAction
import dev.hdytrfli.onigiri.settings.presentation.models.PreferenceAppLanguage
import dev.hdytrfli.onigiri.settings.presentation.models.PreferenceCategory
import dev.hdytrfli.onigiri.settings.presentation.models.PreferenceFileEdit
import dev.hdytrfli.onigiri.settings.presentation.models.PreferenceGroup
import dev.hdytrfli.onigiri.settings.presentation.models.PreferenceIntInput
import dev.hdytrfli.onigiri.settings.presentation.models.PreferenceLongInput
import dev.hdytrfli.onigiri.settings.presentation.models.PreferenceMultiSelect
import dev.hdytrfli.onigiri.settings.presentation.models.PreferenceSelect
import dev.hdytrfli.onigiri.settings.presentation.models.PreferenceSlider
import dev.hdytrfli.onigiri.settings.presentation.models.PreferenceSliders
import dev.hdytrfli.onigiri.settings.presentation.models.PreferenceStepper
import dev.hdytrfli.onigiri.settings.presentation.models.PreferenceSwitch
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

    private fun topLevelPreferences(): List<PreferenceGroup> =
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
                                        nameStringResource = R.string.settings_group_languages,
                                        preferences =
                                            listOf(
                                                PreferenceAppLanguage(
                                                    nameStringResource = R.string.app_language,
                                                    descriptionStringRes =
                                                        R.string.app_language_summary,
                                                    enabled =
                                                        Build.VERSION.SDK_INT >=
                                                            Build.VERSION_CODES.TIRAMISU,
                                                ),
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
                                            ),
                                    )
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
                                        nameStringResource = R.string.general,
                                        preferences =
                                            listOf(
                                                PreferenceSelect(
                                                    nameStringResource = R.string.tablet_mode,
                                                    descriptionStringRes =
                                                        R.string.tablet_mode_summary,
                                                    optionIcon = OptionIcon.TabletMode,
                                                    backendPreference = appPreferences.tabletUiMode,
                                                    options = R.array.tablet_ui_modes_options,
                                                    optionValues = R.array.tablet_ui_modes_values,
                                                ),
                                                PreferenceSwitch(
                                                    nameStringResource = R.string.predictive_back,
                                                    descriptionStringRes =
                                                        R.string.predictive_back_summary,
                                                    backendPreference =
                                                        appPreferences.predictiveBack,
                                                ),
                                                PreferenceSwitch(
                                                    nameStringResource = R.string.blur_hash,
                                                    descriptionStringRes =
                                                        R.string.blur_hash_summary,
                                                    backendPreference = appPreferences.blurHash,
                                                ),
                                                PreferenceSwitch(
                                                    nameStringResource = R.string.extra_info,
                                                    descriptionStringRes =
                                                        R.string.extra_info_summary,
                                                    backendPreference =
                                                        appPreferences.displayExtraInfo,
                                                ),
                                            ),
                                    ),
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
                                                PreferenceSliders(
                                                    nameStringResource = R.string.grid_posters,
                                                    descriptionStringRes =
                                                        R.string.grid_posters_summary,
                                                    sliders =
                                                        listOf(
                                                            PreferenceSlider(
                                                                nameStringResource =
                                                                    R.string.grid_poster_portrait,
                                                                options = R.array.grid_columns,
                                                                optionValues = R.array.grid_columns,
                                                                backendPreference =
                                                                    appPreferences
                                                                        .gridPosterColumnsPortrait,
                                                            ),
                                                            PreferenceSlider(
                                                                nameStringResource =
                                                                    R.string.grid_poster_landscape,
                                                                options = R.array.grid_columns,
                                                                optionValues = R.array.grid_columns,
                                                                backendPreference =
                                                                    appPreferences
                                                                        .gridPosterColumnsLandscape,
                                                            ),
                                                        ),
                                                ),
                                                PreferenceSliders(
                                                    nameStringResource = R.string.grid_thumbnails,
                                                    descriptionStringRes =
                                                        R.string.grid_thumbnails_summary,
                                                    sliders =
                                                        listOf(
                                                            PreferenceSlider(
                                                                nameStringResource =
                                                                    R.string
                                                                        .grid_thumbnail_portrait,
                                                                options = R.array.grid_columns,
                                                                optionValues = R.array.grid_columns,
                                                                backendPreference =
                                                                    appPreferences
                                                                        .gridThumbnailColumnsPortrait,
                                                            ),
                                                            PreferenceSlider(
                                                                nameStringResource =
                                                                    R.string
                                                                        .grid_thumbnail_landscape,
                                                                options = R.array.grid_columns,
                                                                optionValues = R.array.grid_columns,
                                                                backendPreference =
                                                                    appPreferences
                                                                        .gridThumbnailColumnsLandscape,
                                                            ),
                                                        ),
                                                ),
                                                PreferenceSliders(
                                                    nameStringResource = R.string.grid_suggestions,
                                                    descriptionStringRes =
                                                        R.string.grid_suggestions_summary,
                                                    sliders =
                                                        listOf(
                                                            PreferenceSlider(
                                                                nameStringResource =
                                                                    R.string
                                                                        .grid_suggestion_portrait,
                                                                options = R.array.grid_columns,
                                                                optionValues = R.array.grid_columns,
                                                                backendPreference =
                                                                    appPreferences
                                                                        .gridSuggestionColumnsPortrait,
                                                            ),
                                                            PreferenceSlider(
                                                                nameStringResource =
                                                                    R.string
                                                                        .grid_suggestion_landscape,
                                                                options = R.array.grid_columns,
                                                                optionValues = R.array.grid_columns,
                                                                backendPreference =
                                                                    appPreferences
                                                                        .gridSuggestionColumnsLandscape,
                                                            ),
                                                        ),
                                                ),
                                                PreferenceSliders(
                                                    nameStringResource = R.string.grid_profiles,
                                                    descriptionStringRes =
                                                        R.string.grid_profiles_summary,
                                                    sliders =
                                                        listOf(
                                                            PreferenceSlider(
                                                                nameStringResource =
                                                                    R.string.grid_profile_portrait,
                                                                options = R.array.grid_columns,
                                                                optionValues = R.array.grid_columns,
                                                                backendPreference =
                                                                    appPreferences
                                                                        .gridProfileColumnsPortrait,
                                                            ),
                                                            PreferenceSlider(
                                                                nameStringResource =
                                                                    R.string.grid_profile_landscape,
                                                                options = R.array.grid_columns,
                                                                optionValues = R.array.grid_columns,
                                                                backendPreference =
                                                                    appPreferences
                                                                        .gridProfileColumnsLandscape,
                                                            ),
                                                        ),
                                                ),
                                                PreferenceSelect(
                                                    nameStringResource = R.string.grid_spacing,
                                                    descriptionStringRes =
                                                        R.string.grid_spacing_summary,
                                                    optionIcon = OptionIcon.Grid,
                                                    backendPreference = appPreferences.gridSpacing,
                                                    options = R.array.grid_spacing_options,
                                                    optionValues = R.array.grid_spacing_values,
                                                ),
                                            ),
                                    ),
                                    PreferenceGroup(
                                        nameStringResource = R.string.settings_group_artwork,
                                        preferences =
                                            listOf(
                                                PreferenceSelect(
                                                    nameStringResource =
                                                        R.string.backdrop_portrait_ratio,
                                                    descriptionStringRes =
                                                        R.string.backdrop_portrait_ratio_summary,
                                                    optionIcon = OptionIcon.Portrait,
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
                                                    optionIcon = OptionIcon.Landscape,
                                                    backendPreference =
                                                        appPreferences.backdropLandscapeRatio,
                                                    options = R.array.backdrop_ratios,
                                                    optionValues = R.array.backdrop_ratios_values,
                                                ),
                                                PreferenceSelect(
                                                    nameStringResource =
                                                        R.string.player_sheet_backdrop_ratio,
                                                    descriptionStringRes =
                                                        R.string
                                                            .player_sheet_backdrop_ratio_summary,
                                                    optionIcon = OptionIcon.Landscape,
                                                    backendPreference =
                                                        appPreferences.playerSheetBackdropRatio,
                                                    options = R.array.backdrop_ratios,
                                                    optionValues = R.array.backdrop_ratios_values,
                                                ),
                                                // One entry, one dialog: the title image's
                                                // portrait, landscape, and player widths.
                                                PreferenceSliders(
                                                    nameStringResource = R.string.title_image,
                                                    descriptionStringRes =
                                                        R.string.title_image_summary,
                                                    sliders =
                                                        listOf(
                                                            PreferenceSlider(
                                                                nameStringResource =
                                                                    R.string.title_portrait_width,
                                                                options =
                                                                    R.array.title_width_options,
                                                                optionValues =
                                                                    R.array.title_width_values,
                                                                backendPreference =
                                                                    appPreferences
                                                                        .titlePortraitWidth,
                                                            ),
                                                            PreferenceSlider(
                                                                nameStringResource =
                                                                    R.string.title_landscape_width,
                                                                options =
                                                                    R.array.title_width_options_low,
                                                                optionValues =
                                                                    R.array.title_width_values_low,
                                                                backendPreference =
                                                                    appPreferences
                                                                        .titleLandscapeWidth,
                                                            ),
                                                            PreferenceSlider(
                                                                nameStringResource =
                                                                    R.string.title_player_width,
                                                                options =
                                                                    R.array.title_width_options_low,
                                                                optionValues =
                                                                    R.array.title_width_values_low,
                                                                backendPreference =
                                                                    appPreferences.titlePlayerWidth,
                                                            ),
                                                        ),
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
                                ),
                        )
                    )
            ),
            PreferenceGroup(
                preferences =
                    listOf(
                        PreferenceCategory(
                            nameStringResource = R.string.settings_category_reader,
                            descriptionStringRes = R.string.settings_category_reader_summary,
                            iconDrawableId = R.drawable.ic_book_open,
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
                                        nameStringResource = R.string.settings_group_display,
                                        preferences =
                                            listOf(
                                                PreferenceSelect(
                                                    nameStringResource =
                                                        R.string.settings_reader_theme,
                                                    descriptionStringRes =
                                                        R.string.settings_reader_theme_summary,
                                                    backendPreference = appPreferences.readerTheme,
                                                    options = R.array.reader_themes_options,
                                                    optionValues = R.array.reader_themes_values,
                                                ),
                                                PreferenceSelect(
                                                    nameStringResource =
                                                        R.string.settings_reader_max_width,
                                                    descriptionStringRes =
                                                        R.string.settings_reader_max_width_summary,
                                                    backendPreference =
                                                        appPreferences.readerMaxWidth,
                                                    options = R.array.reader_max_width_options,
                                                    optionValues = R.array.reader_max_width_values,
                                                ),
                                            ),
                                    ),
                                    PreferenceGroup(
                                        nameStringResource = R.string.settings_group_reading,
                                        preferences =
                                            listOf(
                                                PreferenceSelect(
                                                    nameStringResource =
                                                        R.string.settings_reader_mode,
                                                    descriptionStringRes =
                                                        R.string.settings_reader_mode_summary,
                                                    backendPreference = appPreferences.readerMode,
                                                    options = R.array.reader_modes_options,
                                                    optionValues = R.array.reader_modes_values,
                                                    onUpdate = { loadPreferences(lastIndexes) },
                                                ),
                                                PreferenceSlider(
                                                    nameStringResource =
                                                        R.string.settings_reader_pull_distance,
                                                    descriptionStringRes =
                                                        R.string
                                                            .settings_reader_pull_distance_summary,
                                                    backendPreference =
                                                        appPreferences.readerPullDistance,
                                                    options = R.array.reader_pull_distance_options,
                                                    optionValues =
                                                        R.array.reader_pull_distance_values,
                                                    enabled =
                                                        appPreferences.getValue(
                                                            appPreferences.readerMode
                                                        ) == "scroll",
                                                ),
                                                PreferenceSelect(
                                                    nameStringResource =
                                                        R.string.settings_reader_alignment,
                                                    descriptionStringRes =
                                                        R.string.settings_reader_alignment_summary,
                                                    backendPreference =
                                                        appPreferences.readerTextAlign,
                                                    options = R.array.reader_alignment_options,
                                                    optionValues = R.array.reader_alignment_values,
                                                ),
                                                PreferenceSwitch(
                                                    nameStringResource =
                                                        R.string.settings_reader_normalization,
                                                    descriptionStringRes =
                                                        R.string
                                                            .settings_reader_normalization_summary,
                                                    backendPreference =
                                                        appPreferences.readerTextNormalization,
                                                ),
                                                PreferenceSwitch(
                                                    nameStringResource =
                                                        R.string.settings_reader_hyphenation,
                                                    descriptionStringRes =
                                                        R.string
                                                            .settings_reader_hyphenation_summary,
                                                    backendPreference =
                                                        appPreferences.readerHyphenation,
                                                ),
                                                PreferenceSwitch(
                                                    nameStringResource =
                                                        R.string.settings_reader_tap_turn,
                                                    descriptionStringRes =
                                                        R.string.settings_reader_tap_turn_summary,
                                                    enabled =
                                                        appPreferences.getValue(
                                                            appPreferences.readerMode
                                                        ) != "scroll",
                                                    backendPreference =
                                                        appPreferences.readerTapTurn,
                                                ),
                                            ),
                                    ),
                                    PreferenceGroup(
                                        nameStringResource = R.string.settings_group_typography,
                                        preferences =
                                            listOf(
                                                PreferenceAction(
                                                    nameStringResource =
                                                        R.string.settings_reader_font,
                                                    descriptionStringRes =
                                                        R.string.settings_reader_font_summary,
                                                    backendPreference =
                                                        appPreferences.readerFontFamily,
                                                    formatValue = { readerFontDisplayName(it) },
                                                    onClick = {
                                                        viewModelScope.launch {
                                                            eventsChannel.send(
                                                                SettingsEvent.NavigateToReaderFont
                                                            )
                                                        }
                                                    },
                                                ),
                                                PreferenceSlider(
                                                    nameStringResource =
                                                        R.string.settings_reader_font_size,
                                                    descriptionStringRes =
                                                        R.string.settings_reader_font_size_summary,
                                                    backendPreference =
                                                        appPreferences.readerFontSize,
                                                    options = R.array.reader_font_size_options,
                                                    optionValues = R.array.reader_font_size_values,
                                                ),
                                                PreferenceSlider(
                                                    nameStringResource =
                                                        R.string.settings_reader_font_weight,
                                                    descriptionStringRes =
                                                        R.string
                                                            .settings_reader_font_weight_summary,
                                                    backendPreference =
                                                        appPreferences.readerFontWeight,
                                                    options = R.array.reader_font_weight_options,
                                                    optionValues =
                                                        R.array.reader_font_weight_values,
                                                ),
                                            ),
                                    ),
                                    PreferenceGroup(
                                        nameStringResource = R.string.settings_group_spacing,
                                        preferences =
                                            listOf(
                                                PreferenceSlider(
                                                    nameStringResource =
                                                        R.string.settings_reader_margins,
                                                    descriptionStringRes =
                                                        R.string.settings_reader_margins_summary,
                                                    backendPreference =
                                                        appPreferences.readerPageMargins,
                                                    options = R.array.reader_margins_options,
                                                    optionValues = R.array.reader_margins_values,
                                                ),
                                                PreferenceSlider(
                                                    nameStringResource =
                                                        R.string.settings_reader_line_height,
                                                    descriptionStringRes =
                                                        R.string
                                                            .settings_reader_line_height_summary,
                                                    backendPreference =
                                                        appPreferences.readerLineHeight,
                                                    options = R.array.reader_line_height_options,
                                                    optionValues =
                                                        R.array.reader_line_height_values,
                                                ),
                                                PreferenceSlider(
                                                    nameStringResource =
                                                        R.string.settings_reader_paragraph_spacing,
                                                    descriptionStringRes =
                                                        R.string
                                                            .settings_reader_paragraph_spacing_summary,
                                                    backendPreference =
                                                        appPreferences.readerParagraphSpacing,
                                                    options =
                                                        R.array.reader_paragraph_spacing_options,
                                                    optionValues =
                                                        R.array.reader_paragraph_spacing_values,
                                                ),
                                                PreferenceSlider(
                                                    nameStringResource =
                                                        R.string.settings_reader_word_spacing,
                                                    descriptionStringRes =
                                                        R.string
                                                            .settings_reader_word_spacing_summary,
                                                    backendPreference =
                                                        appPreferences.readerWordSpacing,
                                                    options = R.array.reader_word_spacing_options,
                                                    optionValues =
                                                        R.array.reader_word_spacing_values,
                                                ),
                                                PreferenceSlider(
                                                    nameStringResource =
                                                        R.string.settings_reader_letter_spacing,
                                                    descriptionStringRes =
                                                        R.string
                                                            .settings_reader_letter_spacing_summary,
                                                    backendPreference =
                                                        appPreferences.readerLetterSpacing,
                                                    options = R.array.reader_letter_spacing_options,
                                                    optionValues =
                                                        R.array.reader_letter_spacing_values,
                                                ),
                                            ),
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
                                                                nameStringResource =
                                                                    R.string.settings_group_output,
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
                                                                                OptionIcon.Decode,
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
                                                                                OptionIcon.Monitor,
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
                                                                                OptionIcon
                                                                                    .AudioOutput,
                                                                            backendPreference =
                                                                                appPreferences
                                                                                    .playerMpvAo,
                                                                            options =
                                                                                R.array
                                                                                    .mpv_ao_options,
                                                                            optionValues =
                                                                                R.array.mpv_aos,
                                                                        ),
                                                                    ),
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
                                            )
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
                                                PreferenceSwitch(
                                                    nameStringResource =
                                                        R.string.picture_in_picture_gesture,
                                                    descriptionStringRes =
                                                        R.string.picture_in_picture_gesture_summary,
                                                    backendPreference =
                                                        appPreferences.playerPipGesture,
                                                ),
                                            ),
                                    ),
                                    PreferenceGroup(
                                        nameStringResource = R.string.settings_group_controls,
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
                                                PreferenceSlider(
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
                                                PreferenceSlider(
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
                                                PreferenceSlider(
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
                                                PreferenceSlider(
                                                    nameStringResource =
                                                        R.string.pref_player_mpv_sub_pos,
                                                    descriptionStringRes =
                                                        R.string.pref_player_mpv_sub_pos_summary,
                                                    optionIcon = OptionIcon.Subtitle,
                                                    backendPreference =
                                                        appPreferences.playerMpvSubPosition,
                                                    options = R.array.mpv_sub_pos_options,
                                                    optionValues = R.array.mpv_sub_pos,
                                                ),
                                                PreferenceSelect(
                                                    nameStringResource =
                                                        R.string.pref_player_mpv_sub_anchor,
                                                    descriptionStringRes =
                                                        R.string.pref_player_mpv_sub_anchor_summary,
                                                    optionIcon = OptionIcon.Subtitle,
                                                    backendPreference =
                                                        appPreferences.playerMpvSubAnchor,
                                                    options = R.array.mpv_sub_anchor_options,
                                                    optionValues = R.array.mpv_sub_anchor_values,
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
                        PreferenceSwitch(
                            nameStringResource = R.string.offline_mode,
                            descriptionStringRes = R.string.offline_mode_summary,
                            iconDrawableId = R.drawable.ic_cloud_off,
                            backendPreference = appPreferences.offlineMode,
                        ),
                        PreferenceAction(
                            nameStringResource = R.string.settings_change_server,
                            descriptionStringRes = R.string.settings_change_server_summary,
                            iconDrawableId = R.drawable.ic_server,
                            backendPreference = appPreferences.currentServer,
                            formatValue = { "" },
                            onClick = {
                                viewModelScope.launch {
                                    eventsChannel.send(SettingsEvent.ResetServer)
                                }
                            },
                        ),
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
                                        nameStringResource = R.string.settings_group_mobile_data,
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
                                            ),
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
                                        nameStringResource = R.string.settings_group_timeouts,
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
                                            ),
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
                                        nameStringResource = R.string.settings_group_image_cache,
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
                                            ),
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

    /** The same display style as the subtitle-font row, with the reader's own default sentinel. */
    private fun readerFontDisplayName(family: String?): String? =
        when (family) {
            null,
            "default" -> context.getString(R.string.subtitle_font_default)
            else -> family
        }

    fun loadPreferences(indexes: List<Int> = emptyList()) {
        lastIndexes = indexes
        viewModelScope.launch {
            val isRootLevel = indexes.isEmpty()

            var preferences = topLevelPreferences()

            // Resolve the page's parent: indexes accumulate one entry per pushed settings
            // subpage, and every non-empty list names a visible category.
            for (index in indexes) {
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
                                            is PreferenceSliders -> {
                                                preference.copy(
                                                    enabled =
                                                        preference.enabled &&
                                                            preference.dependencies.all {
                                                                appPreferences.getValue(it)
                                                            },
                                                    sliders =
                                                        preference.sliders.map { slider ->
                                                            slider.copy(
                                                                value =
                                                                    appPreferences.getValue(
                                                                        slider.backendPreference
                                                                    )
                                                            )
                                                        },
                                                )
                                            }
                                            is PreferenceSlider -> {
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
                // Offline mode is about reaching the server, so it rides with the server section.
                preference is PreferenceSwitch -> R.string.settings_group_server
                preference !is PreferenceCategory -> null
                preference.nameStringResource == R.string.settings_category_language ||
                    preference.nameStringResource == R.string.settings_category_interface ||
                    preference.nameStringResource == R.string.settings_category_reader ||
                    preference.nameStringResource == R.string.settings_category_player ->
                    R.string.settings_group_app
                preference.nameStringResource == R.string.settings_category_network ->
                    R.string.settings_group_server
                preference.nameStringResource == R.string.title_download ||
                    preference.nameStringResource == R.string.settings_category_cache ->
                    R.string.settings_group_storage
                preference.nameStringResource == R.string.about -> R.string.settings_group_about
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
                    is PreferenceSlider ->
                        appPreferences.setValue(
                            action.preference.backendPreference,
                            action.preference.value,
                        )
                }
            }
        }
    }
}
