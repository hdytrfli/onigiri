package dev.jdtech.jellyfin.settings.domain

import android.content.SharedPreferences
import dev.jdtech.jellyfin.settings.domain.models.Preference
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import timber.log.Timber

class AppPreferences @Inject constructor(val sharedPreferences: SharedPreferences) {
    /**
     * Default preferred language, matching the device language (ISO 639-2, e.g. "eng"), so the
     * audio/subtitle selectors start aligned with the app language.
     */
    private val defaultLanguage: String? = runCatching {
        Locale.getDefault().getISO3Language()
    }
        .getOrNull()
        ?.takeIf { it != "und" }

    // Server
    val currentServer = Preference<String?>("pref_current_server", null)

    /** Whether the new onboarding flow has been completed at least once. */
    val onboardingComplete = Preference("pref_onboarding_complete", false)

    /** The app has been intentionally logged out and must reopen on profile selection. */
    val profileLoggedOut = Preference("pref_profile_logged_out", false)

    /** Whether the one-time pre-onboarding session purge has already run. */
    val onboardingSessionCleared = Preference("pref_onboarding_session_cleared", false)

    // Language
    val preferredAudioLanguage = Preference<String?>("pref_audio_language", defaultLanguage)
    val preferredSubtitleLanguage = Preference<String?>("pref_subtitle_language", defaultLanguage)

    // Interface
    val hideStatusBar = Preference("pref_hide_status_bar", false)
    val homeSuggestions = Preference<Boolean>("home_suggestions", true)
    val homeContinueWatching = Preference<Boolean>("home_continue_watching", true)
    val homeNextUp = Preference<Boolean>("home_next_up", true)
    val homeLatest = Preference<Boolean>("home_latest", true)
    val displayExtraInfo = Preference("pref_display_extra_info", false)

    /** Show a blurhash of each image while it loads, instead of the flat surface colour. */
    val blurHash = Preference("pref_blur_hash", true)

    /** Preview the previous screen while swiping back (Android predictive back). */
    val predictiveBack = Preference("pref_predictive_back", false)

    /** Whether the wide-layout navigation rail is used: automatic, always, landscape or never. */
    val tabletUiMode = Preference("pref_tablet_ui_mode", "auto")

    /** Which metadata the movie/show list cards show: release year, rating, age rating. */
    val listDisplayFields = Preference("pref_list_display_fields", setOf("year", "rating", "age"))
    val backdropPortraitRatio = Preference("pref_backdrop_portrait_ratio", "1.5")
    val backdropLandscapeRatio = Preference("pref_backdrop_landscape_ratio", "2.3333333")

    /** The image header ratio in the player's media sheets, 4:3 by default. */
    val playerSheetBackdropRatio = Preference("pref_player_sheet_backdrop_ratio", "1.3333334")
    val titlePortraitWidth = Preference("pref_title_portrait_width", "0.7")
    val titleLandscapeWidth = Preference("pref_title_landscape_width", "0.3")
    val titlePlayerWidth = Preference("pref_title_player_width", "0.2")

    // Layout grid (1..8 columns). "Poster" is the 2:3 poster shape; "thumbnail" is the 16:9 shape.
    val gridPosterColumnsPortrait = Preference("pref_grid_poster_columns_portrait", "2")
    val gridPosterColumnsLandscape = Preference("pref_grid_poster_columns_landscape", "4")
    val gridThumbnailColumnsPortrait = Preference("pref_grid_thumbnail_columns_portrait", "3")
    val gridThumbnailColumnsLandscape = Preference("pref_grid_thumbnail_columns_landscape", "4")
    // How many suggestions the home screen carousel shows at once, per orientation.
    val gridSuggestionColumnsPortrait = Preference("pref_grid_suggestion_columns_portrait", "1")
    val gridSuggestionColumnsLandscape = Preference("pref_grid_suggestion_columns_landscape", "3")
    val gridProfileColumnsPortrait = Preference("pref_grid_profile_columns_portrait", "4")
    val gridProfileColumnsLandscape = Preference("pref_grid_profile_columns_landscape", "6")
    val gridSpacing = Preference("pref_grid_spacing", "1")

    // Player
    val playerAlwaysPlaySubtitles = Preference("pref_player_always_play_subtitles", false)
    val playerBrightness = Preference("pref_player_brightness", -1.0f)

