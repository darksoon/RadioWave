// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.core.model

import java.util.Locale

/**
 * The genres the customisable Home screen offers. Each one has a bundled tile image
 * (genre_*_v1.webp), which is why the set is fixed instead of coming from the directory.
 *
 * REA-UNSICHER: Paket/Name geraten. Im Handy-Build ist das ein eigenes Objekt (p000/qr1.java),
 * genutzt von SettingsRepository (Lesen/Schreiben von "home_genres"/"home_start_genre").
 * Die Liste und die Normalisierung sind exakt aus dem Dekompilat übernommen.
 */
object HomeGenres {
    val ALL: List<String> = listOf("techno", "rock", "jazz", "chillout")

    /** Default for "home_start_genre" when nothing was chosen yet. */
    const val DEFAULT_START_GENRE: String = "chillout"

    /**
     * Trims/lowercases the stored list, drops unknown genres and duplicates (keeping the
     * user's order). An empty result falls back to all genres — Home always shows at least one.
     */
    fun sanitize(genres: List<String>): List<String> {
        val cleaned = genres
            .map { it.trim().lowercase(Locale.ROOT) }
            .filter { it in ALL }
            .distinct()
        return cleaned.ifEmpty { ALL }
    }
}
