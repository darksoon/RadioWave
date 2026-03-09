// SPDX-License-Identifier: GPL-3.0-or-later

package de.radiowave.core.player

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import de.radiowave.core.data.repository.StationRepository
import de.radiowave.core.model.AppSettings
import de.radiowave.core.model.Station
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class StreamQualityResolver @Inject constructor(
    @ApplicationContext private val context: Context,
    private val stationRepository: StationRepository,
) {
    suspend fun resolve(
        station: Station,
        automotiveMode: Boolean,
    ): Station {
        val qualitySetting = loadQualitySetting()
        val effectiveQuality = effectiveQuality(
            requested = qualitySetting,
            automotiveMode = automotiveMode,
        )
        val variants = stationRepository.getStationVariants(station)
            .filter { it.streamUrl.isNotBlank() }
            .ifEmpty { listOf(station) }
        return selectVariant(
            originalStation = station,
            candidates = variants,
            quality = effectiveQuality,
            automotiveMode = automotiveMode,
        )
    }

    private fun loadQualitySetting(): String {
        val prefs = context.getSharedPreferences(AppSettings.PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(AppSettings.KEY_DEFAULT_AUDIO_QUALITY, AppSettings.QUALITY_AUTO)
            ?: AppSettings.QUALITY_AUTO
    }

    private fun effectiveQuality(
        requested: String,
        automotiveMode: Boolean,
    ): String {
        return requested
    }

    private fun selectVariant(
        originalStation: Station,
        candidates: List<Station>,
        quality: String,
        automotiveMode: Boolean,
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
        val targetKbps = if (automotiveMode) {
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

