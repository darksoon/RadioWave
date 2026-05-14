// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.feature.home

import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import de.darksoon.radiowave.core.data.repository.AppSettingsState
import de.darksoon.radiowave.core.data.repository.FavoriteRepository
import de.darksoon.radiowave.feature.home.R
import de.darksoon.radiowave.core.data.repository.RecentRepository
import de.darksoon.radiowave.core.data.repository.SettingsRepository
import de.darksoon.radiowave.core.data.repository.StationRepository
import de.darksoon.radiowave.core.model.Station
import de.darksoon.radiowave.core.player.RadioPlayerManager
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
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min
import java.util.Locale
import android.os.SystemClock

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val stationRepository: StationRepository,
    private val favoriteRepository: FavoriteRepository,
    private val recentRepository: RecentRepository,
    private val playerManager: RadioPlayerManager,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private val playbackHistory = ArrayDeque<Station>()
    private val maxPlaybackHistorySize = 40
    private var dataLoadJob: Job? = null
    private var lastRefreshAtElapsedMs = 0L
    private val minRefreshIntervalMs = 5_000L
    private val isDebuggable: Boolean by lazy {
        (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    val playerState: StateFlow<de.darksoon.radiowave.core.model.PlayerState> = playerManager.playerState

    // Derive favorite IDs directly from the favorites repository.
    val favoriteStationIds: StateFlow<Set<String>> = favoriteRepository.getFavorites()
        .map { favs -> favs.mapTo(HashSet<String>(favs.size)) { it.uuid } as Set<String> }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    private val _similarStations = MutableStateFlow<List<Station>>(emptyList())
    val similarStations: StateFlow<List<Station>> = _similarStations.asStateFlow()

    /**
     * Live snapshot of all app settings. Consumed by the shell composable in MainActivity
     * for theme/dynamicColors/miniplayer/keep-screen-on/onboarding decisions, replacing
     * the previous raw-SharedPreferences listeners.
     */
    val appSettings: StateFlow<AppSettingsState> = settingsRepository.data
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettingsState.DEFAULTS)

    /** Suspend setter used by the onboarding-dialog completion handler in MainActivity. */
    fun setFirstRunOnboardingDone() {
        viewModelScope.launch { settingsRepository.setFirstRunOnboardingDone(true) }
    }

    init {
        if (isDebuggable) Log.d("RadioWave", "HomeViewModel initialized - starting to load stations...")
        loadData()
    }


    private fun loadData() {
        dataLoadJob?.cancel()
        dataLoadJob = combine(
            recentRepository.getRecentStations(limit = 10),
            favoriteRepository.getFavorites(),
            stationRepository.getTopStations(),
        ) { recent, favorites, top ->
            Triple(recent, favorites.take(6), top.take(30))
        }
            .onStart {
                if (isDebuggable) Log.d("RadioWave", "Starting to load data...")
                _uiState.update { it.copy(isLoading = true) }
            }
            .onEach { (recent, favorites, top) ->
                if (isDebuggable) Log.d("RadioWave", "Data loaded: ${recent.size} recent, ${favorites.size} fav, ${top.size} top")
                _uiState.update {
                    it.copy(
                        recentStations = recent,
                        favoriteStations = favorites,
                        topStations = top,
                        isLoading = false,
                        error = null,
                    )
                }
            }
            .catch { error ->
                if (isDebuggable) Log.e("RadioWave", "API error: ${error.message}", error)
                val errorMessage = when {
                    error.message?.contains("UnknownHostException") == true ||
                        error.message?.contains("Unable to resolve host") == true ->
                        context.getString(R.string.home_error_no_connection)
                    error.message?.contains("timeout") == true ->
                        context.getString(R.string.home_error_timeout)
                    else -> context.getString(R.string.home_error_api, error.message ?: context.getString(R.string.home_error_unknown))
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
        val now = SystemClock.elapsedRealtime()
        if (now - lastRefreshAtElapsedMs < minRefreshIntervalMs) return
        lastRefreshAtElapsedMs = now
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

    fun stopPlayback() {
        playerManager.stop()
    }

    fun loadSimilarStationsFor(station: Station) {
        viewModelScope.launch {
            _similarStations.value = stationRepository.getSimilarStations(station)
        }
    }

    fun clearSimilarStations() {
        _similarStations.value = emptyList()
    }
}

