package dev.jdtech.jellyfin.player.local.domain

import android.content.SharedPreferences
import java.util.UUID
import javax.inject.Inject

/**
 * Remembers the audio and subtitle track the user explicitly chose for each media item, so the
 * choice is restored the next time that item is played. Kept locally (per device) because Jellyfin
 * has no per-item track API.
 *
 * Entries are scoped to the active server + user: when either changes, every remembered choice is
 * dropped so one account's selection never leaks into another.
 */
class TrackSelectionStore @Inject constructor(private val sharedPreferences: SharedPreferences) {

    /** The stored audio track index for [itemId], or [UNSET] if the user never chose one. */
    fun getAudio(scope: String, itemId: UUID): Int = get(scope, AUDIO_PREFIX, itemId)

    fun setAudio(scope: String, itemId: UUID, index: Int) = set(scope, AUDIO_PREFIX, itemId, index)

    /** The stored subtitle track index for [itemId], or [UNSET] if the user never chose one. */
    fun getText(scope: String, itemId: UUID): Int = get(scope, TEXT_PREFIX, itemId)

    fun setText(scope: String, itemId: UUID, index: Int) = set(scope, TEXT_PREFIX, itemId, index)

    /** The stored subtitle offset (seconds) for [itemId], or [Float.NaN] if none is stored. */
    fun getSubtitleOffset(scope: String, itemId: UUID): Float {
        ensureScope(scope)
        return sharedPreferences.getFloat(OFFSET_PREFIX + itemId, Float.NaN)
    }

    fun setSubtitleOffset(scope: String, itemId: UUID, seconds: Float) {
        ensureScope(scope)
        sharedPreferences.edit().putFloat(OFFSET_PREFIX + itemId, seconds).apply()
    }

    private fun get(scope: String, prefix: String, itemId: UUID): Int {
        ensureScope(scope)
        return sharedPreferences.getInt(prefix + itemId, UNSET)
    }

    private fun set(scope: String, prefix: String, itemId: UUID, index: Int) {
        ensureScope(scope)
        sharedPreferences.edit().putInt(prefix + itemId, index).apply()
    }

    /**
     * Clears every remembered selection when the active server/user differs from the stored one.
     */
    private fun ensureScope(scope: String) {
        if (sharedPreferences.getString(SCOPE_KEY, null) == scope) return
        val editor = sharedPreferences.edit()
        sharedPreferences.all.keys
            .filter {
                it.startsWith(AUDIO_PREFIX) ||
                    it.startsWith(TEXT_PREFIX) ||
                    it.startsWith(OFFSET_PREFIX)
            }
            .forEach(editor::remove)
        editor.putString(SCOPE_KEY, scope)
        editor.apply()
    }

    companion object {
        /** No explicit choice stored for this track type. */
        const val UNSET = -2

        /** The track is disabled ("None" in the track menu). */
        const val DISABLED = -1

        private const val AUDIO_PREFIX = "pref_track_audio_"
        private const val TEXT_PREFIX = "pref_track_text_"
        private const val OFFSET_PREFIX = "pref_sub_offset_"
        private const val SCOPE_KEY = "pref_track_scope"
    }
}
