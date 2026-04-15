package com.admoseley.quietforaminute.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.admoseley.quietforaminute.data.datastore.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefsRepository: PreferencesRepository
) : ViewModel() {

    val defaultVolume = prefsRepository.defaultVolume.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = 7
    )

    val overlayEnabled = prefsRepository.overlayEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = true
    )

    val chimeOnMute = prefsRepository.chimeOnMute.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = true
    )

    val chimeOnRestore = prefsRepository.chimeOnRestore.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = true
    )

    val muteChimeUri = prefsRepository.muteChimeUri.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null
    )

    val restoreChimeUri = prefsRepository.restoreChimeUri.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null
    )

    val themeMode = prefsRepository.themeMode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = "SYSTEM"
    )

    fun setDefaultVolume(volume: Int) {
        viewModelScope.launch { prefsRepository.setDefaultVolume(volume) }
    }

    fun setOverlayEnabled(enabled: Boolean) {
        viewModelScope.launch { prefsRepository.setOverlayEnabled(enabled) }
    }

    fun setChimeOnMute(enabled: Boolean) {
        viewModelScope.launch { prefsRepository.setChimeOnMute(enabled) }
    }

    fun setChimeOnRestore(enabled: Boolean) {
        viewModelScope.launch { prefsRepository.setChimeOnRestore(enabled) }
    }

    fun setMuteChimeUri(uri: String?) {
        viewModelScope.launch { prefsRepository.setMuteChimeUri(uri) }
    }

    fun setRestoreChimeUri(uri: String?) {
        viewModelScope.launch { prefsRepository.setRestoreChimeUri(uri) }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch { prefsRepository.setThemeMode(mode) }
    }
}
