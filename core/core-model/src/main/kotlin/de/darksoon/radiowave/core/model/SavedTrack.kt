// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.core.model

import kotlinx.serialization.Serializable

/**
 * A song the user bookmarked from the player ("Gemerkte Titel").
 *
 * Only the text the station broadcast (ICY title/artist) is kept — no audio is recorded.
 * [id] is a SHA-256 over the normalised "artist\u0000title" pair, so saving the same song
 * twice (even from another station) updates the existing bookmark instead of duplicating it.
 *
 * REA: Klassenname und Paket sind durch den Serialisierungs-Deskriptor
 * "de.darksoon.radiowave.core.model.SavedTrack" belegt (handy p000/l94.java), Feldnamen
 * und -reihenfolge ebenfalls (id, title, artist?, stationName, savedAt).
 */
@Serializable
data class SavedTrack(
    val id: String,
    val title: String,
    val artist: String?,
    val stationName: String,
    val savedAt: Long,
)
