// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.core.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import de.darksoon.radiowave.core.data.mapper.toDomain
import de.darksoon.radiowave.core.data.mapper.toEntity
import de.darksoon.radiowave.core.data.policy.StationDirectoryBlocklist
import de.darksoon.radiowave.core.database.dao.StationDao
import de.darksoon.radiowave.core.network.RadioBrowserApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Maintains an optional LOCAL MIRROR of the full Radio Browser station directory
 * (~50k stations) in the Room `stations` table. With the mirror in place, search,
 * browse and the fuzzy index work over ALL stations and keep working offline /
 * during Radio Browser outages.
 *
 * Sync strategy: paged `GET /json/stations` (PAGE_SIZE per request, ordered by
 * name for stable pagination) written batch-by-batch into Room — the full dump is
 * never held in memory at once. A page failure aborts the run but keeps everything
 * already written (REPLACE upserts; a later run continues to converge).
 *
 * Scheduling is deliberately simple (no WorkManager dependency): [syncIfDue] is
 * called on app start and applies the user's interval + Wi-Fi-only constraints
 * from Settings. Manual sync is always allowed via [syncNow].
 */
@Singleton
class StationMirrorRepository @Inject constructor(
    private val api: RadioBrowserApi,
    private val stationDao: StationDao,
    private val settingsRepository: SettingsRepository,
    @ApplicationContext private val context: Context,
) {

    sealed interface SyncState {
        data object Idle : SyncState
        /** [stationsSynced] grows page by page while the sync runs. */
        data class Running(val stationsSynced: Int) : SyncState
        data class Done(val stationCount: Int, val atMs: Long) : SyncState
        data class Failed(val stationsSynced: Int) : SyncState
    }

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val syncMutex = Mutex()

    /** Invoked after a successful sync so dependents (fuzzy index) can rebuild. */
    var onMirrorUpdated: (() -> Unit)? = null

    /**
     * Runs a full mirror sync if the user enabled the mirror AND the configured
     * interval has elapsed AND the connectivity constraint (Wi-Fi-only) is met.
     * Cheap no-op otherwise — safe to call on every app start.
     */
    suspend fun syncIfDue() {
        val settings = settingsRepository.data.first()
        if (!settings.stationMirrorEnabled) return
        if (settings.stationMirrorIntervalHours <= 0) return // manual only
        val intervalMs = settings.stationMirrorIntervalHours * 60L * 60L * 1000L
        if (System.currentTimeMillis() - settings.stationMirrorLastSyncAtMs < intervalMs) return
        if (settings.stationMirrorWifiOnly && !isOnUnmeteredNetwork()) return
        syncNow()
    }

    /**
     * Full mirror sync now, ignoring interval/connectivity constraints (user-initiated
     * or first-enable). Returns true on success. Concurrent calls collapse into one run.
     */
    suspend fun syncNow(): Boolean = syncMutex.withLock {
        var synced = 0
        _syncState.value = SyncState.Running(0)
        val result = runCatching {
            var offset = 0
            while (true) {
                val page = api.getStationsPage(offset = offset, limit = PAGE_SIZE)
                val valid = page
                    .map { it.toDomain() }
                    .filter { it.uuid.isNotBlank() && it.streamUrl.isNotBlank() }
                    .filterNot(StationDirectoryBlocklist::isBlocked)
                if (valid.isNotEmpty()) {
                    stationDao.insertStations(valid.map { it.toEntity() })
                    synced += valid.size
                    _syncState.value = SyncState.Running(synced)
                }
                if (page.size < PAGE_SIZE) break // last page
                offset += PAGE_SIZE
                if (offset >= MAX_STATIONS_SAFETY_CAP) break
            }
        }
        return if (result.isSuccess) {
            val now = System.currentTimeMillis()
            val count = runCatching { stationDao.countStations() }.getOrDefault(synced)
            settingsRepository.setStationMirrorSyncResult(syncedAtMs = now, stationCount = count)
            _syncState.value = SyncState.Done(stationCount = count, atMs = now)
            onMirrorUpdated?.invoke()
            true
        } else {
            android.util.Log.w("RadioWave", "Station mirror sync failed: ${result.exceptionOrNull()?.message}")
            _syncState.value = SyncState.Failed(stationsSynced = synced)
            false
        }
    }

    private fun isOnUnmeteredNetwork(): Boolean {
        val manager = context.getSystemService(ConnectivityManager::class.java) ?: return false
        val capabilities = manager.getNetworkCapabilities(manager.activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
    }

    private companion object {
        const val PAGE_SIZE = 5000
        // Backstop against a misbehaving/looping API — well above the ~50k real total.
        const val MAX_STATIONS_SAFETY_CAP = 200_000
    }
}
