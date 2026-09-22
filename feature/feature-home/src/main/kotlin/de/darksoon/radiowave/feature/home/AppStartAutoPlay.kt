// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.feature.home

import de.darksoon.radiowave.core.data.repository.AppSettingsState
import de.darksoon.radiowave.core.model.PlayerState
import de.darksoon.radiowave.core.model.Station

/**
 * Station to resume when the app is cold-started with "auto-play on app start" enabled
 * (asked for by head-unit users who launch the app with the engine), or null to stay idle.
 *
 * Prefers the full recent-history entry for the remembered station so metadata and
 * favourite state are intact; falls back to the persisted last-station fields, built the
 * same way the Android Auto resume does.
 */
internal fun stationForAppStartAutoPlay(
    settings: AppSettingsState,
    playerState: PlayerState,
    recentStations: List<Station>,
): Station? {
    if (!settings.autoPlayOnAppStart) return null
    // Something already runs (Android Auto resume, a notification tap, a shortcut) — leave it.
    if (playerState.currentStation != null &&
        (playerState.isPlaying || playerState.isLoading || playerState.isBuffering)
    ) return null

    val streamUrl = settings.lastStationStreamUrl?.trim().takeUnless { it.isNullOrBlank() }
        ?: return null
    val uuid = settings.lastStationUuid?.trim().takeUnless { it.isNullOrBlank() } ?: streamUrl
    recentStations.firstOrNull { it.uuid == uuid && it.streamUrl.isNotBlank() }?.let { return it }

    return Station(
        uuid = uuid,
        name = settings.lastStationName?.trim().takeUnless { it.isNullOrBlank() } ?: streamUrl,
        streamUrl = streamUrl,
        faviconUrl = settings.lastStationFaviconUrl,
        country = settings.lastStationCountry,
        isCustom = true,
    )
}
