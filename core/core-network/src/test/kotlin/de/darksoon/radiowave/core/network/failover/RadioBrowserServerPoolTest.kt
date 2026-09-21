// SPDX-License-Identifier: GPL-3.0-or-later
package de.darksoon.radiowave.core.network.failover

import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RadioBrowserServerPoolTest {
    private var nowMs = 0L
    private val a = "https://a.api.radio-browser.info/".toHttpUrl()
    private val b = "https://b.api.radio-browser.info/".toHttpUrl()
    private val c = "https://c.api.radio-browser.info/".toHttpUrl()
    private val all = "https://all.api.radio-browser.info/".toHttpUrl()

    private fun pool() = RadioBrowserServerPool(listOf(a, b, c), lastResort = all, clockMs = { nowMs })

    @Test fun `every server is offered once with the round robin name last`() {
        val candidates = pool().candidates()
        assertEquals(setOf(a, b, c), candidates.take(3).toSet())
        assertEquals(all, candidates.last())
        assertEquals(4, candidates.size)
    }

    @Test fun `recently failed servers move to the back until the penalty lapses`() {
        val pool = pool()
        val failed = pool.candidates().first()
        pool.reportFailure(failed)
        assertEquals(failed, pool.candidates()[2])

        nowMs += RadioBrowserServerPool.FAILURE_PENALTY_MS
        assertEquals(failed, pool.candidates().first())
    }

    @Test fun `a working server is sticky until the sticky window ends or it fails`() {
        val pool = pool()
        val last = pool.candidates()[2]
        pool.reportSuccess(last)
        assertEquals(last, pool.candidates().first())

        pool.reportFailure(last)
        assertEquals(last, pool.candidates()[2])

        pool.reportSuccess(last)
        nowMs += RadioBrowserServerPool.STICKY_MS
        assertEquals(pool.currentServers(), pool.candidates().take(3))
    }

    @Test fun `discovery replaces the list and is due again only after its interval`() {
        val pool = pool()
        assertTrue(pool.tryStartDiscovery())
        assertFalse(pool.tryStartDiscovery())

        val d = "https://d.api.radio-browser.info/".toHttpUrl()
        pool.updateServers(listOf(d, a))
        assertEquals(setOf(d, a), pool.currentServers().toSet())

        nowMs += RadioBrowserServerPool.DISCOVERY_INTERVAL_MS - 1
        assertFalse(pool.tryStartDiscovery())
        nowMs += 1
        assertTrue(pool.tryStartDiscovery())
    }

    @Test fun `failed discovery is retried after the retry delay and keeps the fallback list`() {
        val pool = pool()
        assertTrue(pool.tryStartDiscovery())
        pool.updateServers(emptyList())
        assertEquals(setOf(a, b, c), pool.currentServers().toSet())

        val fresh = pool()
        assertTrue(fresh.tryStartDiscovery())
        nowMs += RadioBrowserServerPool.DISCOVERY_RETRY_MS
        assertTrue(fresh.tryStartDiscovery())
    }
}
