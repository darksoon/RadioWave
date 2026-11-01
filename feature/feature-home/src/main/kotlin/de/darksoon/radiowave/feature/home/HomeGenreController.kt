// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.feature.home

import de.darksoon.radiowave.core.data.repository.StationRepository
import de.darksoon.radiowave.core.model.collapseDuplicateStations
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Loads the stations for the genre selected on Home (radio-browser `bytag/<genre>` through
 * [StationRepository.getStationsByTag], i.e. the same offline-first cache as Browse).
 *
 * Re-selecting the genre that is already loaded (or loading) is a no-op unless
 * [select] is called with `forceRefresh = true` (pull-to-refresh, "Try again").
 * While a new genre loads, the previous genre's stations are not shown; when the same genre
 * is refreshed, its stations stay visible until the new result arrives.
 *
 * REA 95fcb33: p000/or1.java (+ nr1 Fall 0 Laden, ge1#m3863b Ergebnis, cf0 Fall 1 Fehler,
 * sw0 Fall 2 Aufbereitung). Klassenname geraten (REA-UNSICHER), Logik hoch.
 */
class HomeGenreController(
    private val stationRepository: StationRepository,
    private val scope: CoroutineScope,
    initialGenre: String,
    private val computeDispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    private val _state = MutableStateFlow(HomeGenreState())
    val state: StateFlow<HomeGenreState> = _state.asStateFlow()

    private var loadJob: Job? = null

    init {
        select(initialGenre.takeIf { it in SUPPORTED_GENRES } ?: DEFAULT_GENRE, forceRefresh = false)
    }

    fun select(genre: String, forceRefresh: Boolean) {
        val normalized = genre.lowercase(Locale.ROOT).takeIf { it in SUPPORTED_GENRES } ?: return
        val current = _state.value
        val sameGenre = normalized == current.selectedGenre
        if (!forceRefresh && sameGenre && loadJob?.isActive == true) return
        if (!forceRefresh && sameGenre && current.stations.isNotEmpty()) return

        loadJob?.cancel()
        val keptStations = if (sameGenre) current.stations else emptyList()
        _state.value = HomeGenreState(selectedGenre = normalized, stations = keptStations, isLoading = true)
        loadJob = scope.launch {
            stationRepository.getStationsByTag(normalized, forceRefresh)
                .catch {
                    val failed = _state.value
                    _state.value = HomeGenreState(failed.selectedGenre, failed.stations, isLoading = false, hasError = true)
                }
                .collect { stations ->
                    val cleaned = withContext(computeDispatcher) {
                        collapseDuplicateStations(stations, favoriteIds = emptySet())
                            .distinctBy { it.uuid }
                            .take(MAX_GENRE_STATIONS)
                    }
                    _state.value = HomeGenreState(selectedGenre = normalized, stations = cleaned, isLoading = false)
                }
        }
    }

    companion object {
        // Separate copy of HomeGenres.ALL in the handy build (or1.f15459g vs qr1.f17567a).
        val SUPPORTED_GENRES: List<String> = listOf("techno", "rock", "jazz", "chillout")
        const val DEFAULT_GENRE = "chillout"
        private const val MAX_GENRE_STATIONS = 12
    }
}
