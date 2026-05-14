// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.feature.browse

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import de.darksoon.radiowave.core.data.repository.FavoriteRepository
import de.darksoon.radiowave.core.data.repository.RecentRepository
import de.darksoon.radiowave.core.data.repository.SettingsRepository
import de.darksoon.radiowave.core.data.repository.StationRepository
import de.darksoon.radiowave.core.model.Station
import de.darksoon.radiowave.core.player.RadioPlayerManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min

/**
 * Drives the Browse screen — search, country filter, sort.
 * Split out of HomeViewModel so Browse no longer mutates Home's topStations.
 *
 * Shared cross-screen data (favoriteStationIds, playStation, toggleFavorite) is
 * sourced directly from repositories / RadioPlayerManager so the two ViewModels
 * stay independent.
 */
@HiltViewModel
class BrowseViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    savedState: SavedStateHandle,
    private val stationRepository: StationRepository,
    private val favoriteRepository: FavoriteRepository,
    private val recentRepository: RecentRepository,
    private val playerManager: RadioPlayerManager,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private var resultsJob: Job? = null
    private var lastRefreshAtElapsedMs = 0L
    private val minRefreshIntervalMs = 5_000L
    private val isDebuggable: Boolean by lazy {
        (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }
    private val localeCountryCode = Locale.getDefault().country.lowercase()
    private val localeLanguageCode = Locale.getDefault().language.lowercase()

    private val _uiState = MutableStateFlow(BrowseUiState())
    val uiState: StateFlow<BrowseUiState> = _uiState.asStateFlow()

    // Initial query from navigation argument (e.g. tap a genre on Home → "techno").
    private val _searchQuery = MutableStateFlow(savedState.get<String>("q").orEmpty())
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCountry = MutableStateFlow<String?>(null)
    val selectedCountry: StateFlow<String?> = _selectedCountry.asStateFlow()

    val playerState: StateFlow<de.darksoon.radiowave.core.model.PlayerState> = playerManager.playerState

    /** Live flag for showing HTTP-only streams — drives the visible-stations filter. */
    val showInsecureStreams: StateFlow<Boolean> = settingsRepository.showInsecureStreams
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    // Read directly from the favorites repository — no cross-VM state sharing required.
    val favoriteStationIds: StateFlow<Set<String>> = favoriteRepository.getFavorites()
        .map { favs -> favs.mapTo(HashSet<String>(favs.size)) { it.uuid } as Set<String> }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    // Recent station UUIDs — used as a ranking boost. Sourced from repo, not from a cross-VM uiState.
    private val recentBoostIds: StateFlow<Set<String>> = recentRepository.getRecentStations(limit = 20)
        .map { recents -> recents.mapTo(HashSet<String>(recents.size)) { it.uuid } as Set<String> }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    init {
        if (isDebuggable) Log.d("RadioWave", "BrowseViewModel initialized (q='${_searchQuery.value}')")
        loadInitial()
        setupSearch()
    }

    private fun loadInitial() {
        // If no search/country active, show top stations as the default browse landing.
        if (_searchQuery.value.isBlank() && _selectedCountry.value == null) {
            loadTopStations()
        } else if (_selectedCountry.value != null) {
            loadStationsByCountry(_selectedCountry.value!!, _searchQuery.value)
        } else {
            searchStations(_searchQuery.value)
        }
    }

    @OptIn(FlowPreview::class)
    private fun setupSearch() {
        combine(
            _searchQuery.debounce(250),
            _selectedCountry,
        ) { query, country -> query to country }
            .drop(1)
            .onEach { (query, country) ->
                if (query.isBlank() && country == null) {
                    loadTopStations()
                } else if (country != null) {
                    loadStationsByCountry(country, query)
                } else {
                    searchStations(query)
                }
            }
            .launchIn(viewModelScope)
    }

    private fun loadTopStations() {
        resultsJob?.cancel()
        resultsJob = stationRepository.getTopStations()
            .onStart { _uiState.update { it.copy(isLoading = true) } }
            .onEach { stations ->
                val sorted = withContext(Dispatchers.Default) {
                    rankAndSortStations(stations.take(60), query = "", selectedCountryCode = null)
                }
                _uiState.update {
                    it.copy(
                        results = sorted.take(50),
                        searchResultCount = sorted.size,
                        isLoading = false,
                        error = null,
                    )
                }
            }
            .catch { error ->
                if (isDebuggable) Log.e("RadioWave", "Top stations error: ${error.message}", error)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = context.getString(R.string.browse_error_load, error.message ?: ""),
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    private fun loadStationsByCountry(countryCode: String, query: String) {
        resultsJob?.cancel()
        resultsJob = stationRepository.getStationsByCountry(countryCode)
            .onStart { _uiState.update { it.copy(isLoading = true) } }
            .onEach { stations ->
                val sorted = withContext(Dispatchers.Default) {
                    val filtered = if (query.isNotBlank()) {
                        stations.filter { station ->
                            station.name.contains(query, ignoreCase = true) ||
                                fuzzyMatchScore(query, station.name) > 0
                        }
                    } else {
                        stations
                    }
                    rankAndSortStations(filtered, query, countryCode)
                }
                _uiState.update {
                    it.copy(
                        results = sorted.take(50),
                        searchResultCount = sorted.size,
                        isLoading = false,
                        error = null,
                    )
                }
            }
            .catch { error ->
                if (isDebuggable) Log.e("RadioWave", "Country filter error: ${error.message}", error)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = context.getString(R.string.browse_error_load, error.message ?: ""),
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    private fun searchStations(query: String) {
        resultsJob?.cancel()
        resultsJob = stationRepository.searchStations(query)
            .onStart { _uiState.update { it.copy(isLoading = true) } }
            .onEach { stations ->
                val sorted = withContext(Dispatchers.Default) {
                    rankAndSortStations(stations, query, selectedCountryCode = null)
                }
                _uiState.update {
                    it.copy(
                        results = sorted.take(50),
                        searchResultCount = sorted.size,
                        isLoading = false,
                        error = null,
                    )
                }
            }
            .catch { error ->
                if (isDebuggable) Log.e("RadioWave", "Search error: ${error.message}", error)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = context.getString(R.string.browse_error_search, error.message ?: ""),
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    // ─── Public actions ───────────────────────────────────────────────────────

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onCountrySelected(countryCode: String?) {
        _selectedCountry.value = countryCode
        _uiState.update { it.copy(selectedCountry = countryCode) }
    }

    fun onSortOptionChanged(sortOption: SortOption) {
        _uiState.update { it.copy(sortOption = sortOption) }
        val current = _uiState.value.results
        if (current.isNotEmpty()) {
            viewModelScope.launch {
                val sorted = withContext(Dispatchers.Default) {
                    rankAndSortStations(current, _searchQuery.value, _selectedCountry.value)
                }
                _uiState.update { it.copy(results = sorted) }
            }
        }
    }

    fun refresh() {
        val now = SystemClock.elapsedRealtime()
        if (now - lastRefreshAtElapsedMs < minRefreshIntervalMs) return
        lastRefreshAtElapsedMs = now
        loadInitial()
    }

    fun playStation(station: Station) {
        viewModelScope.launch {
            recentRepository.addRecentStation(station)
            playerManager.playStation(station)
        }
    }

    fun toggleFavorite(station: Station) {
        viewModelScope.launch { favoriteRepository.toggleFavorite(station) }
    }

    // ─── Ranking helpers (extracted from HomeViewModel) ───────────────────────

    private fun rankAndSortStations(
        stations: List<Station>,
        query: String,
        selectedCountryCode: String?,
    ): List<Station> {
        if (stations.isEmpty()) return stations
        val recent = recentBoostIds.value
        val normalizedQuery = query.trim().lowercase()
        val currentSort = _uiState.value.sortOption
        return stations
            .map { station ->
                val score = rankingScore(station, normalizedQuery, selectedCountryCode, recent)
                station to score
            }
            .sortedWith(
                compareByDescending<Pair<Station, Int>> { it.second }
                    .thenComparator { a, b ->
                        when (currentSort) {
                            SortOption.POPULARITY -> b.first.clickCount.compareTo(a.first.clickCount)
                            SortOption.NAME -> a.first.name.lowercase().compareTo(b.first.name.lowercase())
                            SortOption.COUNTRY -> (a.first.country ?: "zzz").lowercase()
                                .compareTo((b.first.country ?: "zzz").lowercase())
                        }
                    },
            )
            .map { it.first }
    }

    private fun rankingScore(
        station: Station,
        query: String,
        selectedCountryCode: String?,
        recentBoost: Set<String>,
    ): Int {
        var score = 0
        val stationCountryCode = station.countryCode?.lowercase().orEmpty()
        val stationLanguage = station.language?.lowercase().orEmpty()

        if (query.isNotBlank()) {
            score += fuzzyMatchScore(query, station.name)
            if (station.tags.any { tag -> fuzzyMatchScore(query, tag) > 0 }) score += 8
        }
        if (selectedCountryCode != null && stationCountryCode == selectedCountryCode.lowercase()) {
            score += 24
        } else if (localeCountryCode.isNotBlank() && stationCountryCode == localeCountryCode) {
            score += 14
        }
        if (localeLanguageCode.isNotBlank() && stationLanguage.contains(localeLanguageCode)) score += 10
        if (recentBoost.contains(station.uuid)) score += 9
        score += min(8, station.clickCount / 500)
        return score
    }

    private fun fuzzyMatchScore(query: String, candidate: String): Int {
        val q = query.trim().lowercase()
        val c = candidate.trim().lowercase()
        if (q.isBlank() || c.isBlank()) return 0
        if (c == q) return 80
        if (c.startsWith(q)) return 56
        if (c.contains(q)) return 42

        val queryTokens = q.split(" ").filter { it.isNotBlank() }
        var tokenScore = 0
        queryTokens.forEach { token ->
            if (c.startsWith(token)) tokenScore += 10
            else if (c.contains(token)) tokenScore += 6
        }
        val subsequenceScore = if (isSubsequence(q, c)) 12 else 0
        val maxLen = max(q.length, c.length)
        val editPenaltyLimit = 4
        val distance = limitedLevenshtein(q, c, editPenaltyLimit + 1)
        val distanceScore = if (distance <= editPenaltyLimit) max(0, 20 - distance * 4) else 0
        val normalizedLengthBonus = max(0, 8 - (maxLen - q.length))
        return tokenScore + subsequenceScore + distanceScore + normalizedLengthBonus
    }

    private fun isSubsequence(query: String, candidate: String): Boolean {
        var qIndex = 0; var cIndex = 0
        while (qIndex < query.length && cIndex < candidate.length) {
            if (query[qIndex] == candidate[cIndex]) qIndex++
            cIndex++
        }
        return qIndex == query.length
    }

    private fun limitedLevenshtein(a: String, b: String, limit: Int): Int {
        if (kotlin.math.abs(a.length - b.length) > limit) return limit
        val prev = IntArray(b.length + 1) { it }
        val curr = IntArray(b.length + 1)
        for (i in 1..a.length) {
            curr[0] = i
            var minInRow = curr[0]
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                curr[j] = min(min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost)
                minInRow = min(minInRow, curr[j])
            }
            if (minInRow > limit) return limit
            for (j in prev.indices) prev[j] = curr[j]
        }
        return prev[b.length]
    }
}
