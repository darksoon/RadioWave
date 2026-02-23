package de.radiowave.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.radiowave.core.data.repository.FavoriteRepository
import de.radiowave.core.data.repository.RecentRepository
import de.radiowave.core.data.repository.StationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val stationRepository: StationRepository,
    private val favoriteRepository: FavoriteRepository,
    private val recentRepository: RecentRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadData()
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
                _uiState.update { it.copy(isLoading = true) }
            }
            .onEach { (recent, favorites, top) ->
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
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = error.message ?: "Unknown error",
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun refresh() {
        loadData()
    }
}
