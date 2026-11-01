// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.feature.home

import de.darksoon.radiowave.core.model.Station

// REA 95fcb33: unverändert gegenüber v1.0.19 (p000/ms1.java, toString "HomeUiState(...)").
data class HomeUiState(
    val recentStations: List<Station> = emptyList(),
    val favoriteStations: List<Station> = emptyList(),
    val topStations: List<Station> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

/**
 * The genre strip on Home: which genre tile is selected and its (deduplicated) stations.
 *
 * REA 95fcb33: p000/pr1.java, toString "HomeGenreState(selectedGenre=, stations=, isLoading=,
 * hasError=)". Defaults aus dem synthetischen Konstruktor: "chillout", leer, isLoading = true.
 */
data class HomeGenreState(
    val selectedGenre: String = HomeGenreController.DEFAULT_GENRE,
    val stations: List<Station> = emptyList(),
    val isLoading: Boolean = true,
    val hasError: Boolean = false,
)
