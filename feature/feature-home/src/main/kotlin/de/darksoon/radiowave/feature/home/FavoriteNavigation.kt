// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.feature.home

import de.darksoon.radiowave.core.model.Station

/**
 * The favorite the player's skip buttons should jump to, or null if there is none.
 *
 * While a favorite plays, [step] walks the favorites list in its user-defined order and
 * wraps around at both ends. From a non-favorite station, "next" enters the list at the
 * first favorite and "previous" at the last one.
 */
internal fun adjacentFavorite(favorites: List<Station>, currentUuid: String?, step: Int): Station? {
    val playable = favorites.filter { it.streamUrl.isNotBlank() }
    if (playable.isEmpty() || step == 0) return null
    val index = playable.indexOfFirst { it.uuid == currentUuid }
    if (index < 0) return if (step > 0) playable.first() else playable.last()
    if (playable.size == 1) return null
    return playable[(index + step).mod(playable.size)]
}
