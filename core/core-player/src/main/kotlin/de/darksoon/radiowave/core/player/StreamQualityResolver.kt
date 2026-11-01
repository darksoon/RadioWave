// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.core.player

import de.darksoon.radiowave.core.data.repository.SettingsRepository
import de.darksoon.radiowave.core.data.repository.StationRepository
import de.darksoon.radiowave.core.model.AppSettings
import de.darksoon.radiowave.core.model.Station
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * Picks the stream variant of a station that matches the quality setting.
 *
 * REA 95fcb33 (p000/ms4.java) — Änderungen gegenüber v1.0.19:
 *  - Qualität "Auto" löst jetzt über den Netztyp auf: getaktet/Mobilfunk →
 *    `mobileAudioQuality`, sonst `wifiAudioQuality` (vorher fest 128 kbps). Kann der
 *    Netztyp nicht bestimmt werden (Exception), gilt "getaktet".
 *  - Varianten werden nur noch genommen, wenn ihre URL einen Host hat und https ist —
 *    http nur, wenn auch der Ausgangssender http ist UND "Unsichere HTTP-Streams anzeigen"
 *    an ist. So wird ein https-Sender nie auf einen http-Spiegel herabgestuft.
 *  - Ergebnis ist der ORIGINAL-Sender mit ausgetauschter streamUrl/codec/bitrate
 *    (vorher: die Variante mit übernommenen Favoriten-/Verlaufsfeldern) — Name, UUID und
 *    Logo bleiben also die des gewählten Senders.
 * Neuer dritter Konstruktorparameter (p000/js4.java, hier [StreamNetworkInfo]); im Build
 * wird daraus im Konstruktor die Funktionsreferenz `js4::isMetered` (C0486m9) gebaut.
 * REA-UNSICHER: Der interne Lambda-Konstruktor (für Tests) ist nicht belegt — R8 hätte
 * eine solche Konstruktor-Kette zu genau dem beobachteten Bytecode zusammengefaltet.
 */
@Singleton
class StreamQualityResolver internal constructor(
    private val stationRepository: StationRepository,
    private val settingsRepository: SettingsRepository,
    private val isMeteredNetwork: () -> Boolean = { false },
) {
    @Inject
    constructor(
        stationRepository: StationRepository,
        settingsRepository: SettingsRepository,
        networkInfo: StreamNetworkInfo,
    ) : this(stationRepository, settingsRepository, networkInfo::isMetered)

    // Stations already looked up in the directory this process — a station with only one
    // stream must not pay the lookup on every start.
    private val directoryCheckedUuids = ConcurrentHashMap.newKeySet<String>()

    suspend fun resolve(
        station: Station,
        automotiveMode: Boolean,
    ): Station {
        if (station.isCustom || station.uuid.startsWith("custom-")) return station
        val state = settingsRepository.data.first()
        val cached = try {
            stationRepository.getCachedStationVariants(station)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            listOf(station)
        }
        val variants = (if (cached.size > 1) cached else directoryVariantsQuickly(station) ?: cached)
            .filter { isAllowedVariantUrl(it.streamUrl, station.streamUrl, state.showInsecureStreams) }
            .ifEmpty { listOf(station) }
        val quality = if (state.defaultAudioQuality == AppSettings.QUALITY_AUTO) {
            // Decided once at station start; a later network change doesn't switch streams
            // (settings_auto_quality_help: "laufende Wiedergabe wird nicht unterbrochen").
            val metered = runCatching { isMeteredNetwork() }.getOrDefault(true)
            if (metered) state.mobileAudioQuality else state.wifiAudioQuality
        } else {
            state.defaultAudioQuality
        }
        return selectVariant(
            originalStation = station,
            candidates = variants,
            quality = quality,
            automotiveMode = automotiveMode,
            limitAndroidAutoQuality = state.limitAndroidAutoQuality,
        )
    }

    /**
     * A variant must have a parseable host and use https. Plain http is only accepted when
     * the station itself is http and the user allows insecure streams.
     */
    private fun isAllowedVariantUrl(url: String, originalUrl: String, allowInsecure: Boolean): Boolean {
        val uri = runCatching { URI(url) }.getOrNull() ?: return false
        if (uri.host == null) return false
        if (uri.scheme.equals("https", ignoreCase = true)) return true
        return originalUrl.startsWith("http://", ignoreCase = true) &&
            allowInsecure &&
            uri.scheme.equals("http", ignoreCase = true)
    }

    /**
     * Without a local mirror the cache rarely knows other streams of a station, so the
     * quality setting would never apply. One bounded directory lookup per station keeps
     * that working; a slow or unreachable directory only costs [DIRECTORY_LOOKUP_TIMEOUT_MS].
     */
    private suspend fun directoryVariantsQuickly(station: Station): List<Station>? {
        if (!directoryCheckedUuids.add(station.uuid)) return null
        return try {
            withTimeoutOrNull(DIRECTORY_LOOKUP_TIMEOUT_MS) {
                stationRepository.getStationVariants(station)
            }?.takeIf { it.size > 1 }
        } catch (cancelled: CancellationException) {
            directoryCheckedUuids.remove(station.uuid)
            throw cancelled
        } catch (_: Exception) {
            null
        }
    }

    private fun selectVariant(
        originalStation: Station,
        candidates: List<Station>,
        quality: String,
        automotiveMode: Boolean,
        limitAndroidAutoQuality: Boolean,
    ): Station {
        if (candidates.size == 1) return originalStation.withStreamOf(candidates.first())
        val withKnownBitrate = candidates.filter { (it.bitrate ?: 0) > 0 }
        if (withKnownBitrate.isEmpty()) return originalStation.withStreamOf(candidates.first())

        val baseTargetKbps = when (quality) {
            AppSettings.QUALITY_LOW -> 64
            AppSettings.QUALITY_MEDIUM -> if (automotiveMode) 96 else 128
            AppSettings.QUALITY_HIGH -> 192
            else -> 128
        }
        val targetKbps = if (automotiveMode && limitAndroidAutoQuality) {
            minOf(baseTargetKbps, 128)
        } else {
            baseTargetKbps
        }

        val belowOrEqual = withKnownBitrate.filter { (it.bitrate ?: 0) <= targetKbps }
        val selected = if (belowOrEqual.isNotEmpty()) {
            belowOrEqual.maxBy { it.bitrate ?: 0 }
        } else {
            withKnownBitrate.minBy { abs((it.bitrate ?: targetKbps) - targetKbps) }
        }

        return originalStation.withStreamOf(selected)
    }

    /** REA 95fcb33: ms4.m6844c — nur streamUrl, codec und bitrate der Variante übernehmen. */
    private fun Station.withStreamOf(variant: Station): Station = copy(
        streamUrl = variant.streamUrl,
        codec = variant.codec,
        bitrate = variant.bitrate,
    )

    private companion object {
        const val DIRECTORY_LOOKUP_TIMEOUT_MS = 1_500L
    }
}
