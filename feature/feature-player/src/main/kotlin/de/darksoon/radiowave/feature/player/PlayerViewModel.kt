// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.feature.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.darksoon.radiowave.core.data.repository.CoverArtRepository
import de.darksoon.radiowave.core.data.repository.StationRepository
import de.darksoon.radiowave.core.model.Station
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Similar stations for one source station, plus why they were picked: the source's
 * tags that the suggestions share, or its country when the tag lookup found nothing.
 */
data class SimilarStations(
    val sourceStationUuid: String? = null,
    val stations: List<Station> = emptyList(),
    val matchedTags: List<String> = emptyList(),
    val matchedCountry: String? = null,
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val coverArtRepository: CoverArtRepository,
    private val stationRepository: StationRepository,
) : ViewModel() {

    private val _coverArtUrl = MutableStateFlow<String?>(null)
    val coverArtUrl: StateFlow<String?> = _coverArtUrl.asStateFlow()

    private val _similarStations = MutableStateFlow(SimilarStations())
    val similarStations: StateFlow<SimilarStations> = _similarStations.asStateFlow()

    private var similarStationsJob: Job? = null

    suspend fun loadCoverArt(artist: String?, title: String?) {
        _coverArtUrl.value = coverArtRepository.fetchCoverArt(artist, title)
    }

    fun clearCoverArt() {
        _coverArtUrl.value = null
    }

    /**
     * Loads suggestions for [station] once. Re-opening the player for the same station
     * reuses the result; an empty result (typically offline) is retried on the next call.
     * The repository applies the directory blocklist.
     */
    fun loadSimilarStations(station: Station) {
        val current = _similarStations.value
        if (current.sourceStationUuid == station.uuid) {
            if (similarStationsJob?.isActive == true || current.stations.isNotEmpty()) return
        }
        similarStationsJob?.cancel()
        // Drop the previous station's row right away instead of showing stale picks.
        _similarStations.value = SimilarStations(sourceStationUuid = station.uuid)
        similarStationsJob = viewModelScope.launch {
            val candidates = stationRepository.getSimilarStations(station)
            _similarStations.value = buildSimilarStations(station, candidates)
        }
    }

    fun clearSimilarStations() {
        similarStationsJob?.cancel()
        _similarStations.value = SimilarStations()
    }
}

private const val MAX_SIMILAR_STATIONS = 12

/**
 * Removes the source station and its re-uploads under another UUID, entries without a
 * stream and same-name duplicates, then records which source tags the picks share.
 */
internal fun buildSimilarStations(source: Station, candidates: List<Station>): SimilarStations {
    val sourceName = source.name.normalizedStationName()
    val stations = candidates
        .asSequence()
        .filter { it.uuid != source.uuid && it.streamUrl.isNotBlank() }
        .filter { it.name.normalizedStationName() != sourceName }
        .distinctBy { it.name.normalizedStationName() }
        .take(MAX_SIMILAR_STATIONS)
        .toList()
    if (stations.isEmpty()) return SimilarStations(sourceStationUuid = source.uuid)

    val sourceTags = source.tags.map { it.trim() }.filter { it.isNotEmpty() }.take(2)
    // The directory's by-tag lookup matches partially ("jazz" also finds "smooth jazz").
    val matchedTags = sourceTags.filter { tag ->
        stations.any { station -> station.tags.any { it.contains(tag, ignoreCase = true) } }
    }
    val matchedCountry = if (matchedTags.isEmpty()) {
        source.country?.trim()?.takeIf { country ->
            country.isNotEmpty() && stations.any { it.country?.trim().equals(country, ignoreCase = true) }
        }
    } else {
        null
    }
    return SimilarStations(
        sourceStationUuid = source.uuid,
        stations = stations,
        matchedTags = matchedTags,
        matchedCountry = matchedCountry,
    )
}

private fun String.normalizedStationName(): String = trim().lowercase()
