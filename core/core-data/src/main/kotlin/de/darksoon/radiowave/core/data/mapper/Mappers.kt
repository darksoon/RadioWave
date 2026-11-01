// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.core.data.mapper

import de.darksoon.radiowave.core.database.entity.CustomStationEntity
import de.darksoon.radiowave.core.database.entity.FavoriteEntity
import de.darksoon.radiowave.core.database.entity.RecentEntity
import de.darksoon.radiowave.core.database.entity.StationEntity
import de.darksoon.radiowave.core.model.Country
import de.darksoon.radiowave.core.model.Genre
import de.darksoon.radiowave.core.model.Station
import de.darksoon.radiowave.core.network.dto.RadioBrowserCountry
import de.darksoon.radiowave.core.network.dto.RadioBrowserStation
import de.darksoon.radiowave.core.network.dto.RadioBrowserTag

/**
 * Mappers for converting between domain models and entities/DTOs.
 */

// REA 95fcb33 (tk2.f20493a/f20494b, tk2.m9638a): Codec-/Bitraten-Anhänge im
// Sendernamen ("Radio X - MP3 128k", "Radio X | AAC+ 64 kbps", "Radio X - HLS")
// werden für die Anzeige entfernt. Angewendet auf Radio-Browser-DTOs, den
// Stations-Cache sowie die Fallback-Zuordnung von Favoriten/Zuletzt gehört –
// NICHT auf eigene Sender. Name der Funktion/Konstanten geraten.
private val CODEC_BITRATE_SUFFIX = Regex(
    "(?i)\\s*[-–—|]?\\s*(?:aac(?:\\s*plus|\\+)?|mp3|ogg|opus|hls)\\s*(?:hd\\s*)?\\d{2,4}\\s*k(?:bps)?\\s*$",
)
private val CODEC_SUFFIX = Regex(
    "(?i)\\s*[-–—|]\\s*(?:aac(?:\\s*plus|\\+)?|mp3|ogg|opus|hls)\\s*(?:hd)?\\s*$",
)

fun cleanStationName(name: String): String {
    val trimmed = name.trim()
    if (trimmed.isEmpty()) return name
    val cleaned = CODEC_SUFFIX.replace(CODEC_BITRATE_SUFFIX.replace(trimmed, "").trim(), "").trim()
    // Bestünde der Name nur aus dem Codec-Anhang, bleibt er erhalten.
    return if (cleaned.isBlank()) trimmed else cleaned
}

// Network DTO to Domain Model
fun RadioBrowserStation.toDomain(): Station {
    return Station(
        uuid = uuid ?: "",
        name = cleanStationName(name ?: "Unknown Station"),
        streamUrl = urlResolved ?: url ?: "",
        homepageUrl = homepage,
        faviconUrl = favicon,
        country = country,
        countryCode = countryCode,
        language = language,
        tags = tags?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList(),
        codec = codec,
        bitrate = bitrate,
        // Only ever populated from a fresh network response (see Station.clickCount's
        // default of 0) — the local `stations` cache table has no column for it, since
        // a stale click count from a cached row isn't a meaningful popularity signal
        // and adding one would mean another Room migration for a nice-to-have.
        clickCount = clickCount ?: 0,
    )
}

fun RadioBrowserTag.toDomain(): Genre = Genre(
    name = name,
    stationCount = stationCount,
)

fun RadioBrowserCountry.toDomain(): Country = Country(
    name = name,
    code = code,
    stationCount = stationCount,
)

// Entity to Domain Model
fun CustomStationEntity.toDomain(): Station = Station(
    uuid = uuid,
    name = name,
    streamUrl = streamUrl,
    homepageUrl = homepageUrl,
    faviconUrl = faviconUrl,
    country = country,
    tags = genre?.let { listOf(it) } ?: emptyList(),
    isCustom = true,
    addedAt = addedAt,
)

fun FavoriteEntity.toDomain(station: Station): Station = station.copy(isFavorite = true)

fun RecentEntity.toDomain(station: Station): Station = station.copy(
    lastPlayedAt = lastPlayedAt,
)

// Domain Model to Entity
fun Station.toCustomEntity(): CustomStationEntity = CustomStationEntity(
    uuid = uuid,
    name = name,
    streamUrl = streamUrl,
    homepageUrl = homepageUrl,
    faviconUrl = faviconUrl,
    genre = tags.firstOrNull(),
    country = country,
    addedAt = addedAt,
)

fun Station.toFavoriteEntity(sortOrder: Int = 0): FavoriteEntity = FavoriteEntity(
    stationUuid = uuid,
    stationName = name,
    streamUrl = streamUrl,
    homepageUrl = homepageUrl,
    faviconUrl = faviconUrl,
    country = country,
    countryCode = countryCode,
    language = language,
    tags = tags.joinToString(",").takeIf { it.isNotBlank() },
    codec = codec,
    bitrate = bitrate,
    sortOrder = sortOrder,
)

fun Station.toRecentEntity(): RecentEntity = RecentEntity(
    stationUuid = uuid,
    stationName = name,
    streamUrl = streamUrl,
    homepageUrl = homepageUrl,
    faviconUrl = faviconUrl,
    country = country,
    countryCode = countryCode,
    language = language,
    tags = tags.joinToString(",").takeIf { it.isNotBlank() },
    codec = codec,
    bitrate = bitrate,
    lastPlayedAt = System.currentTimeMillis(),
)

// Station Entity Mappers
fun StationEntity.toDomain(): Station = Station(
    uuid = uuid,
    name = cleanStationName(name),
    streamUrl = streamUrl,
    homepageUrl = homepageUrl,
    faviconUrl = faviconUrl,
    country = country,
    countryCode = countryCode,
    language = language,
    tags = tags?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList(),
    codec = codec,
    bitrate = bitrate,
)

fun Station.toEntity(): StationEntity = StationEntity(
    uuid = uuid,
    name = name,
    streamUrl = streamUrl,
    homepageUrl = homepageUrl,
    faviconUrl = faviconUrl,
    country = country,
    countryCode = countryCode,
    language = language,
    tags = tags.joinToString(","),
    codec = codec,
    bitrate = bitrate,
)

