// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.core.model

import java.net.URI
import java.util.Locale

/*
 * Radio-browser lists the same station many times (one entry per stream URL/bitrate).
 * Home (and other lists) collapse those variants to one card.
 *
 * REA-UNSICHER: Datei/Paket geraten. Im Handy-Build: Schlüssel = p000/hr4.java,
 * Zusammenfassen = p000/d43.java#m2127e. Die Funktionen werden auch von anderen
 * Bildschirmen benutzt (b00/d00/C0474ly/l00/wi0/tm0/C0771tz/C0648qn) — falls ein anderer
 * Agent sie schon an anderer Stelle rekonstruiert hat, bitte zusammenführen.
 */

private val WHITESPACE = Regex("\\s+")

/**
 * Identity of a station independent of its stream variant: normalised name plus the
 * homepage (preferred) or the stream URL's host/path/query. Null when the name is blank
 * or no usable URL exists — callers then fall back to the uuid.
 */
fun Station.duplicateKey(): String? {
    val normalizedName = name.trim().lowercase(Locale.ROOT).replace(WHITESPACE, " ")
    if (normalizedName.isBlank()) return null

    val homepageKey = homepageUrl?.trim()?.takeIf { it.isNotEmpty() }?.let(::urlKey)
    if (homepageKey != null) return "$normalizedName|home:$homepageKey"

    val streamKey = streamUrl.trim().takeIf { it.isNotEmpty() }?.let(::urlKey) ?: return null
    return "$normalizedName|stream:$streamKey"
}

/** [duplicateKey] or, when that is unavailable, a uuid-based key. */
fun Station.duplicateKeyOrUuid(): String = duplicateKey() ?: uuid

/**
 * Collapses station variants to one entry per [duplicateKey], keeping the position of the
 * first occurrence. Among duplicates the preferred variant wins: a favourite over a
 * non-favourite, then https over http, then the higher bitrate, then more clicks.
 */
fun collapseDuplicateStations(stations: List<Station>, favoriteIds: Set<String>): List<Station> {
    val byKey = LinkedHashMap<String, Station>(stations.size)
    for (candidate in stations) {
        val key = candidate.duplicateKey() ?: "uuid:${candidate.uuid}"
        val existing = byKey[key]
        if (existing == null || candidate.isPreferredOver(existing, favoriteIds)) {
            byKey[key] = candidate
        }
    }
    return byKey.values.toList()
}

private fun Station.isPreferredOver(other: Station, favoriteIds: Set<String>): Boolean {
    val thisFavorite = uuid in favoriteIds
    if (thisFavorite != (other.uuid in favoriteIds)) return thisFavorite
    val thisHttps = streamUrl.startsWith("https://", ignoreCase = true)
    if (thisHttps != other.streamUrl.startsWith("https://", ignoreCase = true)) return thisHttps
    val thisBitrate = (bitrate ?: 0).coerceAtLeast(0)
    val otherBitrate = (other.bitrate ?: 0).coerceAtLeast(0)
    return if (thisBitrate == otherBitrate) clickCount > other.clickCount else thisBitrate > otherBitrate
}

private fun urlKey(raw: String): String? {
    val uri = parseLenient(raw) ?: return null
    val host = hostKey(uri) ?: return null
    val path = (uri.path ?: "").trimEnd('/')
    val query = uri.rawQuery?.takeUnless { it.isBlank() }?.let { "?$it" } ?: ""
    return host + path + query
}

private fun hostKey(uri: URI): String? {
    val host = uri.host?.lowercase(Locale.ROOT)?.removePrefix("www.")?.takeUnless { it.isBlank() }
        ?: return null
    val defaultPort = when (uri.scheme?.lowercase(Locale.ROOT)) {
        "http" -> 80
        "https" -> 443
        else -> -1
    }
    val port = uri.port.takeIf { it != -1 && it != defaultPort }
    return host + (port?.let { ":$it" } ?: "")
}

private fun parseLenient(value: String): URI? = runCatching {
    URI(if (value.contains("://")) value else "https://$value")
}.getOrNull()
