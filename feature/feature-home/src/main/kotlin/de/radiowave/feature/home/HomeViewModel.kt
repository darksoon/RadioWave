package de.radiowave.feature.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.radiowave.core.data.repository.FavoriteRepository
import de.radiowave.core.data.repository.RecentRepository
import de.radiowave.core.data.repository.StationRepository
import de.radiowave.core.model.Station
import de.radiowave.core.player.RadioPlayerManager
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min
import java.util.Locale

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val stationRepository: StationRepository,
    private val favoriteRepository: FavoriteRepository,
    private val recentRepository: RecentRepository,
    private val playerManager: RadioPlayerManager,
) : ViewModel() {
    private val playbackHistory = ArrayDeque<Station>()
    private val maxPlaybackHistorySize = 40
    private var dataLoadJob: Job? = null
    private var browseResultsJob: Job? = null
    private val localeCountryCode = Locale.getDefault().country.lowercase()
    private val localeLanguageCode = Locale.getDefault().language.lowercase()

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    val playerState: StateFlow<de.radiowave.core.model.PlayerState> = playerManager.playerState

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCountry = MutableStateFlow<String?>(null)
    val selectedCountry: StateFlow<String?> = _selectedCountry.asStateFlow()

    init {
        Log.d("RadioWave", "HomeViewModel initialized - starting to load stations...")
        loadData()
        setupSearch()
    }

    @OptIn(FlowPreview::class)
    private fun setupSearch() {
        combine(
            _searchQuery.debounce(500),
            _selectedCountry,
        ) { query, country -> query to country }
            .drop(1)
            .onEach { (query, country) ->
                if (query.isBlank() && country == null) {
                    loadData()
                } else if (country != null) {
                    loadStationsByCountry(country, query)
                } else {
                    searchStations(query)
                }
            }
            .launchIn(viewModelScope)
    }

    private fun loadStationsByCountry(countryCode: String, query: String) {
        browseResultsJob?.cancel()
        browseResultsJob = stationRepository.getStationsByCountry(countryCode)
            .onStart {
                _uiState.update { it.copy(isLoading = true) }
            }
            .onEach { stations ->
                val filtered = if (query.isNotBlank()) {
                    stations.filter { station ->
                        station.name.contains(query, ignoreCase = true) ||
                            fuzzyMatchScore(query, station.name) > 0
                    }
                } else {
                    stations
                }
                val sorted = rankAndSortStations(
                    stations = filtered,
                    query = query,
                    selectedCountryCode = countryCode,
                )
                Log.d("RadioWave", "Country filter: ${sorted.size} stations for '$countryCode'")
                _uiState.update {
                    it.copy(
                        topStations = sorted.take(50),
                        searchResultCount = sorted.size,
                        isLoading = false,
                        error = null,
                    )
                }
            }
            .catch { error ->
                Log.e("RadioWave", "Country filter error: ${error.message}", error)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Fehler beim Laden: ${error.message}"
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    private fun searchStations(query: String) {
        browseResultsJob?.cancel()
        browseResultsJob = stationRepository.searchStations(query)
            .onStart {
                _uiState.update { it.copy(isLoading = true) }
            }
            .onEach { stations ->
                val baseResults = if (stations.isEmpty() && query.isNotBlank()) {
                    val localPool = (
                        _uiState.value.topStations +
                            _uiState.value.recentStations +
                            _uiState.value.favoriteStations
                        ).distinctBy { station -> station.uuid }
                    localPool.filter { station ->
                        fuzzyMatchScore(query, station.name) > 0 ||
                            station.tags.any { tag -> fuzzyMatchScore(query, tag) > 0 }
                    }
                } else {
                    stations
                }
                val sorted = rankAndSortStations(
                    stations = baseResults,
                    query = query,
                    selectedCountryCode = null,
                )
                Log.d("RadioWave", "Search results: ${sorted.size} stations for '$query'")
                _uiState.update {
                    it.copy(
                        topStations = sorted.take(50),
                        searchResultCount = sorted.size,
                        isLoading = false,
                        error = null,
                    )
                }
            }
            .catch { error ->
                Log.e("RadioWave", "Search error: ${error.message}", error)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Fehler bei der Suche: ${error.message}"
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    private fun rankAndSortStations(
        stations: List<Station>,
        query: String,
        selectedCountryCode: String?,
    ): List<Station> {
        if (stations.isEmpty()) return stations
        val recentBoostIds = _uiState.value.recentStations.map { it.uuid }.toSet()
        val normalizedQuery = query.trim().lowercase()
        val currentSort = _uiState.value.sortOption
        return stations
            .map { station ->
                val score = rankingScore(
                    station = station,
                    query = normalizedQuery,
                    selectedCountryCode = selectedCountryCode,
                    recentBoostIds = recentBoostIds,
                )
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
        recentBoostIds: Set<String>,
    ): Int {
        var score = 0
        val stationName = station.name.lowercase()
        val stationCountryCode = station.countryCode?.lowercase().orEmpty()
        val stationLanguage = station.language?.lowercase().orEmpty()

        if (query.isNotBlank()) {
            score += fuzzyMatchScore(query, station.name)
            if (station.tags.any { tag -> fuzzyMatchScore(query, tag) > 0 }) {
                score += 8
            }
        }

        if (selectedCountryCode != null && stationCountryCode == selectedCountryCode.lowercase()) {
            score += 24
        } else if (localeCountryCode.isNotBlank() && stationCountryCode == localeCountryCode) {
            score += 14
        }

        if (localeLanguageCode.isNotBlank() && stationLanguage.contains(localeLanguageCode)) {
            score += 10
        }

        if (recentBoostIds.contains(station.uuid)) {
            score += 9
        }

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
        val distanceScore = if (distance <= editPenaltyLimit) {
            max(0, 20 - distance * 4)
        } else {
            0
        }

        val normalizedLengthBonus = max(0, 8 - (maxLen - q.length))
        return tokenScore + subsequenceScore + distanceScore + normalizedLengthBonus
    }

    private fun isSubsequence(query: String, candidate: String): Boolean {
        var qIndex = 0
        var cIndex = 0
        while (qIndex < query.length && cIndex < candidate.length) {
            if (query[qIndex] == candidate[cIndex]) {
                qIndex++
            }
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
                curr[j] = min(
                    min(curr[j - 1] + 1, prev[j] + 1),
                    prev[j - 1] + cost,
                )
                minInRow = min(minInRow, curr[j])
            }
            if (minInRow > limit) return limit
            for (j in prev.indices) {
                prev[j] = curr[j]
            }
        }
        return prev[b.length]
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onCountrySelected(countryCode: String?) {
        _selectedCountry.value = countryCode
        _uiState.update { it.copy(selectedCountry = countryCode) }
    }

    fun onSortOptionChanged(sortOption: SortOption) {
        _uiState.update { it.copy(sortOption = sortOption) }
        val currentStations = _uiState.value.topStations
        if (currentStations.isNotEmpty()) {
            val sorted = rankAndSortStations(
                stations = currentStations,
                query = _searchQuery.value,
                selectedCountryCode = _selectedCountry.value,
            )
            _uiState.update { it.copy(topStations = sorted) }
        }
    }

    private fun loadData() {
        browseResultsJob?.cancel()
        dataLoadJob?.cancel()
        dataLoadJob = combine(
            recentRepository.getRecentStations(limit = 10),
            favoriteRepository.getFavorites(),
            stationRepository.getTopStations(),
        ) { recent, favorites, top ->
            Triple(recent, favorites.take(6), top.take(30))
        }
            .onStart {
                Log.d("RadioWave", "Starting to load data...")
                _uiState.update { it.copy(isLoading = true) }
            }
            .onEach { (recent, favorites, top) ->
                Log.d("RadioWave", "Data loaded successfully: ${recent.size} recent, ${favorites.size} favorites, ${top.size} top stations")
                val hasActiveFilter = _searchQuery.value.isNotBlank() || _selectedCountry.value != null
                _uiState.update {
                    if (hasActiveFilter) {
                        // Keep current browse/search result list stable while favorites/recents update.
                        it.copy(
                            recentStations = recent,
                            favoriteStations = favorites,
                            isLoading = false,
                            error = null,
                        )
                    } else {
                        it.copy(
                            recentStations = recent,
                            favoriteStations = favorites,
                            topStations = top,
                            isLoading = false,
                            error = null,
                        )
                    }
                }
            }
            .catch { error ->
                Log.e("RadioWave", "API Fehler: ${error.message}", error)
                val errorMessage = when {
                    error.message?.contains("UnknownHostException") == true -> 
                        "DNS Fehler: Server nicht erreichbar. Bitte später erneut versuchen."
                    error.message?.contains("timeout") == true -> 
                        "Zeitüberschreitung beim Laden. Bitte versuche es erneut."
                    error.message?.contains("Unable to resolve host") == true ->
                        "Keine Internetverbindung oder DNS-Problem."
                    else -> "API Fehler: ${error.message ?: "Unbekannter Fehler"}"
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = errorMessage,
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun refresh() {
        loadData()
    }

    fun playStation(station: Station) {
        playStationInternal(station, addCurrentToHistory = true)
    }

    fun playPreviousStation() {
        val previousStation = playbackHistory.removeLastOrNull() ?: return
        playStationInternal(previousStation, addCurrentToHistory = false)
    }

    fun playRandomStation() {
        val state = _uiState.value
        val currentUuid = playerState.value.currentStation?.uuid
        val pool = (state.favoriteStations + state.recentStations + state.topStations)
            .distinctBy { it.uuid }
            .filter { it.streamUrl.isNotBlank() }
        if (pool.isEmpty()) return

        val candidates = pool.filterNot { it.uuid == currentUuid }
        val station = if (candidates.isNotEmpty()) {
            candidates.random()
        } else {
            pool.random()
        }
        playStationInternal(station, addCurrentToHistory = true)
    }

    private fun playStationInternal(
        station: Station,
        addCurrentToHistory: Boolean,
    ) {
        viewModelScope.launch {
            if (addCurrentToHistory) {
                rememberCurrentStationForBackNavigation(nextStation = station)
            }
            recentRepository.addRecentStation(station)
            playerManager.playStation(station)
        }
    }

    private fun rememberCurrentStationForBackNavigation(nextStation: Station) {
        val currentStation = playerState.value.currentStation ?: return
        if (currentStation.uuid == nextStation.uuid) return
        if (playbackHistory.lastOrNull()?.uuid == currentStation.uuid) return

        playbackHistory.addLast(currentStation)
        while (playbackHistory.size > maxPlaybackHistorySize) {
            playbackHistory.removeFirst()
        }
    }

    fun toggleFavorite(station: Station) {
        viewModelScope.launch {
            favoriteRepository.toggleFavorite(station)
        }
    }

    fun togglePlayPause() {
        playerManager.togglePlayPause()
    }

    fun toggleMute() {
        playerManager.toggleMute()
    }
}
