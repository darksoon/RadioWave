// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.feature.browse

import de.darksoon.radiowave.core.model.Station

/** Sort order for browse search results. */
enum class SortOption {
    POPULARITY,
    NAME,
    COUNTRY,
}

data class BrowseUiState(
    val results: List<Station> = emptyList(),
    val searchResultCount: Int = 0,
    val sortOption: SortOption = SortOption.POPULARITY,
    val selectedCountry: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
)
