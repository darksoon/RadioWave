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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
 * from Settings. [syncNow] ignores the interval but always respects Wi-Fi-only.
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
        data object WaitingForWifi : SyncState
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
        syncNow()
    }

    /**
     * Full sync ignoring the interval, including first enable. Connectivity restrictions
     * still apply. Concurrent callers wait for the current run instead of downloading again.
     */
    suspend fun syncNow(): Boolean = withContext(Dispatchers.IO) {
        if (!syncMutex.tryLock()) {
            syncMutex.withLock { /* wait for the running operation */ }
            return@withContext _syncState.value is SyncState.Done
        }
        var synced = 0
        try {
            if (!settingsRepository.data.first().stationMirrorEnabled) return@withContext false
            _syncState.value = SyncState.Running(0)
            var offset = 0
            while (true) {
                val settings = settingsRepository.data.first()
                if (!settings.stationMirrorEnabled) {
                    _syncState.value = SyncState.Idle
                    return@withContext false
                }
                if (settings.stationMirrorWifiOnly && !isOnWifiNetwork()) {
                    _syncState.value = SyncState.WaitingForWifi
                    return@withContext false
                }
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
                check(offset < MAX_STATIONS_SAFETY_CAP) { "Directory pagination exceeded safety cap" }
            }
            val now = System.currentTimeMillis()
            val count = stationDao.countStations()
            settingsRepository.setStationMirrorSyncResult(syncedAtMs = now, stationCount = count)
            _syncState.value = SyncState.Done(stationCount = count, atMs = now)
            onMirrorUpdated?.invoke()
            true
        } catch (cancelled: CancellationException) {
            _syncState.value = SyncState.Failed(stationsSynced = synced)
            throw cancelled
        } catch (error: Exception) {
            android.util.Log.w("RadioWave", "Station mirror sync failed: ${error.message}")
            _syncState.value = SyncState.Failed(stationsSynced = synced)
            false
        } finally {
            syncMutex.unlock()
        }
    }

    /** The optional offline directory is user data, not a disposable browse cache. */
    suspend fun clearCachedStations() = syncMutex.withLock {
        if (!settingsRepository.data.first().stationMirrorEnabled) {
            stationDao.deleteAllStations()
            settingsRepository.setStationMirrorSyncResult(syncedAtMs = 0, stationCount = 0)
            _syncState.value = SyncState.Idle
            onMirrorUpdated?.invoke()
        }
    }

    private fun isOnWifiNetwork(): Boolean {
        val manager = context.getSystemService(ConnectivityManager::class.java) ?: return false
        val capabilities = manager.getNetworkCapabilities(manager.activeNetwork) ?: return false
        return !capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) &&
            (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
    }

    private companion object {
        const val PAGE_SIZE = 5000
        // Backstop against a misbehaving/looping API — well above the ~50k real total.
        const val MAX_STATIONS_SAFETY_CAP = 200_000
    }
}