    // Player - overlay
    val playerControlsTimeout = Preference("pref_player_controls_timeout", "5000")

    // Player - time display
    val playerTimeDisplay = Preference("pref_player_time_display", "duration")
    val playerTimeEndFormat = Preference("pref_player_time_end_format", "24")

    // Derived flag, only used to conditionally show the end-time format select.
    val playerTimeShowFinish = Preference("pref_player_time_show_finish", false)

    // Player - mpv
    val playerMpv = Preference("pref_player_mpv", false)
    val playerMpvHwdec = Preference("pref_player_mpv_hwdec", "mediacodec")
    val playerMpvVo = Preference("pref_player_mpv_vo", "gpu-next")
    val playerMpvAo = Preference("pref_player_mpv_ao", "aaudio")

    // Player - mpv subtitles (applied as mpv sub-* options at playback start)
    val playerMpvSubFontSize = Preference("pref_player_mpv_sub_font_size", "28")
    val playerMpvSubFont = Preference("pref_player_mpv_sub_font", SubtitleFonts.DEFAULT_FAMILY)
    val playerMpvSubFontWeight =
        Preference("pref_player_mpv_sub_font_weight", SubtitleFonts.DEFAULT_WEIGHT.toString())
    val playerMpvSubColor = Preference("pref_player_mpv_sub_color", "#FFFFFFFF")
    val playerMpvSubPosition = Preference("pref_player_mpv_sub_pos", "100")

    /** Where subtitles are anchored: the whole screen, or just the video frame. */
    val playerMpvSubAnchor = Preference("pref_player_mpv_sub_anchor", "screen")

    // Reader
    val readerTheme = Preference("pref_reader_theme", "dark")
    val readerFontFamily = Preference("pref_reader_font_family", "default")
    val readerFontSize = Preference("pref_reader_font_size", "1.0")
    val readerFontWeight = Preference("pref_reader_font_weight", "1.0")
    val readerPageMargins = Preference("pref_reader_page_margins", "1.0")
    val readerMode = Preference("pref_reader_mode", "single")
    val readerPullDistance = Preference("pref_reader_pull_distance", "0.2")
    val readerTextAlign = Preference("pref_reader_text_align", "default")
    val readerTextNormalization = Preference("pref_reader_normalize", true)
    val readerHyphenation = Preference("pref_reader_hyphenation", false)
    val readerTapTurn = Preference("pref_reader_tap_turn", true)
    val readerMaxWidth = Preference("pref_reader_max_width", "default")
    val readerLineHeight = Preference("pref_reader_line_height", "1.5")
    val readerParagraphSpacing = Preference("pref_reader_paragraph_spacing", "1.3")
    val readerWordSpacing = Preference("pref_reader_word_spacing", "0.0")
    val readerLetterSpacing = Preference("pref_reader_letter_spacing", "0.0")
    val readerBrightness = Preference("pref_reader_brightness", "1.0")
    val playerMpvSubBorderSize = Preference("pref_player_mpv_sub_border_size", "3")
    val playerMpvSubBorderColor = Preference("pref_player_mpv_sub_border_color", "#FF000000")
    val playerMpvSubBorderOpacity = Preference("pref_player_mpv_sub_border_opacity", "255")
    val playerMpvSubAssOverride = Preference("pref_player_mpv_sub_ass_override", "scale")
    val playerMpvSubBorderEnabled = Preference("pref_player_mpv_sub_border_enabled", true)
    val playerMpvSubBorderStyle = Preference("pref_player_mpv_sub_border_style", "opaque-box")

    // Player - gestures
    val playerGesturesVolume = Preference("pref_player_gestures_volume", true)
    val playerGesturesBrightness = Preference("pref_player_gestures_brightness", true)
    val playerGesturesZoom = Preference("pref_player_gestures_zoom", true)
    val playerGesturesSeek = Preference("pref_player_gestures_seek", true)
    val playerGesturesDoubleTap = Preference("pref_player_gestures_double_tap", false)
    val playerGesturesSeekTrickplay = Preference("pref_player_gestures_seek_trickplay", true)
    val playerGesturesChapterSkip = Preference("pref_player_gestures_chapter_skip", false)
    val playerGesturesStartMaximized = Preference("pref_player_start_maximized", false)

