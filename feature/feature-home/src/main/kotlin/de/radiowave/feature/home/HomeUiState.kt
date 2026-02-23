package de.radiowave.feature.home

import de.radiowave.core.model.Station

data class HomeUiState(
    val recentStations: List<Station> = emptyList(),
    val favoriteStations: List<Station> = emptyList(),
    val topStations: List<Station> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)
