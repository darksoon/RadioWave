// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.feature.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.darksoon.radiowave.core.data.repository.CustomStationRepository
import de.darksoon.radiowave.core.data.repository.FavoriteRepository
import de.darksoon.radiowave.core.data.repository.SettingsRepository
import de.darksoon.radiowave.core.model.Station
import de.darksoon.radiowave.core.player.RadioPlayerManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class FavoritesUiState(
    val stations: List<Station> = emptyList(),
    val customStations: List<Station> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val favoriteRepository: FavoriteRepository,
    private val customStationRepository: CustomStationRepository,
    private val playerManager: RadioPlayerManager,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    val playerState = playerManager.playerState

    /** Live flag for "ask before removing favorite" — drives the confirm dialog. */
    val confirmRemove: StateFlow<Boolean> = settingsRepository.confirmRemoveFavorite
        .stateIn(viewModelScope, SharingStarted.Eagerly, settingsRepository.initialSnapshotBlocking().confirmRemoveFavorite)

    // Tracked so retry() can cancel the in-flight collector before starting a
    // new one — otherwise repeated retries stack live flow collectors and
    // produce duplicate UI updates + redundant DB work.
    private var favoritesJob: Job? = null
    private var customStationsJob: Job? = null

    init {
        observeFavorites()
        observeCustomStations()
    }

    /** Re-subscribe to favorites + custom stations. Used as retry callback in the UI. */
    fun retry() {
        _uiState.update { it.copy(error = null, isLoading = true) }
        observeFavorites()
        observeCustomStations()
    }

    private fun observeFavorites() {
        favoritesJob?.cancel()
        favoritesJob = viewModelScope.launch {
            favoriteRepository.getFavorites()
                .onStart {
                    _uiState.update { it.copy(isLoading = true, error = null) }
                }
                .catch { throwable ->
                    _uiState.update { it.copy(isLoading = false, error = throwable.message) }
                }
                .collect { favorites ->
                    _uiState.update { it.copy(stations = favorites, isLoading = false, error = null) }
                }
        }
    }

    private fun observeCustomStations() {
        customStationsJob?.cancel()
        customStationsJob = viewModelScope.launch {
            customStationRepository.getCustomStations()
                .catch { }
                .collect { custom ->
                    _uiState.update { it.copy(customStations = custom) }
                }
        }
    }

    fun playStation(station: Station) {
        viewModelScope.launch { playerManager.playStation(station) }
    }

    fun toggleFavorite(station: Station) {
        viewModelScope.launch { favoriteRepository.toggleFavorite(station) }
    }

    fun moveFavoriteUp(station: Station) {
        val current = _uiState.value.stations
        val index = current.indexOfFirst { it.uuid == station.uuid }
        if (index <= 0) return
        val reordered = current.toMutableList().apply { add(index - 1, removeAt(index)) }
        viewModelScope.launch { favoriteRepository.reorderFavorites(reordered.map { it.uuid }) }
    }

    fun moveFavoriteToTop(station: Station) {
        val current = _uiState.value.stations
        val index = current.indexOfFirst { it.uuid == station.uuid }
        if (index <= 0) return
        val reordered = current.toMutableList().apply { add(0, removeAt(index)) }
        viewModelScope.launch { favoriteRepository.reorderFavorites(reordered.map { it.uuid }) }
    }

    /**
     * Returns true if [streamUrl] is a valid http/https URL — usable for UI validation.
     *
     * Uri.parse is lax: "https:" or "http://" alone parse successfully and yield
     * the right scheme, so the previous scheme-only check let through obviously
     * broken inputs that then crashed ExoPlayer at play time. We now also
     * require a non-blank host.
     */
    fun isValidStreamUrl(streamUrl: String): Boolean {
        val trimmed = streamUrl.trim()
        if (trimmed.isBlank()) return false
        val uri = runCatching { android.net.Uri.parse(trimmed) }.getOrNull() ?: return false
        val scheme = uri.scheme?.lowercase()
        val host = uri.host
        return (scheme == "http" || scheme == "https") && !host.isNullOrBlank()
    }

    fun addCustomStation(name: String, streamUrl: String) {
        if (!isValidStreamUrl(streamUrl)) return
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) return
        viewModelScope.launch {
            val station = Station(
                uuid = "custom-${UUID.randomUUID()}",
                name = trimmedName,
                streamUrl = streamUrl.trim(),
            )
            customStationRepository.addCustomStation(station)
            favoriteRepository.toggleFavorite(station.copy(isFavorite = false))
        }
    }

    fun deleteCustomStation(station: Station) {
        viewModelScope.launch {
            customStationRepository.deleteCustomStation(station.uuid)
            favoriteRepository.toggleFavorite(station.copy(isFavorite = true))
        }
    }
}
