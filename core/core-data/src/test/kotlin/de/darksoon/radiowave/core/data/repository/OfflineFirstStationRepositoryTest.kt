// SPDX-License-Identifier: GPL-3.0-or-later
package de.darksoon.radiowave.core.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import de.darksoon.radiowave.core.data.policy.StationDirectoryBlocklist
import de.darksoon.radiowave.core.database.dao.StationDao
import de.darksoon.radiowave.core.database.entity.StationEntity
import de.darksoon.radiowave.core.model.Station
import de.darksoon.radiowave.core.network.RadioBrowserApi
import de.darksoon.radiowave.core.network.dto.RadioBrowserStation
import java.io.File
import java.lang.reflect.Proxy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OfflineFirstStationRepositoryTest {
    @get:Rule val tmp = TemporaryFolder()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val rows = LinkedHashMap<String, StationEntity>()
    private var topDirectory = listOf("t1", "t2", "t3")
    private var directoryDown = false
    private var topRequests = 0
    private var nowMs = 1_000_000L
    private lateinit var storeFile: File

    private val api = Proxy.newProxyInstance(RadioBrowserApi::class.java.classLoader, arrayOf(RadioBrowserApi::class.java)) { _, method, _ ->
        check(method.name == "getTopStations") { "Unexpected API call: ${method.name}" }
        topRequests++
        if (directoryDown) throw IllegalStateException("Directory unreachable")
        topDirectory.map { RadioBrowserStation(uuid = it, name = "Radio $it", url = "https://example.test/$it") }
    } as RadioBrowserApi

    @Suppress("UNCHECKED_CAST")
    private val dao = Proxy.newProxyInstance(StationDao::class.java.classLoader, arrayOf(StationDao::class.java)) { _, method, args ->
        when (method.name) {
            "insertStations" -> {
                (args[0] as List<StationEntity>).forEach { rows[it.uuid] = it.copy(cachedAt = nowMs++) }
                Unit
            }
            // Deliberately unordered, like SQL IN () — the repository must restore the ranking.
            "getStationsByUuids" -> (args[0] as List<String>).mapNotNull { rows[it] }.reversed()
            "getLatestStations" -> rows.values.sortedByDescending { it.cachedAt }.take(args[0] as Int)
            else -> error("Unexpected DAO operation: ${method.name}")
        }
    } as StationDao

    @Before fun setUp() {
        storeFile = File(tmp.root, "top_stations.txt")
    }

    /** A fresh repository = a fresh process: empty in-memory caches, same disk state. */
    private fun repository() = OfflineFirstStationRepository(
        api,
        dao,
        StationMirrorRepository(api, dao, IdleSettings(), context),
        TopStationsStore(storeFile),
    ).also { it.nowMs = { nowMs } }

    private suspend fun OfflineFirstStationRepository.top(force: Boolean = false): List<String> =
        getTopStations(force).toList().last().map(Station::uuid)

    private fun cacheMirrorPage(vararg uuids: String) {
        uuids.forEach { rows[it] = StationEntity(uuid = it, name = "Mirror $it", streamUrl = "https://example.test/$it", cachedAt = nowMs++) }
    }

    @Test fun `offline top list keeps the last fetched ranking after a mirror import`() = runTest {
        assertEquals(listOf("t1", "t2", "t3"), repository().top())
        // A later mirror sync writes newer rows; they must not become "Top".
        cacheMirrorPage("m1", "m2", "m3")

        directoryDown = true
        val offline = repository().getTopStations().toList()
        assertEquals(listOf("t1", "t2", "t3"), offline.first().map(Station::uuid))
        assertEquals(listOf("t1", "t2", "t3"), offline.last().map(Station::uuid))
    }

    @Test fun `a newer top list replaces the stored ranking`() = runTest {
        repository().top()
        topDirectory = listOf("t3", "t9", "t1")
        // Online the fresh ranking leads; older entries may trail after it.
        assertEquals(listOf("t3", "t9", "t1"), repository().top().take(3))

        directoryDown = true
        assertEquals(listOf("t3", "t9", "t1"), repository().top())
        assertEquals(listOf("t3", "t9", "t1"), storeFile.readLines())
    }

    @Test fun `stored top stations are blocklist scrubbed`() = runTest {
        val blocked = StationDirectoryBlocklist.blockedUuids.first()
        cacheMirrorPage("t1", blocked, "t2")
        TopStationsStore(storeFile).write(listOf("t1", blocked, "t2"))

        directoryDown = true
        assertEquals(listOf("t1", "t2"), repository().top())
    }

    @Test fun `without a stored top list the latest cached stations stand in`() = runTest {
        cacheMirrorPage("m1", "m2")
        directoryDown = true
        assertEquals(listOf("m2", "m1"), repository().top())
    }

    @Test fun `plain loads within the ttl stay on the cache`() = runTest {
        val repo = repository()
        repo.top()
        nowMs += 20 * 60 * 1000L
        repo.top()
        assertEquals(1, topRequests)
    }

    @Test fun `forced refresh bypasses the ttl but not within a few seconds`() = runTest {
        val repo = repository()
        repo.top()
        repo.top(force = true)
        assertEquals("repeated pull right after a fetch reuses it", 1, topRequests)

        nowMs += 11_000L
        topDirectory = listOf("t4", "t5")
        assertEquals(listOf("t4", "t5"), repo.top(force = true).take(2))
        assertEquals(2, topRequests)
    }

    @Test fun `forced refresh falls back to cached data when the directory fails`() = runTest {
        val repo = repository()
        repo.top()
        nowMs += 11_000L
        directoryDown = true
        assertEquals(listOf("t1", "t2", "t3"), repo.top(force = true))
        assertEquals(2, topRequests)
    }

    @Test fun `top list store survives a restart and drops duplicates`() = runTest {
        TopStationsStore(storeFile).write(listOf("a", "b", "a", " ", "c"))
        assertEquals(listOf("a", "b", "c"), TopStationsStore(storeFile).read())
        assertEquals(emptyList<String>(), TopStationsStore(File(tmp.root, "missing.txt")).read())
    }
}

private class IdleSettings : SettingsRepository(ApplicationProvider.getApplicationContext()) {
    override val data = MutableStateFlow(AppSettingsState())
    override fun initialSnapshotBlocking() = data.value
}
