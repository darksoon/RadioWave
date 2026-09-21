// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.core.data.policy

import de.darksoon.radiowave.core.model.Station
import java.io.ByteArrayInputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StationInputValidatorTest {

    private fun valid(station: Station): Station {
        val result = StationInputValidator.validate(station)
        assertTrue("expected Valid, got $result", result is StationInputValidator.Result.Valid)
        return (result as StationInputValidator.Result.Valid).station
    }

    @Test
    fun `accepts http and https urls with host`() {
        listOf(
            "http://stream.example.com/live",
            "https://stream.example.com:8443/live.mp3?token=1",
            "  HTTPS://Example.com  ",
            "http://user:pw@example.com/",
            "http://[2001:db8::1]:8000/stream",
            "http://10.0.0.1",
        ).forEach { assertTrue(it, StationInputValidator.isValidStreamUrl(it)) }
    }

    @Test
    fun `rejects other schemes, missing hosts and malformed ports`() {
        listOf(
            "",
            "   ",
            "https:",
            "http://",
            "http:///path",
            "http://:8000/",
            "http://user@/x",
            "ftp://example.com/a",
            "file:///sdcard/a.mp3",
            "content://media/external/audio/1",
            "javascript:alert(1)",
            "example.com/stream",
            "http://example.com:99999/",
            "http://example.com:abc/",
            "http://exa mple.com/",
            "http://example.com/\nlive",
        ).forEach { assertFalse(it, StationInputValidator.isValidStreamUrl(it)) }
    }

    @Test
    fun `rejects urls above the length limit`() {
        val base = "https://example.com/"
        val atLimit = base + "a".repeat(StationInputValidator.MAX_URL_LENGTH - base.length)
        assertTrue(StationInputValidator.isValidStreamUrl(atLimit))
        assertFalse(StationInputValidator.isValidStreamUrl(atLimit + "a"))
    }

    @Test
    fun `validate rejects blank or oversized uuid and invalid stream url`() {
        val ok = Station(uuid = "id", name = "Name", streamUrl = "https://example.com")
        assertEquals(StationInputValidator.Result.Invalid, StationInputValidator.validate(ok.copy(uuid = " ")))
        assertEquals(
            StationInputValidator.Result.Invalid,
            StationInputValidator.validate(ok.copy(uuid = "x".repeat(StationInputValidator.MAX_UUID_LENGTH + 1))),
        )
        assertEquals(
            StationInputValidator.Result.Invalid,
            StationInputValidator.validate(ok.copy(streamUrl = "file:///etc/hosts")),
        )
    }

    @Test
    fun `validate trims and truncates fields and drops non-http side urls`() {
        val station = valid(
            Station(
                uuid = "  id  ",
                name = "  " + "n".repeat(500) + "  ",
                streamUrl = " https://example.com/live ",
                homepageUrl = "javascript:alert(1)",
                faviconUrl = "https://example.com/icon.png",
                country = "c".repeat(500),
                countryCode = "  ",
                tags = listOf(" rock ", "", "rock", "t".repeat(200)) + List(100) { "tag$it" },
                bitrate = -1,
            ),
        )
        assertEquals("id", station.uuid)
        assertEquals(StationInputValidator.MAX_NAME_LENGTH, station.name.length)
        assertEquals("https://example.com/live", station.streamUrl)
        assertNull(station.homepageUrl)
        assertEquals("https://example.com/icon.png", station.faviconUrl)
        assertEquals(StationInputValidator.MAX_FIELD_LENGTH, station.country!!.length)
        assertNull(station.countryCode)
        assertEquals("rock", station.tags.first())
        assertEquals(StationInputValidator.MAX_TAG_LENGTH, station.tags[1].length)
        assertEquals(StationInputValidator.MAX_TAGS, station.tags.size)
        assertNull(station.bitrate)
    }

    @Test
    fun `validate falls back to the stream url for a blank name`() {
        val station = valid(Station(uuid = "id", name = "  ", streamUrl = "https://example.com/live"))
        assertEquals("https://example.com/live", station.name)
    }

    @Test
    fun `validate reports blocklisted stations as Blocked`() {
        assertEquals(
            StationInputValidator.Result.Blocked,
            StationInputValidator.validate(
                Station(uuid = "custom-1", name = "Anything", streamUrl = "https://stream.alnour.com.lb/live"),
            ),
        )
        assertEquals(
            StationInputValidator.Result.Blocked,
            StationInputValidator.validate(
                Station(uuid = "03ecebf1-7164-11e9-af37-52543be04c81", name = "Renamed", streamUrl = "https://example.com"),
            ),
        )
        // A blocked homepage must count even though sanitizing would keep/drop it.
        assertEquals(
            StationInputValidator.Result.Blocked,
            StationInputValidator.validate(
                Station(
                    uuid = "x",
                    name = "Mirror",
                    streamUrl = "https://example.com",
                    homepageUrl = "https://www.almanar.com.lb",
                ),
            ),
        )
        assertEquals(
            StationInputValidator.Result.Blocked,
            StationInputValidator.validate(Station(uuid = "x", name = "Al-Aqsa Voice", streamUrl = "https://example.com")),
        )
    }

    @Test
    fun `readBoundedText returns content up to the limit and null above it`() {
        val text = "äöü #EXTM3U"
        val bytes = text.toByteArray(Charsets.UTF_8)
        assertEquals(text, StationInputValidator.readBoundedText(ByteArrayInputStream(bytes), maxBytes = bytes.size))
        assertNull(StationInputValidator.readBoundedText(ByteArrayInputStream(bytes), maxBytes = bytes.size - 1))
        val big = ByteArray(StationInputValidator.MAX_IMPORT_BYTES + 1) { 'a'.code.toByte() }
        assertNull(StationInputValidator.readBoundedText(ByteArrayInputStream(big)))
    }
}
