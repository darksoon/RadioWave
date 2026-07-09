// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.core.data.playlist

import de.darksoon.radiowave.core.data.playlist.PlaylistParser.PlaylistEntry
import java.io.BufferedReader
import java.io.StringReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistParserTest {

    private fun parse(fileName: String?, content: String): List<PlaylistEntry> =
        PlaylistParser.parse(fileName, BufferedReader(StringReader(content)))

    @Test
    fun `parses extended M3U with EXTINF titles`() {
        val content = """
            #EXTM3U
            #EXTINF:-1,Station Display Name
            http://stream.example.com/stream.mp3
            #EXTINF:-1,Another Station
            https://stream2.example.com/live
        """.trimIndent()

        val entries = parse("stations.m3u", content)

        assertEquals(
            listOf(
                PlaylistEntry("Station Display Name", "http://stream.example.com/stream.mp3"),
                PlaylistEntry("Another Station", "https://stream2.example.com/live"),
            ),
            entries,
        )
    }

    @Test
    fun `parses plain M3U with bare URLs and falls back to last path segment`() {
        val content = """
            http://stream.example.com/radio-one.mp3
            https://stream2.example.com/live
        """.trimIndent()

        val entries = parse("plain.m3u", content)

        assertEquals(
            listOf(
                PlaylistEntry("radio-one.mp3", "http://stream.example.com/radio-one.mp3"),
                PlaylistEntry("live", "https://stream2.example.com/live"),
            ),
            entries,
        )
    }

    @Test
    fun `skips non-EXTINF comment and directive lines and blank lines in M3U`() {
        val content = """
            #EXTM3U

            # just a comment
            #EXTVLCOPT:foo=bar
            #EXTINF:-1,Real Station

            http://stream.example.com/real.mp3
        """.trimIndent()

        val entries = parse("mixed.m3u", content)

        assertEquals(listOf(PlaylistEntry("Real Station", "http://stream.example.com/real.mp3")), entries)
    }

    @Test
    fun `parses well-formed PLS with titles`() {
        val content = """
            [playlist]
            NumberOfEntries=2
            File1=http://stream.example.com/stream.mp3
            Title1=Station Display Name
            Length1=-1
            File2=https://stream2.example.com/live
            Title2=Another Station
            Version=2
        """.trimIndent()

        val entries = parse("stations.pls", content)

        assertEquals(
            listOf(
                PlaylistEntry("Station Display Name", "http://stream.example.com/stream.mp3"),
                PlaylistEntry("Another Station", "https://stream2.example.com/live"),
            ),
            entries,
        )
    }

    @Test
    fun `PLS with missing titles falls back to URL-derived name`() {
        val content = """
            [playlist]
            NumberOfEntries=2
            File1=http://stream.example.com/stream.mp3
            File2=https://stream2.example.com/live-feed
            Title2=Has A Title
            Version=2
        """.trimIndent()

        val entries = parse("notitles.pls", content)

        assertEquals(
            listOf(
                PlaylistEntry("stream.mp3", "http://stream.example.com/stream.mp3"),
                PlaylistEntry("Has A Title", "https://stream2.example.com/live-feed"),
            ),
            entries,
        )
    }

    @Test
    fun `PLS with gaps in numbering and missing NumberOfEntries is parsed from File keys directly`() {
        val content = """
            [playlist]
            File1=http://stream.example.com/one.mp3
            Title1=One
            File5=http://stream.example.com/five.mp3
            Title5=Five
        """.trimIndent()

        val entries = parse("gaps.pls", content)

        assertEquals(
            listOf(
                PlaylistEntry("One", "http://stream.example.com/one.mp3"),
                PlaylistEntry("Five", "http://stream.example.com/five.mp3"),
            ),
            entries,
        )
    }

    @Test
    fun `empty file yields no entries`() {
        val entries = parse("empty.m3u", "")
        assertTrue(entries.isEmpty())
    }

    @Test
    fun `file with only comments and blank lines yields no entries`() {
        val content = """
            #EXTM3U
            # nothing here

            # still nothing
        """.trimIndent()

        val entries = parse("commentsonly.m3u", content)

        assertTrue(entries.isEmpty())
    }

    @Test
    fun `malformed content with unknown extension and no recognizable header yields no entries without crashing`() {
        val content = """
            This is not a playlist at all.
            Just some random text someone uploaded by mistake.
        """.trimIndent()

        val entries = parse("readme.txt", content)

        assertTrue(entries.isEmpty())
    }

    @Test
    fun `content sniffing overrides a wrong extension`() {
        // Mislabeled as .m3u but the content is clearly a PLS file.
        val content = """
            [playlist]
            File1=http://stream.example.com/stream.mp3
            Title1=Mislabeled Station
            NumberOfEntries=1
        """.trimIndent()

        val entries = parse("mislabeled.m3u", content)

        assertEquals(listOf(PlaylistEntry("Mislabeled Station", "http://stream.example.com/stream.mp3")), entries)
    }

    @Test
    fun `falls back to extension when content is ambiguous plain M3U`() {
        // A bare-URL M3U file has no distinctive header to sniff, so the
        // extension must be trusted here.
        val content = "http://stream.example.com/only.mp3"

        val entries = parse("bare.m3u8", content)

        assertEquals(listOf(PlaylistEntry("only.mp3", "http://stream.example.com/only.mp3")), entries)
    }

    @Test
    fun `unresolved format without extension or content signal yields no entries`() {
        val content = "http://stream.example.com/only.mp3"

        val entries = parse(null, content)

        assertTrue(entries.isEmpty())
    }
}
