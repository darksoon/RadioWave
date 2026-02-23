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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val stationRepository: StationRepository,
    private val favoriteRepository: FavoriteRepository,
    private val recentRepository: RecentRepository,
    private val playerManager: RadioPlayerManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    val playerState: StateFlow<de.radiowave.core.model.PlayerState> = playerManager.playerState

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init {
        Log.d("RadioWave", "HomeViewModel initialized - starting to load stations...")
        loadData()
        setupSearch()
    }

    @OptIn(FlowPreview::class)
    private fun setupSearch() {
        _searchQuery
            .debounce(500) // Wait 500ms after user stops typing
            .onEach { query ->
                if (query.isBlank()) {
                    loadData() // Load default data when search is cleared
                } else {
                    searchStations(query)
                }
            }
            .launchIn(viewModelScope)
    }

    private fun searchStations(query: String) {
        stationRepository.searchStations(query)
            .onStart {
                _uiState.update { it.copy(isLoading = true) }
            }
            .onEach { stations ->
                Log.d("RadioWave", "Search results: ${stations.size} stations for '$query'")
                _uiState.update {
                    it.copy(
                        topStations = stations.take(20),
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

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    private fun loadData() {
        combine(
            recentRepository.getRecentStations(limit = 10),
            favoriteRepository.getFavorites(),
            stationRepository.getTopStations(),
        ) { recent, favorites, top ->
            Triple(recent, favorites.take(6), top.take(10))
        }
            .onStart {
                Log.d("RadioWave", "Starting to load data...")
                _uiState.update { it.copy(isLoading = true) }
            }
            .onEach { (recent, favorites, top) ->
                Log.d("RadioWave", "Data loaded successfully: ${recent.size} recent, ${favorites.size} favorites, ${top.size} top stations")
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
        viewModelScope.launch {
            playerManager.playStation(station)
        }
    }

    fun togglePlayPause() {
        playerManager.togglePlayPause()
    }
}
