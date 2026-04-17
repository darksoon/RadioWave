// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.feature.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.darksoon.radiowave.core.data.repository.CoverArtRepository
import de.darksoon.radiowave.core.data.repository.StationRepository
import de.darksoon.radiowave.core.model.Station
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val coverArtRepository: CoverArtRepository,
    private val stationRepository: StationRepository,
) : ViewModel() {

    private val _coverArtUrl = MutableStateFlow<String?>(null)
    val coverArtUrl: StateFlow<String?> = _coverArtUrl.asStateFlow()

    private val _similarStations = MutableStateFlow<List<Station>>(emptyList())
    val similarStations: StateFlow<List<Station>> = _similarStations.asStateFlow()

    suspend fun loadCoverArt(artist: String?, title: String?) {
        _coverArtUrl.value = coverArtRepository.fetchCoverArt(artist, title)
    }

    fun clearCoverArt() {
        _coverArtUrl.value = null
    }

    fun loadSimilarStations(station: Station) {
        viewModelScope.launch {
            _similarStations.value = stationRepository.getSimilarStations(station)
        }
    }

    fun clearSimilarStations() {
        _similarStations.value = emptyList()
    }
}
