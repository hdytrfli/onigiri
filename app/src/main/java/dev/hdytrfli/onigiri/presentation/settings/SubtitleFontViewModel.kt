package dev.hdytrfli.onigiri.presentation.settings

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.hdytrfli.onigiri.settings.domain.AppPreferences
import dev.hdytrfli.onigiri.settings.domain.SubtitleFonts
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface SubtitleFontEvent {
    /** The picked file was read but did not yield a usable font. */
    data object InvalidFontFile : SubtitleFontEvent
}

/**
 * Backs the "Subtitle font" picker. The font list is loaded off the main thread because
 * [SubtitleFonts.available] reads the filesystem and unpacks the bundled fonts.
 */
@HiltViewModel
class SubtitleFontViewModel
@Inject
constructor(
    private val application: Application,
    private val appPreferences: AppPreferences,
) : ViewModel() {
    private val _state =
        MutableStateFlow(
            SubtitleFontState(
                selectedFamily = appPreferences.getValue(appPreferences.playerMpvSubFont)
            )
        )
    val state = _state.asStateFlow()

    private val eventsChannel = Channel<SubtitleFontEvent>()
    val events = eventsChannel.receiveAsFlow()

    init {
        refresh()
    }

    fun selectFont(font: SubtitleFonts.Font) {
        appPreferences.setValue(appPreferences.playerMpvSubFont, font.family)
        _state.update { it.copy(selectedFamily = font.family) }
    }

    /** Removes an imported font and, if it was selected, falls back to mpv's default font. */
    fun deleteFont(font: SubtitleFonts.Font) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { SubtitleFonts.delete(application, font) }
            if (
                appPreferences.getValue(appPreferences.playerMpvSubFont).equals(font.family, true)
            ) {
                appPreferences.setValue(
                    appPreferences.playerMpvSubFont,
                    SubtitleFonts.DEFAULT_FAMILY,
                )
            }
            refresh()
        }
    }

    /** Imports a picked file and selects it, or reports that it is not a usable font. */
    fun importFont(uri: Uri) {
        viewModelScope.launch {
            val imported =
                withContext(Dispatchers.IO) { SubtitleFonts.importFrom(application, uri) }
            if (imported == null) {
                eventsChannel.send(SubtitleFontEvent.InvalidFontFile)
            } else {
                appPreferences.setValue(appPreferences.playerMpvSubFont, imported.family)
                refresh()
            }
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            val fonts = withContext(Dispatchers.IO) { SubtitleFonts.available(application) }
            _state.update {
                it.copy(
                    isLoading = false,
                    fonts = fonts,
                    selectedFamily = appPreferences.getValue(appPreferences.playerMpvSubFont),
                )
            }
        }
    }
}
