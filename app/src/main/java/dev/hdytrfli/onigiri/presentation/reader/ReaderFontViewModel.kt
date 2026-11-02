package dev.hdytrfli.onigiri.presentation.reader

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.hdytrfli.onigiri.settings.domain.AppPreferences
import dev.hdytrfli.onigiri.settings.domain.SubtitleFonts
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ReaderFontState(
    val fonts: List<SubtitleFonts.Font> = emptyList(),
    val selectedFamily: String = SubtitleFonts.DEFAULT_FAMILY,
)

/** Offers the same font list as the subtitle font setting, for the book text. */
@HiltViewModel
class ReaderFontViewModel
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val appPreferences: AppPreferences,
) : ViewModel() {
    private val _state = MutableStateFlow(ReaderFontState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val fonts =
                withContext(Dispatchers.IO) {
                    SubtitleFonts.available(context).filter { !it.removable }
                }
            val stored = appPreferences.getValue(appPreferences.readerFontFamily)
            _state.value =
                ReaderFontState(
                    fonts = fonts,
                    selectedFamily =
                        if (stored.isBlank() || stored == "default") {
                            SubtitleFonts.DEFAULT_FAMILY
                        } else {
                            stored
                        },
                )
        }
    }

    fun select(font: SubtitleFonts.Font) {
        val stored = if (font.family == SubtitleFonts.DEFAULT_FAMILY) "default" else font.family
        appPreferences.setValue(appPreferences.readerFontFamily, stored)
        _state.value = _state.value.copy(selectedFamily = font.family)
    }
}
