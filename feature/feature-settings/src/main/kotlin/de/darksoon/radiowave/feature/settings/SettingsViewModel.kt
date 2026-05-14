// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.darksoon.radiowave.core.data.repository.AppSettingsState
import de.darksoon.radiowave.core.data.repository.OfflineFirstStationRepository
import de.darksoon.radiowave.core.data.repository.RecentRepository
import de.darksoon.radiowave.core.data.repository.SettingsRepository
import de.darksoon.radiowave.core.database.dao.StationDao
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val recentRepository: RecentRepository,
    private val stationDao: StationDao,
    private val stationRepository: OfflineFirstStationRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    /** Snapshot of all settings — observed reactively by the UI. */
    val settings: StateFlow<AppSettingsState> = settingsRepository.data
        .stateIn(viewModelScope, SharingStarted.Eagerly, settingsRepository.initialSnapshotBlocking())

    // Setters — thin wrappers around SettingsRepository. UI dispatches onChange events here.
    fun setThemeMode(v: String) = viewModelScope.launch { settingsRepository.setThemeMode(v) }
    fun setAppLanguage(v: String) = viewModelScope.launch { settingsRepository.setAppLanguage(v) }
    fun setDynamicColors(v: Boolean) = viewModelScope.launch { settingsRepository.setDynamicColors(v) }
    fun setShowMiniplayerMetadata(v: Boolean) = viewModelScope.launch { settingsRepository.setShowMiniplayerMetadata(v) }
    fun setKeepScreenOnFullscreen(v: Boolean) = viewModelScope.launch { settingsRepository.setKeepScreenOnFullscreen(v) }
    fun setShowQuickToasts(v: Boolean) = viewModelScope.launch { settingsRepository.setShowQuickToasts(v) }
    fun setShowInsecureStreams(v: Boolean) = viewModelScope.launch { settingsRepository.setShowInsecureStreams(v) }
    fun setNotificationShowPlayPause(v: Boolean) = viewModelScope.launch { settingsRepository.setNotificationShowPlayPause(v) }
    fun setNotificationShowStop(v: Boolean) = viewModelScope.launch { settingsRepository.setNotificationShowStop(v) }
    fun setNotificationShowPrevious(v: Boolean) = viewModelScope.launch { settingsRepository.setNotificationShowPrevious(v) }
    fun setNotificationShowNext(v: Boolean) = viewModelScope.launch { settingsRepository.setNotificationShowNext(v) }
    fun setDefaultAudioQuality(v: String) = viewModelScope.launch { settingsRepository.setDefaultAudioQuality(v) }
    fun setAllowMobileData(v: Boolean) = viewModelScope.launch { settingsRepository.setAllowMobileData(v) }
    fun setBufferProfile(v: String) = viewModelScope.launch { settingsRepository.setBufferProfile(v) }
    fun setTimeshiftGuard(v: Boolean) = viewModelScope.launch { settingsRepository.setTimeshiftGuard(v) }
    fun setThermalMode(v: Boolean) = viewModelScope.launch { settingsRepository.setThermalMode(v) }
    fun setAutoPlayOnAndroidAutoConnect(v: Boolean) = viewModelScope.launch { settingsRepository.setAutoPlayOnAndroidAutoConnect(v) }
    fun setLimitAndroidAutoQuality(v: Boolean) = viewModelScope.launch { settingsRepository.setLimitAndroidAutoQuality(v) }
    fun setConfirmRemoveFavorite(v: Boolean) = viewModelScope.launch { settingsRepository.setConfirmRemoveFavorite(v) }

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
