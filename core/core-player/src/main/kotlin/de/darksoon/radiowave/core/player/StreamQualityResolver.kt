// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.core.player

import de.darksoon.radiowave.core.data.repository.SettingsRepository
import de.darksoon.radiowave.core.data.repository.StationRepository
import de.darksoon.radiowave.core.model.AppSettings
import de.darksoon.radiowave.core.model.Station
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class StreamQualityResolver @Inject constructor(
    private val stationRepository: StationRepository,
    private val settingsRepository: SettingsRepository,
) {
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
            .filter { it.streamUrl.isNotBlank() }
            .ifEmpty { listOf(station) }
        return selectVariant(
            originalStation = station,
            candidates = variants,
            quality = state.defaultAudioQuality,
            automotiveMode = automotiveMode,
            limitAndroidAutoQuality = state.limitAndroidAutoQuality,
        )
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
        if (candidates.size == 1) return candidates.first()
        val withKnownBitrate = candidates.filter { (it.bitrate ?: 0) > 0 }
        if (withKnownBitrate.isEmpty()) return candidates.first()

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

        return selected.copy(
            isFavorite = originalStation.isFavorite,
            lastPlayedAt = originalStation.lastPlayedAt,
            addedAt = originalStation.addedAt,
            isCustom = originalStation.isCustom,
            clickCount = selected.clickCount.takeIf { it > 0 } ?: originalStation.clickCount,
        )
    }

    private companion object {
        const val DIRECTORY_LOOKUP_TIMEOUT_MS = 1_500L
    }
}
