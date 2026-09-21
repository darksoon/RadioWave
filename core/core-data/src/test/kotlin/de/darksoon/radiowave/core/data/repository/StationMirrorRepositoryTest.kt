// SPDX-License-Identifier: GPL-3.0-or-later
package de.darksoon.radiowave.core.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import androidx.test.core.app.ApplicationProvider
import de.darksoon.radiowave.core.database.dao.StationDao
import de.darksoon.radiowave.core.network.RadioBrowserApi
import de.darksoon.radiowave.core.network.dto.RadioBrowserStation
import java.lang.reflect.Proxy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowNetworkInfo

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StationMirrorRepositoryTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val settings = MirrorSettings()
    private var pageRequests = 0
    private var deletes = 0
    private var cancelDownload = false
    private var directory = emptyList<RadioBrowserStation>()
    private var prunes = 0
    private val api = Proxy.newProxyInstance(RadioBrowserApi::class.java.classLoader, arrayOf(RadioBrowserApi::class.java)) { _, method, _ ->
        check(method.name == "getStationsPage")
        pageRequests++
        if (cancelDownload) throw CancellationException("Leaving screen")
        directory
    } as RadioBrowserApi
    private val dao = Proxy.newProxyInstance(StationDao::class.java.classLoader, arrayOf(StationDao::class.java)) { _, method, _ ->
        when (method.name) {
            "countStations" -> 12
            "deleteAllStations" -> { deletes++; Unit }
            "insertStations" -> Unit
            "deleteStationsNotRefreshedSince" -> { prunes++; 0 }
            else -> error("Unexpected DAO operation: ${method.name}")
        }
    } as StationDao

    private fun mirror() = StationMirrorRepository(api, dao, settings, context)

    @Test fun `initial download respects wifi only even on unmetered cellular`() = runTest {
        setNetwork(NetworkCapabilities.TRANSPORT_CELLULAR)
        val mirror = mirror()
        assertFalse(mirror.syncNow())
        assertEquals(0, pageRequests)
        assertEquals(StationMirrorRepository.SyncState.WaitingForWifi, mirror.syncState.value)
        assertEquals(0L, settings.data.value.stationMirrorLastSyncAtMs)
    }

    @Test fun `wifi download records completed state`() = runTest {
        setNetwork(NetworkCapabilities.TRANSPORT_WIFI)
        val mirror = mirror()
        assertTrue(mirror.syncNow())
        assertEquals(1, pageRequests)
        assertEquals(12, settings.data.value.stationMirrorStationCount)
        assertTrue(mirror.syncState.value is StationMirrorRepository.SyncState.Done)
    }

    @Test fun `complete sync prunes stations removed upstream`() = runTest {
        setNetwork(NetworkCapabilities.TRANSPORT_WIFI)
        settings.update { it.copy(stationMirrorStationCount = 3) }
        directory = stations(3)
        assertTrue(mirror().syncNow())
        assertEquals(1, prunes)
    }

    @Test fun `truncated or empty directory never prunes the mirror`() = runTest {
        setNetwork(NetworkCapabilities.TRANSPORT_WIFI)
        settings.update { it.copy(stationMirrorStationCount = 100) }
        directory = stations(3)
        assertTrue(mirror().syncNow())
        directory = emptyList()
        assertTrue(mirror().syncNow())
        assertEquals(0, prunes)
    }

    private fun stations(n: Int) = (1..n).map {
        RadioBrowserStation(uuid = "uuid-$it", name = "Radio $it", url = "https://example.test/$it")
    }

    @Test fun `cache clear preserves enabled offline directory and sync status`() = runTest {
        settings.update { it.copy(stationMirrorLastSyncAtMs = 100, stationMirrorStationCount = 12) }
        mirror().clearCachedStations()
        assertEquals(0, deletes)
        assertEquals(100L, settings.data.value.stationMirrorLastSyncAtMs)
        assertEquals(12, settings.data.value.stationMirrorStationCount)
    }

    @Test fun `clearing disabled directory also resets stale sync status`() = runTest {
        settings.update { it.copy(stationMirrorEnabled = false, stationMirrorLastSyncAtMs = 100, stationMirrorStationCount = 12) }
        mirror().clearCachedStations()
        assertEquals(1, deletes)
        assertEquals(0L, settings.data.value.stationMirrorLastSyncAtMs)
        assertEquals(0, settings.data.value.stationMirrorStationCount)
    }

    @Test fun `cancelled download remains cancelled and releases lock for retry`() = runTest {
        setNetwork(NetworkCapabilities.TRANSPORT_WIFI)
        val mirror = mirror()
        cancelDownload = true
        try {
            mirror.syncNow()
            fail("Cancellation must propagate")
        } catch (_: CancellationException) { /* expected */ }
        assertEquals(0L, settings.data.value.stationMirrorLastSyncAtMs)
        cancelDownload = false
        assertTrue(mirror.syncNow())
    }

    private fun setNetwork(transport: Int) {
        val manager = context.getSystemService(ConnectivityManager::class.java)
        val type = if (transport == NetworkCapabilities.TRANSPORT_WIFI) ConnectivityManager.TYPE_WIFI else ConnectivityManager.TYPE_MOBILE
        shadowOf(manager).setActiveNetworkInfo(
            ShadowNetworkInfo.newInstance(NetworkInfo.DetailedState.CONNECTED, type, 0, true, true),
        )
        val capabilities = NetworkCapabilities().also {
            shadowOf(it).addTransportType(transport)
            shadowOf(it).addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
        }
        shadowOf(manager).setNetworkCapabilities(manager.activeNetwork!!, capabilities)
    }
}

private class MirrorSettings : SettingsRepository(ApplicationProvider.getApplicationContext()) {
    override val data = MutableStateFlow(AppSettingsState(stationMirrorEnabled = true))
    override fun initialSnapshotBlocking() = data.value
    fun update(transform: (AppSettingsState) -> AppSettingsState) { data.value = transform(data.value) }
    override suspend fun setStationMirrorSyncResult(syncedAtMs: Long, stationCount: Int) {
        update { it.copy(stationMirrorLastSyncAtMs = syncedAtMs, stationMirrorStationCount = stationCount) }
    }
}
