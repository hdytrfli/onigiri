package dev.jdtech.jellyfin.presentation.reader

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.settings.domain.AppPreferences
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Backs the reader settings page in the app's Settings screen. */
@HiltViewModel
class ReaderSettingsViewModel @Inject constructor(private val appPreferences: AppPreferences) :
    ViewModel() {
    private val _settings = MutableStateFlow(appPreferences.readerSettings())
    val settings = _settings.asStateFlow()

    fun update(settings: ReaderSettings) {
        appPreferences.setReaderSettings(settings)
        _settings.value = settings
    }
}
