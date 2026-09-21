// SPDX-License-Identifier: GPL-3.0-or-later
package de.darksoon.radiowave.core.data.repository

import androidx.test.core.app.ApplicationProvider
import de.darksoon.radiowave.core.network.ITunesSearchApi
import de.darksoon.radiowave.core.network.dto.ITunesSearchResponse
import de.darksoon.radiowave.core.network.dto.ITunesSearchResult
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CoverArtRepositoryImplTest {
    private val settings = CoverSettings()
    private val api = FakeITunesApi()
    private var nowMs = 1_000_000L

    private fun repository() = CoverArtRepositoryImpl(api, settings) { nowMs }

    private fun hit(artist: String, art: String? = "https://img.example/$artist/600x600bb.jpg") =
        ITunesSearchResult(artworkUrl600 = art, artistName = artist, trackName = "Song")

    @Test fun `skips a wrong-artist first hit and takes the matching one`() = runTest {
        api.results = listOf(hit("Karaoke Stars"), hit("Daft Punk"))
        assertEquals("https://img.example/Daft Punk/600x600bb.jpg", repository().fetchCoverArt("Daft Punk", "One More Time"))
        assertEquals(CoverArtRepositoryImpl.SEARCH_RESULT_LIMIT, api.lastLimit)
    }

    @Test fun `no matching artist means no cover`() = runTest {
        api.results = listOf(hit("Somebody Else"), hit("Another Band"))
        assertNull(repository().fetchCoverArt("Daft Punk", "One More Time"))
    }

    @Test fun `no match is cached for the ttl only`() = runTest {
        api.results = emptyList()
        val repo = repository()
        assertNull(repo.fetchCoverArt("Artist", "Title"))
        assertNull(repo.fetchCoverArt("Artist", "Title"))
        assertEquals(1, api.calls)

        nowMs += CoverArtRepositoryImpl.NO_MATCH_TTL_MS
        api.results = listOf(hit("Artist"))
        assertEquals("https://img.example/Artist/600x600bb.jpg", repo.fetchCoverArt("Artist", "Title"))
        assertEquals(2, api.calls)
    }

    @Test fun `hits stay cached past the no-match ttl`() = runTest {
        api.results = listOf(hit("Artist"))
        val repo = repository()
        repo.fetchCoverArt("Artist", "Title")
        nowMs += CoverArtRepositoryImpl.NO_MATCH_TTL_MS * 4
        assertEquals("https://img.example/Artist/600x600bb.jpg", repo.fetchCoverArt("Artist", "Title"))
        assertEquals(1, api.calls)
    }

    @Test fun `io errors are not cached`() = runTest {
        val repo = repository()
        api.failure = IOException("offline")
        assertNull(repo.fetchCoverArt("Artist", "Title"))
        api.failure = null
        api.results = listOf(hit("Artist"))
        assertEquals("https://img.example/Artist/600x600bb.jpg", repo.fetchCoverArt("Artist", "Title"))
        assertEquals(2, api.calls)
    }

    @Test fun `cancellation is rethrown and not cached`() = runTest {
        val repo = repository()
        api.failure = CancellationException("song changed")
        var cancelled = false
        try {
            repo.fetchCoverArt("Artist", "Title")
        } catch (e: CancellationException) {
            cancelled = true
        }
        assertTrue(cancelled)
        api.failure = null
        api.results = listOf(hit("Artist"))
        assertEquals("https://img.example/Artist/600x600bb.jpg", repo.fetchCoverArt("Artist", "Title"))
    }

    @Test fun `disabled online lookup sends nothing`() = runTest {
        settings.data.value = settings.data.value.copy(onlineCoverLookup = false)
        api.results = listOf(hit("Artist"))
        assertNull(repository().fetchCoverArt("Artist", "Title"))
        assertEquals(0, api.calls)
    }

    @Test fun `falls back to upscaled 100px artwork`() = runTest {
        api.results = listOf(
            ITunesSearchResult(artworkUrl100 = "https://img.example/a/100x100bb.jpg", artistName = "Artist"),
        )
        assertEquals("https://img.example/a/600x600bb.jpg", repository().fetchCoverArt("Artist", "Title"))
    }

    @Test fun `artist matching normalizes case diacritics punctuation and featured artists`() {
        assertTrue(CoverArtMatcher.artistMatches("BEYONCE", "Beyoncé"))
        assertTrue(CoverArtMatcher.artistMatches("AC-DC", "AC/DC"))
        assertTrue(CoverArtMatcher.artistMatches("Simon and Garfunkel", "Simon & Garfunkel"))
        assertTrue(CoverArtMatcher.artistMatches("David Guetta feat. Sia", "David Guetta"))
        assertTrue(CoverArtMatcher.artistMatches("David Guetta ft Sia", "David Guetta & Sia"))
        assertTrue(CoverArtMatcher.artistMatches("Beatles", "The Beatles"))
        assertTrue(CoverArtMatcher.artistMatches("Mark Ronson (featuring Bruno Mars)", "Mark Ronson"))
        assertFalse(CoverArtMatcher.artistMatches("Daft Punk", "Punky Brewster Cast"))
        assertFalse(CoverArtMatcher.artistMatches("Adele", null))
        assertFalse(CoverArtMatcher.artistMatches("Adele", "Adeleine"))
    }
}

private class FakeITunesApi : ITunesSearchApi {
    var results: List<ITunesSearchResult> = emptyList()
    var failure: Exception? = null
    var calls = 0
    var lastLimit = 0

    override suspend fun search(term: String, media: String, limit: Int): ITunesSearchResponse {
        calls++
        lastLimit = limit
        failure?.let { throw it }
        return ITunesSearchResponse(resultCount = results.size, results = results)
    }
}

private class CoverSettings : SettingsRepository(ApplicationProvider.getApplicationContext()) {
    override val data = MutableStateFlow(AppSettingsState())
    override fun initialSnapshotBlocking() = data.value
}
