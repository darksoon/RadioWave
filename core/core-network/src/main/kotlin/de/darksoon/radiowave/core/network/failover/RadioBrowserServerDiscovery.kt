// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.core.network.failover

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

/**
 * Refreshes [pool] from the `/json/servers` list of a server that just answered —
 * in the background, at most once per [RadioBrowserServerPool.DISCOVERY_INTERVAL_MS],
 * so no API call ever waits for it. Until the first discovery succeeds the pool
 * works from its hard-coded fallback list.
 */
class RadioBrowserServerDiscovery(
    private val client: OkHttpClient,
    private val pool: RadioBrowserServerPool,
    private val json: Json,
    private val userAgent: String,
) {
    fun refreshIfDue(server: HttpUrl) {
        if (!pool.tryStartDiscovery()) return
        val request = Request.Builder()
            .url(server.newBuilder().encodedPath("/json/servers").query(null).build())
            .header("User-Agent", userAgent)
            .build()
        client.newCall(request).enqueue(
            object : Callback {
                override fun onFailure(call: Call, e: IOException) = Unit

                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        if (!it.isSuccessful) return
                        val servers = runCatching { parseServers(json, it.body?.string().orEmpty()) }
                            .getOrDefault(emptyList())
                        pool.updateServers(servers)
                    }
                }
            },
        )
    }

    @Serializable
    private data class ServerEntry(val name: String = "")

    companion object {
        private const val API_DOMAIN_SUFFIX = ".api.radio-browser.info"

        /**
         * Server names from a `/json/servers` answer (one entry per IP, so names
         * repeat). Only `*.api.radio-browser.info` hosts are accepted — a server
         * list must not be able to redirect the app to an arbitrary host.
         */
        fun parseServers(json: Json, body: String): List<HttpUrl> =
            json.decodeFromString(ListSerializer(ServerEntry.serializer()), body)
                .map { it.name.trim().lowercase() }
                .filter { it.endsWith(API_DOMAIN_SUFFIX) && it != RadioBrowserFailoverInterceptor.PLACEHOLDER_HOST }
                .distinct()
                .mapNotNull { "https://$it/".toHttpUrlOrNull() }
    }
}
