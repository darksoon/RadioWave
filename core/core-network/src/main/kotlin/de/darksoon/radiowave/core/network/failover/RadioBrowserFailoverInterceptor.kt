// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.core.network.failover

import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Sends Radio Browser API calls to a concrete server from [pool] instead of the
 * round-robin placeholder host the Retrofit base URL uses. A connection failure,
 * timeout or 5xx answer transparently retries the same request on the next
 * server; 2xx–4xx answers are final (a 404 is the server working correctly).
 *
 * Each server is tried at most once per call. Non-idempotent calls (the click /
 * vote POSTs) get at most [maxNonIdempotentAttempts] servers so a slow server
 * that did register the call isn't followed by a whole chain of duplicates.
 * Requests to any other host pass through untouched.
 */
class RadioBrowserFailoverInterceptor(
    private val pool: RadioBrowserServerPool,
    private val userAgent: String,
    private val placeholderHost: String = PLACEHOLDER_HOST,
    private val maxAttempts: Int = 4,
    private val maxNonIdempotentAttempts: Int = 2,
    private val attemptConnectTimeoutMs: Int = ATTEMPT_CONNECT_TIMEOUT_MS,
    private val onServerConfirmed: (HttpUrl) -> Unit = {},
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        if (!original.url.host.equals(placeholderHost, ignoreCase = true)) {
            return chain.proceed(original)
        }
        val idempotent = original.method == "GET" || original.method == "HEAD"
        val servers = pool.candidates()
            .take(if (idempotent) maxAttempts else maxNonIdempotentAttempts)
        // A short connect timeout per attempt keeps the fallback fast; the read
        // timeout stays whatever the client is configured with.
        val attemptChain = chain.withConnectTimeout(attemptConnectTimeoutMs, TimeUnit.MILLISECONDS)

        var lastFailure: IOException? = null
        for ((index, server) in servers.withIndex()) {
            val request = original.newBuilder()
                .url(
                    original.url.newBuilder()
                        .scheme(server.scheme)
                        .host(server.host)
                        .port(server.port)
                        .build(),
                )
                .header("User-Agent", userAgent)
                .build()
            val response = try {
                attemptChain.proceed(request)
            } catch (e: IOException) {
                // A cancelled call (screen left, coroutine cancelled) is not a server fault.
                if (chain.call().isCanceled()) throw e
                pool.reportFailure(server)
                lastFailure = e
                continue
            }
            if (response.code >= 500) {
                pool.reportFailure(server)
                if (index < servers.lastIndex) {
                    response.close()
                    continue
                }
                return response
            }
            pool.reportSuccess(server)
            onServerConfirmed(server)
            return response
        }
        throw lastFailure ?: IOException("No Radio Browser server available")
    }

    companion object {
        /** Host of the Retrofit base URL; it only marks requests this interceptor routes. */
        const val PLACEHOLDER_HOST = "all.api.radio-browser.info"
        const val ATTEMPT_CONNECT_TIMEOUT_MS = 8_000
    }
}
