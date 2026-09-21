// SPDX-License-Identifier: GPL-3.0-or-later
package de.darksoon.radiowave.core.network.failover

import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.util.concurrent.TimeUnit

class RadioBrowserFailoverInterceptorTest {
    private val servers = List(3) { MockWebServer() }
    private val byBase = HashMap<HttpUrl, MockWebServer>()
    private var nowMs = 1_000L
    private lateinit var pool: RadioBrowserServerPool
    private lateinit var client: OkHttpClient
    private val confirmed = mutableListOf<HttpUrl>()

    @Before fun setUp() {
        servers.forEach { it.start() }
        servers.forEach { byBase[it.url("/")] = it }
        pool = RadioBrowserServerPool(servers.map { it.url("/") }, clockMs = { nowMs })
        client = OkHttpClient.Builder()
            .readTimeout(500, TimeUnit.MILLISECONDS)
            .addInterceptor(
                RadioBrowserFailoverInterceptor(pool, userAgent = "RadioWave/test", onServerConfirmed = { confirmed += it }),
            )
            .build()
    }

    @After fun tearDown() {
        servers.forEach { runCatching { it.shutdown() } }
    }

    /** Servers in the order the pool will try them. */
    private fun order(): List<MockWebServer> = pool.candidates().map { byBase.getValue(it) }

    private fun get(path: String = "/json/stations/topclick/5") = client.newCall(
        Request.Builder().url("https://all.api.radio-browser.info$path").build(),
    ).execute()

    @Test fun `server error retries the same request on the next server`() {
        val (first, second) = order()
        first.enqueue(MockResponse().setResponseCode(503))
        second.enqueue(MockResponse().setBody("[]"))

        get().use { response ->
            assertEquals(200, response.code)
            assertEquals("[]", response.body!!.string())
        }
        assertEquals(1, first.requestCount)
        assertEquals("/json/stations/topclick/5", second.takeRequest().path)
        assertEquals(listOf(second.url("/")), confirmed)
    }

    @Test fun `connection failure falls through to a working server`() {
        val (first, second) = order()
        // Dropping the connection instead of shutdown(): MockWebServer.shutdown() can itself
        // throw after a 5 s executor wait inside CI containers.
        first.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))
        second.enqueue(MockResponse().setBody("ok"))

        get().use { assertEquals("ok", it.body!!.string()) }
        assertEquals(1, second.requestCount)
    }

    @Test fun `read timeout falls through to a working server`() {
        val (first, second) = order()
        first.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
        second.enqueue(MockResponse().setBody("ok"))

        get().use { assertEquals("ok", it.body!!.string()) }
        assertEquals(1, first.requestCount)
        assertEquals(1, second.requestCount)
    }

    @Test fun `client errors are final and not retried`() {
        val (first, second) = order()
        first.enqueue(MockResponse().setResponseCode(404))

        get().use { assertEquals(404, it.code) }
        assertEquals(0, second.requestCount)
    }

    @Test fun `the server that answered stays sticky for the next calls`() {
        val (first, second) = order()
        first.enqueue(MockResponse().setResponseCode(502))
        second.enqueue(MockResponse().setBody("a"))
        second.enqueue(MockResponse().setBody("b"))
        get().close()

        get().use { assertEquals("b", it.body!!.string()) }
        assertEquals(1, first.requestCount)
        assertEquals(2, second.requestCount)
    }

    @Test fun `post is sent at most once per server and to at most two servers`() {
        servers.forEach { it.enqueue(MockResponse().setResponseCode(503)) }
        val (first, second, third) = order()
        val response = client.newCall(
            Request.Builder()
                .url("https://all.api.radio-browser.info/json/url/abc")
                .post(ByteArray(0).toRequestBody())
                .build(),
        ).execute()

        response.use { assertEquals(503, it.code) }
        assertEquals(listOf(1, 1, 0), listOf(first, second, third).map { it.requestCount })
        assertTrue(confirmed.isEmpty())
    }

    @Test fun `every server failing surfaces the io error`() {
        servers.forEach { it.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START)) }
        val error = runCatching { get() }.exceptionOrNull()
        assertTrue(error is IOException)
    }

    @Test fun `last server's 5xx answer is returned instead of an exception`() {
        servers.forEach { it.enqueue(MockResponse().setResponseCode(500)) }
        get().use { assertEquals(500, it.code) }
        assertEquals(listOf(1, 1, 1), servers.map { it.requestCount })
    }

    @Test fun `requests carry the app user agent`() {
        val first = order().first()
        first.enqueue(MockResponse().setBody("ok"))
        get().close()
        assertEquals("RadioWave/test", first.takeRequest().getHeader("User-Agent"))
    }

    @Test fun `other hosts pass through untouched`() {
        val target = servers[2]
        target.enqueue(MockResponse().setBody("direct"))
        client.newCall(Request.Builder().url(target.url("/cover")).build()).execute()
            .use { assertEquals("direct", it.body!!.string()) }
        assertEquals(listOf(0, 0, 1), servers.map { it.requestCount })
        assertTrue(confirmed.isEmpty())
    }

    @Test fun `server list parsing keeps only radio browser api hosts`() {
        val body = """
            [{"ip":"91.98.4.78","name":"de1.api.radio-browser.info"},
             {"ip":"2a01:4f8::1","name":"de1.api.radio-browser.info"},
             {"ip":"1.2.3.4","name":"FI1.api.radio-browser.info"},
             {"ip":"6.6.6.6","name":"evil.example.com"},
             {"ip":"7.7.7.7","name":"all.api.radio-browser.info"}]
        """.trimIndent()
        val parsed = RadioBrowserServerDiscovery.parseServers(Json { ignoreUnknownKeys = true }, body)
        assertEquals(listOf("de1.api.radio-browser.info", "fi1.api.radio-browser.info"), parsed.map { it.host })
        assertTrue(parsed.all { it.scheme == "https" })
    }
}
