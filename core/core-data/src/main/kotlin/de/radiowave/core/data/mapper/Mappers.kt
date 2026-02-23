package de.radiowave.core.data.mapper

import de.radiowave.core.database.entity.CustomStationEntity
import de.radiowave.core.database.entity.FavoriteEntity
import de.radiowave.core.database.entity.RecentEntity
import de.radiowave.core.database.entity.StationEntity
import de.radiowave.core.model.Country
import de.radiowave.core.model.Genre
import de.radiowave.core.model.Station
import de.radiowave.core.network.dto.RadioBrowserCountry
import de.radiowave.core.network.dto.RadioBrowserStation
import de.radiowave.core.network.dto.RadioBrowserTag

/**
 * Mappers for converting between domain models and entities/DTOs.
 */

// Network DTO to Domain Model
fun RadioBrowserStation.toDomain(): Station = Station(
    uuid = uuid,
    name = name,
    streamUrl = urlResolved,
    homepageUrl = homepage,
    faviconUrl = favicon,
    country = country,
    countryCode = countryCode,
    language = language,
    tags = tags?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList(),
    codec = codec,
    bitrate = bitrate,
)

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
    sortOrder = sortOrder,
)

fun Station.toRecentEntity(): RecentEntity = RecentEntity(
    stationUuid = uuid,
    lastPlayedAt = System.currentTimeMillis(),
)

// Station Entity Mappers
fun StationEntity.toDomain(): Station = Station(
    uuid = uuid,
    name = name,
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
