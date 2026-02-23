package de.radiowave.feature.home

import de.radiowave.core.model.Station

enum class SortOption {
    POPULARITY,
    NAME,
    COUNTRY,
}

data class HomeUiState(
    val recentStations: List<Station> = emptyList(),
    val favoriteStations: List<Station> = emptyList(),
    val topStations: List<Station> = emptyList(),
    val searchResultCount: Int = 0,
    val selectedCountry: String? = null,
    val sortOption: SortOption = SortOption.POPULARITY,
    val isLoading: Boolean = false,
    val error: String? = null,
)
