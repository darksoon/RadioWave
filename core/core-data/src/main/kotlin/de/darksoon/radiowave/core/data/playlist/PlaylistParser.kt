// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.core.data.playlist

import java.io.BufferedReader

/**
 * Parses M3U/M3U8 and PLS playlist files into a flat list of [PlaylistEntry].
 *
 * This is pure, dependency-free parsing logic so it can be unit tested without
 * Robolectric or a device. Callers are responsible for opening the
 * [BufferedReader] (e.g. from a SAF `Uri`'s `InputStream`) and for
 * validating/deduping/persisting the resulting entries — this object only
 * turns playlist text into candidate (name, streamUrl) pairs.
 */
object PlaylistParser {

    /** One parsed playlist entry, before stream URL validation. */
    data class PlaylistEntry(val name: String, val streamUrl: String)

    enum class PlaylistFormat { M3U, PLS }

    /**
     * Parses [reader] line by line into a list of entries.
     *
     * Format is decided by [fileName]'s extension first (`.m3u`/`.m3u8` vs
     * `.pls`). If the extension is missing, unrecognized, or contradicted by
     * the file's actual content (a mislabeled file), content sniffing of the
     * first non-blank line (`#EXTM3U` header vs `[playlist]` header) is used
     * instead — a clear content signal always wins so a mislabeled-but-valid
     * file doesn't fail to import just because of its extension.
     *
     * Returns an empty list if the format can't be determined at all, the
     * file is empty, or it contains no parseable entries. Callers should
     * treat that as "nothing to import" rather than an error — this function
     * never throws on malformed input.
     */
    fun parse(fileName: String?, reader: BufferedReader): List<PlaylistEntry> {
        val iterator = reader.lineSequence().iterator()
        val buffered = mutableListOf<String>()
        var sniffLine: String? = null
        while (iterator.hasNext()) {
            val line = iterator.next()
            buffered += line
            if (sniffLine == null && line.isNotBlank()) {
                sniffLine = line
                break
            }
        }
        val format = detectFormatByContent(sniffLine) ?: detectFormatByExtension(fileName) ?: return emptyList()
        val allLines = buffered.asSequence() + iterator.asSequence()
        return when (format) {
            PlaylistFormat.M3U -> parseM3u(allLines)
            PlaylistFormat.PLS -> parsePls(allLines)
        }
    }

    private fun detectFormatByExtension(fileName: String?): PlaylistFormat? {
        val extension = fileName?.substringAfterLast('.', "")?.lowercase()
        return when (extension) {
            "m3u", "m3u8" -> PlaylistFormat.M3U
            "pls" -> PlaylistFormat.PLS
            else -> null
        }
    }

    private fun detectFormatByContent(firstNonBlankLine: String?): PlaylistFormat? {
        val trimmed = firstNonBlankLine?.trim() ?: return null
        return when {
            trimmed.startsWith("#EXTM3U", ignoreCase = true) -> PlaylistFormat.M3U
            trimmed.equals("[playlist]", ignoreCase = true) -> PlaylistFormat.PLS
            else -> null
        }
    }

    /**
     * Extended M3U: `#EXTINF:<duration>,<title>` followed by a URL line.
     * Plain M3U: bare URLs, one per line, no `#EXTINF`. Lines starting with
     * `#` other than `#EXTINF` are directives/comments and are skipped, as
     * are blank lines. When no `#EXTINF` title precedes a URL, the display
     * name falls back to the URL's last path segment (or the URL itself).
     */
    private fun parseM3u(lines: Sequence<String>): List<PlaylistEntry> {
        val entries = mutableListOf<PlaylistEntry>()
        var pendingTitle: String? = null
        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue
            if (line.startsWith("#EXTINF", ignoreCase = true)) {
                val commaIndex = line.indexOf(',')
                pendingTitle = if (commaIndex in 0 until line.length - 1) {
                    line.substring(commaIndex + 1).trim().takeIf { it.isNotBlank() }
                } else {
                    null
                }
                continue
            }
            if (line.startsWith("#")) continue
            entries += PlaylistEntry(name = pendingTitle ?: displayNameFromUrl(line), streamUrl = line)
            pendingTitle = null
        }
        return entries
    }

    /**
     * PLS (`.ini`-style): `File<N>`/`Title<N>` pairs keyed by index.
     * `NumberOfEntries`/`Version`/`Length<N>` and any other keys are ignored.
     * `File<N>` keys are collected directly (not driven by `NumberOfEntries`,
     * which can be missing or wrong) and sorted by index for stable, gap-safe
     * output. A missing `Title<N>` falls back the same way M3U does, for a
     * consistent naming convention across both formats.
     */
    private fun parsePls(lines: Sequence<String>): List<PlaylistEntry> {
        val fileKeyRegex = Regex("(?i)^File(\\d+)$")
        val titleKeyRegex = Regex("(?i)^Title(\\d+)$")
        val files = sortedMapOf<Int, String>()
        val titles = mutableMapOf<Int, String>()
        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue
            val eqIndex = line.indexOf('=')
            if (eqIndex <= 0) continue
            val key = line.substring(0, eqIndex).trim()
            val value = line.substring(eqIndex + 1).trim()
            if (value.isEmpty()) continue

            val fileMatch = fileKeyRegex.matchEntire(key)
            if (fileMatch != null) {
                val index = fileMatch.groupValues[1].toIntOrNull()
                if (index != null) files[index] = value
                continue
            }
            val titleMatch = titleKeyRegex.matchEntire(key)
            if (titleMatch != null) {
                val index = titleMatch.groupValues[1].toIntOrNull()
                if (index != null) titles[index] = value
            }
        }
        return files.entries.sortedBy { it.key }.map { (index, url) ->
            PlaylistEntry(name = titles[index] ?: displayNameFromUrl(url), streamUrl = url)
        }
    }

    private fun displayNameFromUrl(url: String): String {
        val withoutQueryOrFragment = url.substringBefore('?').substringBefore('#')
        val lastSegment = withoutQueryOrFragment.trimEnd('/').substringAfterLast('/')
        return lastSegment.takeIf { it.isNotBlank() } ?: url
    }
}
