// SPDX-License-Identifier: GPL-3.0-or-later

package de.radiowave.feature.player

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import de.radiowave.core.data.repository.CoverArtRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val coverArtRepository: CoverArtRepository,
) : ViewModel() {

    private val _coverArtUrl = MutableStateFlow<String?>(null)
    val coverArtUrl: StateFlow<String?> = _coverArtUrl.asStateFlow()

    suspend fun loadCoverArt(artist: String?, title: String?) {
        _coverArtUrl.value = coverArtRepository.fetchCoverArt(artist, title)
    }

    fun clearCoverArt() {
        _coverArtUrl.value = null
    }
}
