// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.core.model

/**
 * Rough data-usage estimate for a given quality setting, shown in Settings so users on a
 * limited mobile plan know what to expect before picking a quality level. Deliberately an
 * estimate, not a live measurement — actual usage varies per station (not every station
 * offers every bitrate; [de.darksoon.radiowave.core.player.StreamQualityResolver] falls back
 * to the closest available variant).
 *
 * The target-kbps-per-quality values mirror [de.darksoon.radiowave.core.player.StreamQualityResolver]'s
 * `baseTargetKbps` selection (core-player can't be a dependency of core-model, so these are
 * intentionally kept in sync by hand rather than shared) — update both if either changes.
 */
object DataUsageEstimate {
    private const val LOW_KBPS = 64
    private const val MEDIUM_KBPS = 128
    private const val HIGH_KBPS = 192

    /** Approximate megabytes per hour of continuous playback at [quality]. */
    fun megabytesPerHour(quality: String): Double {
        val kbps = when (quality) {
            AppSettings.QUALITY_LOW -> LOW_KBPS
            AppSettings.QUALITY_HIGH -> HIGH_KBPS
            // AUTO currently resolves the same as MEDIUM in StreamQualityResolver (no live
            // adaptive-bitrate logic yet — see ROADMAP.md) — same estimate is honest, not a bug.
            AppSettings.QUALITY_MEDIUM, AppSettings.QUALITY_AUTO -> MEDIUM_KBPS
            else -> MEDIUM_KBPS
        }
        // kbps * 3600s/h / 8 bits-per-byte / 1024 KB-per-MB
        return kbps * 3600.0 / 8.0 / 1024.0
    }
}
