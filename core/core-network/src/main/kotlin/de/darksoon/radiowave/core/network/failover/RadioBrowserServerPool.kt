// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.core.network.failover

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import kotlin.random.Random

/**
 * The set of Radio Browser API servers a request may be sent to, following
 * https://api.radio-browser.info/: pick a random server instead of hammering one,
 * stay on a server that works, move on when it fails.
 *
 * Order handed out by [candidates]: the sticky server (last one that answered,
 * for [stickyMs]), then the remaining servers in a per-list random order with
 * recently failed ones last, then the DNS round-robin name as a last resort.
 * Servers are base URLs (scheme + host + port) so tests can point them at local
 * mock servers.
 */
class RadioBrowserServerPool(
    fallbackServers: List<HttpUrl>,
    lastResort: HttpUrl? = null,
    private val stickyMs: Long = STICKY_MS,
    private val failurePenaltyMs: Long = FAILURE_PENALTY_MS,
    private val random: Random = Random.Default,
    private val clockMs: () -> Long = { System.nanoTime() / 1_000_000L },
) {
    private val lastResort = lastResort?.base()
    private val fallback = fallbackServers.map { it.base() }.distinct()
    private var servers: List<HttpUrl> = fallback.shuffled(random)
    private var stickyServer: HttpUrl? = null
    private var stickySinceMs = 0L
    private val failedAtMs = HashMap<HttpUrl, Long>()
    private var nextDiscoveryAtMs = Long.MIN_VALUE

    @Synchronized
    fun candidates(): List<HttpUrl> {
        val now = clockMs()
        val sticky = stickyServer?.takeIf { now - stickySinceMs < stickyMs }
        val (penalized, healthy) = servers.partition { server ->
            failedAtMs[server]?.let { now - it < failurePenaltyMs } == true
        }
        return (listOfNotNull(sticky) + healthy + penalized + listOfNotNull(lastResort))
            .distinct()
    }

    @Synchronized
    fun reportSuccess(server: HttpUrl) {
        val base = server.base()
        failedAtMs.remove(base)
        if (stickyServer != base) {
            stickyServer = base
            stickySinceMs = clockMs()
        }
    }

    @Synchronized
    fun reportFailure(server: HttpUrl) {
        val base = server.base()
        failedAtMs[base] = clockMs()
        if (stickyServer == base) stickyServer = null
    }

    /**
     * Replaces the server list with a discovered one. An empty result keeps the
     * current list and counts as a failed discovery (retried sooner).
     */
    @Synchronized
    fun updateServers(discovered: List<HttpUrl>) {
        val fresh = discovered.map { it.base() }.distinct()
        if (fresh.isEmpty()) return
        nextDiscoveryAtMs = clockMs() + DISCOVERY_INTERVAL_MS
        if (fresh.toSet() == servers.toSet()) return
        servers = fresh.shuffled(random)
        failedAtMs.keys.retainAll(fresh.toSet())
        if (stickyServer != null && stickyServer !in fresh && stickyServer != lastResort) {
            stickyServer = null
        }
    }

    /**
     * Claims the next server-list discovery. Returns false while one is not due;
     * a claimed but failed discovery is retried after [DISCOVERY_RETRY_MS].
     */
    @Synchronized
    fun tryStartDiscovery(): Boolean {
        val now = clockMs()
        if (now < nextDiscoveryAtMs) return false
        nextDiscoveryAtMs = now + DISCOVERY_RETRY_MS
        return true
    }

    @Synchronized
    fun currentServers(): List<HttpUrl> = servers

    private fun HttpUrl.base(): HttpUrl = "$scheme://$host:$port/".toHttpUrl()

    companion object {
        const val STICKY_MS = 30 * 60 * 1000L
        const val FAILURE_PENALTY_MS = 5 * 60 * 1000L
        const val DISCOVERY_INTERVAL_MS = 24 * 60 * 60 * 1000L
        const val DISCOVERY_RETRY_MS = 60 * 60 * 1000L
    }
}
