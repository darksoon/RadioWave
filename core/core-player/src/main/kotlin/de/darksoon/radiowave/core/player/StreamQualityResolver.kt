// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.core.player

import de.darksoon.radiowave.core.data.repository.SettingsRepository
import de.darksoon.radiowave.core.data.repository.StationRepository
import de.darksoon.radiowave.core.model.AppSettings
import de.darksoon.radiowave.core.model.Station
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class StreamQualityResolver @Inject constructor(
    private val stationRepository: StationRepository,
    private val settingsRepository: SettingsRepository,
) {
    suspend fun resolve(
        station: Station,
        automotiveMode: Boolean,
    ): Station {
        val state = settingsRepository.data.first()
        val variants = stationRepository.getStationVariants(station)
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
}

