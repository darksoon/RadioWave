// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.feature.savedtracks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.darksoon.radiowave.core.data.repository.SavedTracksRepository
import de.darksoon.radiowave.core.model.SavedTrack
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Backs the "Saved songs" button/dialog and the player's "Save song" button.
 *
 * REA 95fcb33: p000/x94.java (Felder f23988d = hasError, f23989e = tracks),
 * catch-Zweig p000/w94.java, onEach p000/sw0.java Fall 7, launch-Hülle p000/ba2.java Fall 17.
 * Sicherheit: hoch (Logik), Namen mittel.
 */
@HiltViewModel
class SavedTracksViewModel @Inject constructor(
    private val repository: SavedTracksRepository,
) : ViewModel() {

    private val _hasError = MutableStateFlow(false)
    val hasError: StateFlow<Boolean> = _hasError.asStateFlow()

    val tracks: StateFlow<List<SavedTrack>> = repository.tracks
        .onEach { _hasError.value = false }
        .catch {
            _hasError.value = true
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun save(title: String, artist: String?, stationName: String) {
        launchSafely { repository.save(title, artist, stationName) }
    }

    fun remove(id: String) {
        launchSafely { repository.remove(id) }
    }

    fun clearAll() {
        launchSafely { repository.clear() }
    }

    private fun launchSafely(block: suspend () -> Unit): Job = viewModelScope.launch {
        try {
            block()
            _hasError.value = false
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _hasError.value = true
        }
    }
}
