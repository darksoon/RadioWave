// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.darksoon.radiowave.core.data.update.GitHubReleaseChecker
import de.darksoon.radiowave.core.data.update.GitHubReleaseInfo
import de.darksoon.radiowave.core.data.repository.RecentRepository
import de.darksoon.radiowave.core.database.dao.StationDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUpdateUiState(
    val isChecking: Boolean = false,
    val latestRelease: GitHubReleaseInfo? = null,
    val hasUpdate: Boolean = false,
    val lastError: String? = null,
    val lastCheckedAtMs: Long? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val recentRepository: RecentRepository,
    private val stationDao: StationDao,
) : ViewModel() {
    private val _updateUiState = MutableStateFlow(SettingsUpdateUiState())
    val updateUiState: StateFlow<SettingsUpdateUiState> = _updateUiState.asStateFlow()

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

    fun checkForUpdates(
        currentVersionName: String,
        includePrerelease: Boolean,
    ) {
        viewModelScope.launch {
            _updateUiState.update { it.copy(isChecking = true, lastError = null) }
            val now = System.currentTimeMillis()
            runCatching {
                val latest = GitHubReleaseChecker.getLatestInstallableRelease(
                    includePrerelease = includePrerelease,
                )
                val hasUpdate = latest?.let { release ->
                    normalizeVersion(release.tag) != normalizeVersion(currentVersionName)
                } ?: false
                latest to hasUpdate
            }.onSuccess { (latest, hasUpdate) ->
                _updateUiState.update {
                    it.copy(
                        isChecking = false,
                        latestRelease = latest,
                        hasUpdate = hasUpdate,
                        lastError = null,
                        lastCheckedAtMs = now,
                    )
                }
            }.onFailure { error ->
                _updateUiState.update {
                    it.copy(
                        isChecking = false,
                        lastError = error.message ?: "Update-Pruefung fehlgeschlagen",
                        lastCheckedAtMs = now,
                    )
                }
            }
        }
    }

    private fun normalizeVersion(raw: String): String {
        return raw.trim().removePrefix("v").lowercase()
    }
}

