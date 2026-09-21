// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.core.network.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import de.darksoon.radiowave.core.network.BuildConfig
import de.darksoon.radiowave.core.network.ITunesSearchApi
import de.darksoon.radiowave.core.network.RadioBrowserApi
import de.darksoon.radiowave.core.network.failover.RadioBrowserFailoverInterceptor
import de.darksoon.radiowave.core.network.failover.RadioBrowserServerDiscovery
import de.darksoon.radiowave.core.network.failover.RadioBrowserServerPool
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

/**
 * Dagger module for network dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val BASE_URL = "https://all.api.radio-browser.info/"
    private const val ITUNES_BASE_URL = "https://itunes.apple.com/"
    private const val RADIO_BROWSER_USER_AGENT = "RadioWave/1.0"

    // Known API servers (checked against /json/servers on 2026-09-21). Only the
    // starting point: the live list is discovered at runtime, and the round-robin
    // name BASE_URL stays the last resort.
    private val RADIO_BROWSER_FALLBACK_SERVERS = listOf(
        "https://de1.api.radio-browser.info/",
        "https://de2.api.radio-browser.info/",
    )

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)

        if (BuildConfig.DEBUG) {
            builder.addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                },
            )
        }

        return builder.build()
    }

    @Provides
    @Singleton
    fun provideRadioBrowserServerPool(): RadioBrowserServerPool = RadioBrowserServerPool(
        fallbackServers = RADIO_BROWSER_FALLBACK_SERVERS.map { it.toHttpUrl() },
        lastResort = BASE_URL.toHttpUrl(),
    )

    /**
     * The shared client plus server failover (see [RadioBrowserFailoverInterceptor]).
     * Added first so the debug logger sees every attempt with its real host.
     */
    @Provides
    @Singleton
    @Named("radioBrowser")
    fun provideRadioBrowserOkHttpClient(
        okHttpClient: OkHttpClient,
        pool: RadioBrowserServerPool,
        json: Json,
    ): OkHttpClient {
        val discovery = RadioBrowserServerDiscovery(okHttpClient, pool, json, RADIO_BROWSER_USER_AGENT)
        val failover = RadioBrowserFailoverInterceptor(
            pool = pool,
            userAgent = RADIO_BROWSER_USER_AGENT,
            onServerConfirmed = discovery::refreshIfDue,
        )
        return okHttpClient.newBuilder()
            .apply { interceptors().add(0, failover) }
            .build()
    }

    @Provides
    @Singleton
    @Named("radioBrowser")
    fun provideRadioBrowserRetrofit(
        @Named("radioBrowser") okHttpClient: OkHttpClient,
        json: Json,
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Provides
    @Singleton
    fun provideRadioBrowserApi(@Named("radioBrowser") retrofit: Retrofit): RadioBrowserApi {
        return retrofit.create(RadioBrowserApi::class.java)
    }

    @Provides
    @Singleton
    @Named("iTunes")
    fun provideITunesRetrofit(
        okHttpClient: OkHttpClient,
        json: Json,
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(ITUNES_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Provides
    @Singleton
    fun provideITunesSearchApi(@Named("iTunes") retrofit: Retrofit): ITunesSearchApi {
        return retrofit.create(ITunesSearchApi::class.java)
    }
}
