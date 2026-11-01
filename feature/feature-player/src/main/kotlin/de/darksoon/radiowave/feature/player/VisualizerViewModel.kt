// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.feature.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.darksoon.radiowave.core.data.repository.AppSettingsState
import de.darksoon.radiowave.core.data.repository.SettingsRepository
import de.darksoon.radiowave.core.player.visualizer.AudioVisualizerTap
import de.darksoon.radiowave.core.player.visualizer.SpectrumFrame
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

// REA 95fcb33: neue Datei. Fundstellen: p000/C0096bp.java (ViewModel: Felder tap, settings,
// settings-StateFlow mit WhileSubscribed(5000)), Hilt-Factory p000/C0134cp.java:34,
// keepCapturing = p000/C0624q.java Fall 7, Umschalten = p000/C0197ee.java Fall 2 +
// p000/C0059ap.java Fall 0 (edit { visualizer_enabled = !current }).
// REA-UNSICHER: Klassen- und Methodennamen geraten.

@HiltViewModel
class VisualizerViewModel @Inject constructor(
    private val visualizerTap: AudioVisualizerTap,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val settings: StateFlow<AppSettingsState> = settingsRepository.data.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppSettingsState(),
    )

    val spectrum: StateFlow<SpectrumFrame> = visualizerTap.spectrum

    fun setVisualizerEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setVisualizerEnabled(enabled) }
    }

    /**
     * Keeps the analyser fed for as long as the calling coroutine runs (the player screen
     * runs it inside repeatOnLifecycle(STARTED)); the handle is released on cancellation.
     */
    suspend fun keepCapturing() {
        visualizerTap.acquire().use { awaitCancellation() }
    }
}
