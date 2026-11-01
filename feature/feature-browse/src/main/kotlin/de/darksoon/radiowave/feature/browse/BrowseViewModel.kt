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
import de.darksoon.radiowave.core.data.repository.OfflineFirstStationRepository
import de.darksoon.radiowave.core.data.repository.RecentRepository
import de.darksoon.radiowave.core.data.repository.SettingsRepository
import de.darksoon.radiowave.core.data.repository.StationRepository
import de.darksoon.radiowave.core.model.Station
import de.darksoon.radiowave.core.model.collapseDuplicateStations
import de.darksoon.radiowave.core.model.duplicateKey
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
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
    // Decode the URL-encoded value MainActivity wrote — otherwise multi-word genres
    // like "hip hop" or "drum & bass" arrive as "hip+hop" / "drum+%26+bass" and the
    // station search returns 0 results.
    private val _searchQuery = MutableStateFlow(
        runCatching {
            java.net.URLDecoder.decode(savedState.get<String>("q").orEmpty(), "UTF-8")
        }.getOrDefault(savedState.get<String>("q").orEmpty())
    )
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCountry = MutableStateFlow<String?>(null)
    val selectedCountry: StateFlow<String?> = _selectedCountry.asStateFlow()

    // REA 95fcb33 (p000/m00.java f12705h/f12706i): the quick-genre selection moved from the
    // screen (remember) into the ViewModel. A selected genre is loaded by TAG instead of being
    // typed into the search field.
    private val _selectedGenre = MutableStateFlow<String?>(null)
    val selectedGenre: StateFlow<String?> = _selectedGenre.asStateFlow()

    val playerState: StateFlow<de.darksoon.radiowave.core.model.PlayerState> = playerManager.playerState

    /** Live flag for showing HTTP-only streams — drives the visible-stations filter. */
    val showInsecureStreams: StateFlow<Boolean> = settingsRepository.showInsecureStreams
        .stateIn(viewModelScope, SharingStarted.Eagerly, settingsRepository.initialSnapshotBlocking().showInsecureStreams)

    /** False while the Radio Browser directory is unreachable — drives the offline banner. */
    val directoryAvailable: StateFlow<Boolean> =
        (stationRepository as? OfflineFirstStationRepository)?.directoryAvailable
            ?: MutableStateFlow(true)

    /** Whether the local directory mirror is enabled — hides the search hint once it is. */
    val stationMirrorEnabled: StateFlow<Boolean> = settingsRepository.data
        .map { it.stationMirrorEnabled }
        .distinctUntilChanged()
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            settingsRepository.initialSnapshotBlocking().stationMirrorEnabled,
        )

    // Read directly from the favorites repository — no cross-VM state sharing required.
    val favoriteStationIds: StateFlow<Set<String>> = favoriteRepository.getFavorites()
        .map { favs -> favs.mapTo(HashSet<String>(favs.size)) { it.uuid } as Set<String> }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    // REA 95fcb33 (p000/m00.java f12722y, C0648qn Fall 2): duplicate keys of all favorites, so a
    // directory variant of a favourite station (other stream URL/bitrate) also shows the heart.
    val favoriteDuplicateKeys: StateFlow<Set<String>> = favoriteRepository.getFavorites()
        .map { favs -> favs.mapNotNull { it.duplicateKey() }.toSet() }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    // Recent station UUIDs — used as a ranking boost. Sourced from repo, not from a cross-VM uiState.
    private val recentBoostIds: StateFlow<Set<String>> = recentRepository.getRecentStations(limit = 20)
        .map { recents -> recents.mapTo(HashSet<String>(recents.size)) { it.uuid } as Set<String> }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    init {
        if (isDebuggable) Log.d("RadioWave", "BrowseViewModel initialized (q='${_searchQuery.value}')")
        setupSearch()
    }

    /** [forceRefresh]: explicit user refresh — bypass the repository's TTL cache once. */
    private fun loadInitial(forceRefresh: Boolean = false) {
        // REA 95fcb33 (p000/m00.java#m6587i): a selected quick genre wins over search/country.
        val genre = _selectedGenre.value
        if (genre != null) {
            loadStationsByGenre(genre, forceRefresh)
        } else if (_searchQuery.value.isBlank() && _selectedCountry.value == null) {
            // If no search/country active, show top stations as the default browse landing.
            loadTopStations(forceRefresh)
        } else if (_selectedCountry.value != null) {
            loadStationsByCountry(_selectedCountry.value!!, _searchQuery.value, forceRefresh)
        } else {
            searchStations(_searchQuery.value, forceRefresh)
        }
    }

    // REA 95fcb33 (p000/m00.java Konstruktor, Ende): query, country AND genre are combined and
    // debounced together; the first emission performs the initial load (the explicit
    // loadInitial() + drop(1) of v1.0.19 is gone).
    // REA-UNSICHER: Reihenfolge debounce → distinctUntilChanged aus der Operator-Verschachtelung
    // (ym0/td1 = debounce(250), n10.m6944t = distinctUntilChanged) abgeleitet.
    @OptIn(FlowPreview::class)
    private fun setupSearch() {
        combine(
            _searchQuery,
            _selectedCountry,
            _selectedGenre,
        ) { query, country, genre -> Triple(query, country, genre) }
            .debounce(250)
            .distinctUntilChanged()
            .onEach { loadInitial(forceRefresh = false) }
            .launchIn(viewModelScope)
    }

    /** Favourite uuids right now — used to prefer the favourite variant when collapsing. */
    private suspend fun currentFavoriteIds(): Set<String> = withContext(Dispatchers.IO) {
        favoriteRepository.getFavorites().first().mapTo(HashSet()) { it.uuid }
    }

    // REA 95fcb33 (p000/m00.java#m6587i Zweig 1, c00 Fall 0, b00 Fall 0, a00 Fall 1):
    // genre = Radio-Browser tag, optionally narrowed to the selected country.
    private fun loadStationsByGenre(genre: String, forceRefresh: Boolean = false) {
        resultsJob?.cancel()
        resultsJob = stationRepository.getStationsByTag(genre, forceRefresh)
            .onStart { _uiState.update { it.copy(isLoading = true) } }
            .onEach { stations ->
                val favoriteIds = currentFavoriteIds()
                val country = _selectedCountry.value
                val sorted = withContext(Dispatchers.Default) {
                    val filtered = if (country != null) {
                        stations.filter { it.countryCode.equals(country, ignoreCase = true) }
                    } else {
                        stations
                    }
                    collapseDuplicateStations(
                        rankAndSortStations(filtered, query = "", selectedCountryCode = country),
                        favoriteIds,
                    )
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
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = context.getString(R.string.browse_error_load, error.message ?: ""),
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    private fun loadTopStations(forceRefresh: Boolean = false) {
        resultsJob?.cancel()
        resultsJob = stationRepository.getTopStations(forceRefresh)
            .onStart { _uiState.update { it.copy(isLoading = true) } }
            .onEach { stations ->
                // REA 95fcb33 (c00 Fall 1 → d00 Fall 0): variants collapsed, favourite preferred.
                val favoriteIds = currentFavoriteIds()
                val sorted = withContext(Dispatchers.Default) {
                    collapseDuplicateStations(
                        rankAndSortStations(stations.take(60), query = "", selectedCountryCode = null),
                        favoriteIds,
                    )
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

    private fun loadStationsByCountry(countryCode: String, query: String, forceRefresh: Boolean = false) {
        resultsJob?.cancel()
        resultsJob = stationRepository.getStationsByCountry(countryCode, forceRefresh)
            .onStart { _uiState.update { it.copy(isLoading = true) } }
            .onEach { stations ->
                // REA 95fcb33 (C0158dc Fall 2 → C0474ly Fall 1): + collapseDuplicateStations.
                val favoriteIds = currentFavoriteIds()
                val sorted = withContext(Dispatchers.Default) {
                    val filtered = if (query.isNotBlank()) {
                        stations.filter { station ->
                            station.name.contains(query, ignoreCase = true) ||
                                fuzzyMatchScore(query, station.name) > 0
                        }
                    } else {
                        stations
                    }
                    collapseDuplicateStations(rankAndSortStations(filtered, query, countryCode), favoriteIds)
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

    private fun searchStations(query: String, forceRefresh: Boolean = false) {
        resultsJob?.cancel()
        resultsJob = stationRepository.searchStations(query, forceRefresh)
            .onStart { _uiState.update { it.copy(isLoading = true) } }
            .onEach { stations ->
                // REA 95fcb33 (C0698s Fall 6 → b00 Fall 1): + collapseDuplicateStations.
                val favoriteIds = currentFavoriteIds()
                val sorted = withContext(Dispatchers.Default) {
                    collapseDuplicateStations(
                        rankAndSortStations(stations, query, selectedCountryCode = null),
                        favoriteIds,
                    )
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
        // REA 95fcb33 (AbstractC0920xz az Fall 0): typing leaves genre mode.
        _selectedGenre.value = null
        _searchQuery.value = query
    }

    /**
     * Selects (or clears with null/blank) a quick genre. Clears the search text.
     * REA 95fcb33 (p000/m00.java#m6588j, smali geprüft). REA-UNSICHER: Name geraten.
     */
    fun selectGenre(genre: String?) {
        _selectedGenre.value = genre?.trim()?.lowercase(Locale.ROOT)?.takeIf { it.isNotEmpty() }
        _searchQuery.value = ""
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
        loadInitial(forceRefresh = true)
    }

    fun playStation(station: Station) {
        viewModelScope.launch {
            recentRepository.addRecentStation(station)
            playerManager.playStation(station)
        }
    }

    fun toggleFavorite(station: Station) {
        // REA 95fcb33 (p000/l00.java Fall 0): if any favourite is a variant of this station
        // (same duplicateKey), remove all of those variants; otherwise toggle as before.
        viewModelScope.launch {
            val key = station.duplicateKey()
            val matchingFavorites = if (key == null) {
                emptyList()
            } else {
                favoriteRepository.getFavorites().first().filter { it.duplicateKey() == key }
            }
            if (matchingFavorites.isEmpty()) {
                favoriteRepository.toggleFavorite(station)
            } else {
                matchingFavorites.forEach { favoriteRepository.removeFavorite(it.uuid) }
            }
        }
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
                // An explicit NAME/COUNTRY choice is the user's PRIMARY intent (alphabetical /
                // by country); the relevance score is only the tiebreaker. Previously the score
                // always dominated, so "Sort by Name/Country" merely reordered equal-score ties
                // and the requested ordering was effectively ignored. POPULARITY keeps the
                // score-first ordering (relevance already encodes popularity + recent boost).
                when (currentSort) {
                    SortOption.NAME ->
                        compareBy<Pair<Station, Int>> { it.first.name.lowercase() }
                            .thenByDescending { it.second }
                    SortOption.COUNTRY ->
                        compareBy<Pair<Station, Int>> { (it.first.country ?: "zzz").lowercase() }
                            .thenByDescending { it.second }
                    SortOption.POPULARITY ->
                        compareByDescending<Pair<Station, Int>> { it.second }
                            .thenByDescending { it.first.clickCount }
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
