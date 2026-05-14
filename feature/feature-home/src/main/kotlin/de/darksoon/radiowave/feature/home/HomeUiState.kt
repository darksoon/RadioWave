// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.feature.home

import de.darksoon.radiowave.core.model.Station

data class HomeUiState(
    val recentStations: List<Station> = emptyList(),
    val favoriteStations: List<Station> = emptyList(),
    val topStations: List<Station> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

