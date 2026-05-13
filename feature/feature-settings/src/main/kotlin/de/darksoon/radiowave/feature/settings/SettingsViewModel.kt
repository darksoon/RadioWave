// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.darksoon.radiowave.core.data.repository.OfflineFirstStationRepository
import de.darksoon.radiowave.core.data.repository.RecentRepository
import de.darksoon.radiowave.core.database.dao.StationDao
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val recentRepository: RecentRepository,
    private val stationDao: StationDao,
    private val stationRepository: OfflineFirstStationRepository,
) : ViewModel() {
    fun clearHistory() {
        viewModelScope.launch {
            recentRepository.clearHistory()
        }
    }

    fun clearStationCache() {
        viewModelScope.launch {
            stationDao.deleteAllStations()
            // Also clear the in-memory search cache so results are visibly gone
            stationRepository.clearSearchCache()
        }
    }
}
