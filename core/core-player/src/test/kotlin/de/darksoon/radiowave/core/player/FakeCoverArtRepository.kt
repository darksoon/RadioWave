// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.core.player

import de.darksoon.radiowave.core.data.repository.CoverArtRepository

/** Shared test double — returns a fixed cover URL (default: none found). */
internal class FakeCoverArtRepository(
    private val result: String? = null,
) : CoverArtRepository {
    override suspend fun fetchCoverArt(artist: String?, title: String?): String? = result
    override fun clearCache() = Unit
}
