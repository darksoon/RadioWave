// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.feature.home

import de.darksoon.radiowave.core.model.Station

enum class SortOption {
    POPULARITY,
    NAME,
    COUNTRY,
}

data class HomeUiState(
    val recentStations: List<Station> = emptyList(),
    val favoriteStations: List<Station> = emptyList(),
    val topStations: List<Station> = emptyList(),
    val nearbyStations: List<Station> = emptyList(),
    val isNearbyLoading: Boolean = false,
    val searchResultCount: Int = 0,
    val selectedCountry: String? = null,
    val sortOption: SortOption = SortOption.POPULARITY,
    val isLoading: Boolean = false,
    val error: String? = null,
)

