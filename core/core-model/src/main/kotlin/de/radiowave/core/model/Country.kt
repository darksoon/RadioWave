package de.radiowave.core.model

/**
 * Represents a country with available radio stations.
 */
data class Country(
    val name: String,
    val code: String,
    val stationCount: Int,
)
