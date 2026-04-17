// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.feature.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.darksoon.radiowave.core.data.repository.FavoriteRepository
import de.darksoon.radiowave.core.model.Station
import de.darksoon.radiowave.core.player.RadioPlayerManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FavoritesUiState(
    val stations: List<Station> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val favoriteRepository: FavoriteRepository,
    private val playerManager: RadioPlayerManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    val playerState = playerManager.playerState

    init {
        observeFavorites()
    }

    private fun observeFavorites() {
        viewModelScope.launch {
            favoriteRepository.getFavorites()
                .onStart {
                    _uiState.update { state ->
                        state.copy(
                            isLoading = true,
                            error = null,
                        )
                    }
                }
                .catch { throwable ->
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            error = throwable.message ?: "Favoriten konnten nicht geladen werden.",
                        )
                    }
                }
                .collect { favorites ->
                    _uiState.update { state ->
                        state.copy(
                            stations = favorites,
                            isLoading = false,
                            error = null,
                        )
                    }
                }
        }
    }

    fun playStation(station: Station) {
        viewModelScope.launch {
            playerManager.playStation(station)
        }
    }

    fun toggleFavorite(station: Station) {
        viewModelScope.launch {
            favoriteRepository.toggleFavorite(station)
        }
    }

    fun moveFavoriteUp(station: Station) {
        val current = _uiState.value.stations
        val index = current.indexOfFirst { it.uuid == station.uuid }
        if (index <= 0) return
        val reordered = current.toMutableList().apply {
            add(index - 1, removeAt(index))
        }
        viewModelScope.launch {
            favoriteRepository.reorderFavorites(reordered.map { it.uuid })
        }
    }

    fun moveFavoriteToTop(station: Station) {
        val current = _uiState.value.stations
        val index = current.indexOfFirst { it.uuid == station.uuid }
        if (index <= 0) return
        val reordered = current.toMutableList().apply {
            add(0, removeAt(index))
        }
        viewModelScope.launch {
            favoriteRepository.reorderFavorites(reordered.map { it.uuid })
        }
    }
}