    /** Pinch to zoom the video freely, instead of the pinch filling the screen. */
    val playerGesturesDynamicZoom = Preference("pref_player_dynamic_zoom", false)

    // Player - seeking
    val playerSeekBackInc = Preference("pref_player_seek_back_inc", 5_000L)
    val playerSeekForwardInc = Preference("pref_player_seek_forward_inc", 5_000L)
    val playerChapterMarkers = Preference("pref_player_chapter_markers", true)

    // Player - Media Segments
    val playerMediaSegmentsSkipButton
        get() = Preference("pref_player_media_segments_skip_button", true)

    val playerMediaSegmentsSkipButtonType
        get() = Preference("pref_player_media_segments_skip_button_type", setOf("INTRO", "OUTRO"))

    val playerMediaSegmentsSkipButtonDuration
        get() = Preference("pref_player_media_segments_skip_button_duration", 5L)

    val playerMediaSegmentsAutoSkip
        get() = Preference("pref_player_media_segments_auto_skip", false)

    val playerMediaSegmentsAutoSkipMode
        get() =
            Preference(
                "pref_player_media_segments_auto_skip_mode",
                Constants.PlayerMediaSegmentsAutoSkip.ALWAYS,
            )

    val playerMediaSegmentsAutoSkipType
        get() = Preference("pref_player_media_segments_auto_skip_type", setOf("INTRO", "OUTRO"))

    val playerMediaSegmentsNextEpisodeThreshold
        get() = Preference("pref_player_media_segments_next_episode_threshold", 5_000L)

    // Player - trickplay
    val playerTrickplay = Preference("pref_player_trickplay", true)

    // Player - PiP
    val playerPipGesture = Preference("pref_player_picture_in_picture_gesture", false)

    // Downloads
    val downloadOverMobileData = Preference("pref_downloads_mobile_data", false)
    val downloadWhenRoaming = Preference("pref_downloads_roaming", false)

    // Network
    val requestTimeout =
        Preference("pref_network_request_timeout", Constants.NETWORK_DEFAULT_REQUEST_TIMEOUT)
    val connectTimeout =
        Preference("pref_network_connect_timeout", Constants.NETWORK_DEFAULT_CONNECT_TIMEOUT)
    val socketTimeout =
        Preference("pref_network_socket_timeout", Constants.NETWORK_DEFAULT_SOCKET_TIMEOUT)

    // Cache
    val imageCache = Preference("pref_image_cache", true)
    val imageCacheSize = Preference("pref_image_cache_size", 20)

    // Sorting
    val sortBy = Preference("pref_sort_by", "SortName")
    val sortOrder = Preference("pref_sort_order", "Ascending")

    // Offline mode
    val offlineMode = Preference("pref_offline_mode", false)

    // General
    val confirmExit = Preference("pref_confirm_exit", true)

    /** Offers the playback statistics button, and with it the stats overlay, in the player. */
    val playerStats = Preference("pref_player_stats", true)

    // Migrations
    val mpvMigrated = Preference("mpv_migrated", false)

    inline fun <reified T> getValue(preference: Preference<T>): T {
        return try {
            @Suppress("UNCHECKED_CAST")
            when (preference.defaultValue) {
                is Boolean ->
                    sharedPreferences.getBoolean(preference.backendName, preference.defaultValue)
                        as T
                is Int ->
                    sharedPreferences.getInt(preference.backendName, preference.defaultValue) as T
                is Long ->
                    sharedPreferences.getLong(preference.backendName, preference.defaultValue) as T
                is Float ->
                    sharedPreferences.getFloat(preference.backendName, preference.defaultValue) as T
                is String? ->
                    sharedPreferences.getString(preference.backendName, preference.defaultValue)
                        as T
                is Set<*> ->
                    sharedPreferences.getStringSet(
                        preference.backendName,
                        preference.defaultValue as Set<String>,
                    ) as T
                else -> preference.defaultValue
            }
        } catch (_: Exception) {
            Timber.w(
                "Failed to load ${preference.backendName} preference. Resetting to default value..."
            )
            setValue(preference, preference.defaultValue)
            preference.defaultValue
        }
    }

