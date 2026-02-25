package de.radiowave.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.radiowave.core.data.repository.RecentRepository
import de.radiowave.core.database.dao.StationDao
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val recentRepository: RecentRepository,
    private val stationDao: StationDao,
) : ViewModel() {

    fun clearHistory() {
        viewModelScope.launch {
            recentRepository.clearHistory()
        }
    }

    fun clearStationCache() {
        viewModelScope.launch {
            stationDao.deleteAllStations()
        }
    }
}