    /** Emits the current value of [preference] and then emits a new value every time it changes. */
    inline fun <reified T> observe(preference: Preference<T>): Flow<T> = callbackFlow {
        trySend(getValue(preference))

        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, changedKey ->
            if (changedKey == preference.backendName) {
                trySend(getValue(preference))
            }
        }
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    inline fun <reified T> setValue(preference: Preference<T>, value: T) {
        val editor = sharedPreferences.edit()
        @Suppress("UNCHECKED_CAST")
        when (preference.defaultValue) {
            is Boolean -> editor.putBoolean(preference.backendName, value as Boolean)
            is Int -> editor.putInt(preference.backendName, value as Int)
            is Long -> editor.putLong(preference.backendName, value as Long)
            is Float -> editor.putFloat(preference.backendName, value as Float)
            is String? -> editor.putString(preference.backendName, value as String?)
            is Set<*> -> editor.putStringSet(preference.backendName, value as Set<String>)
            else -> throw Exception()
        }
        editor.apply()
    }

    // Profile PINs are local to this device and deliberately independent from the Jellyfin
    // password. A per-profile random salt keeps identical PINs from sharing a hash.

    fun hasProfilePin(serverId: String, userId: UUID): Boolean =
        sharedPreferences.getString(profilePinHashKey(serverId, userId), null) != null

    fun setProfilePin(serverId: String, userId: UUID, pin: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val saltHex = salt.toHex()
        sharedPreferences
            .edit()
            .putString(profilePinSaltKey(serverId, userId), saltHex)
            .putString(profilePinHashKey(serverId, userId), hashPin(salt, pin))
            .apply()
    }

    fun verifyProfilePin(serverId: String, userId: UUID, pin: String): Boolean {
        val salt = sharedPreferences.getString(profilePinSaltKey(serverId, userId), null)
        val stored = sharedPreferences.getString(profilePinHashKey(serverId, userId), null)
        if (salt == null || stored == null) return false
        return stored == hashPin(salt.hexToBytes(), pin)
    }

    fun clearProfilePin(serverId: String, userId: UUID) {
        sharedPreferences
            .edit()
            .remove(profilePinSaltKey(serverId, userId))
            .remove(profilePinHashKey(serverId, userId))
            .apply()
    }

    fun setProfileAdmin(serverId: String, userId: UUID, isAdmin: Boolean) {
        sharedPreferences.edit().putBoolean(profileAdminKey(serverId, userId), isAdmin).apply()
    }

    fun isProfileAdmin(serverId: String, userId: UUID): Boolean =
        sharedPreferences.getBoolean(profileAdminKey(serverId, userId), false)

    fun clearProfileMetadata(serverId: String, userId: UUID) {
        sharedPreferences
            .edit()
            .remove(profilePinSaltKey(serverId, userId))
            .remove(profilePinHashKey(serverId, userId))
            .remove(profileAdminKey(serverId, userId))
            .apply()
    }

    fun clearAllProfileMetadata() {
        val keys =
            sharedPreferences.all.keys.filter {
                it.startsWith("pref_profile_pin_") || it.startsWith("pref_profile_admin_")
            }
        if (keys.isEmpty()) return
        sharedPreferences.edit().apply { keys.forEach(::remove) }.apply()
    }

    /** Reading position and page counts are user-scoped caches, not server data. */
    fun clearReaderState() {
        val keys =
            sharedPreferences.all.keys.filter {
                it.startsWith("reader_locator_") || it.startsWith("reader_page_count_")
            }
        if (keys.isEmpty()) return
        sharedPreferences.edit().apply { keys.forEach(::remove) }.apply()
    }

    private fun profilePinSaltKey(serverId: String, userId: UUID) =
        "pref_profile_pin_salt_${serverId}_$userId"

    private fun profilePinHashKey(serverId: String, userId: UUID) =
        "pref_profile_pin_hash_${serverId}_$userId"

    private fun profileAdminKey(serverId: String, userId: UUID) =
        "pref_profile_admin_${serverId}_$userId"

    private fun hashPin(salt: ByteArray, pin: String): String =
        MessageDigest.getInstance("SHA-256").digest(salt + pin.toByteArray(Charsets.UTF_8)).toHex()

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private fun String.hexToBytes(): ByteArray =
        chunked(2).map { it.toInt(16).toByte() }.toByteArray()
}
