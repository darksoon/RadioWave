// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.core.data.policy

import de.darksoon.radiowave.core.model.Station
import java.io.InputStream

/**
 * Single validation gate for stations that enter the app from user input rather than
 * from the directory API: the manual "add custom station" dialog, M3U/PLS playlist
 * import and the favorites JSON import. All three used to carry their own (diverging)
 * checks — the JSON import only rejected blank values.
 *
 * Pure JVM code (no `android.net.Uri`) so it is unit-testable without Robolectric.
 */
object StationInputValidator {

    /** Import files above this size are rejected before parsing. */
    const val MAX_IMPORT_BYTES: Int = 2 * 1024 * 1024

    /** Entries beyond this count are skipped (reported as skipped, not silently dropped). */
    const val MAX_IMPORT_ENTRIES: Int = 5_000

    const val MAX_URL_LENGTH: Int = 2_048
    const val MAX_NAME_LENGTH: Int = 200
    const val MAX_UUID_LENGTH: Int = 128
    const val MAX_FIELD_LENGTH: Int = 100
    const val MAX_TAG_LENGTH: Int = 64
    const val MAX_TAGS: Int = 50

    sealed interface Result {
        /** [station] is the sanitized copy that may be stored. */
        data class Valid(val station: Station) : Result

        /** Not playable/storable (bad URL, missing id, ...). */
        data object Invalid : Result

        /** Rejected by the content policy ([StationDirectoryBlocklist]). */
        data object Blocked : Result
    }

    /**
     * True for an http(s) URL with a non-blank host, no control characters and at most
     * [MAX_URL_LENGTH] characters. Surrounding whitespace is ignored.
     */
    fun isValidStreamUrl(url: String): Boolean {
        val trimmed = url.trim()
        if (trimmed.isEmpty() || trimmed.length > MAX_URL_LENGTH) return false
        if (trimmed.any { it.isISOControl() }) return false
        val separator = trimmed.indexOf("://")
        if (separator <= 0) return false
        val scheme = trimmed.substring(0, separator).lowercase()
        if (scheme != "http" && scheme != "https") return false
        val authority = trimmed.substring(separator + 3)
            .substringBefore('/')
            .substringBefore('?')
            .substringBefore('#')
            .substringAfterLast('@') // strip userinfo
        val host: String
        val port: String?
        if (authority.startsWith('[')) {
            // IPv6 literal: [::1]:8000
            val end = authority.indexOf(']')
            if (end < 0) return false
            host = authority.substring(1, end)
            port = authority.substring(end + 1).takeIf { it.isNotEmpty() }?.removePrefix(":")
        } else {
            host = authority.substringBefore(':')
            port = if (':' in authority) authority.substringAfter(':') else null
        }
        if (host.isBlank() || host.any { it.isWhitespace() }) return false
        if (port != null && port.isNotEmpty()) {
            val number = port.toIntOrNull() ?: return false
            if (number !in 1..65535) return false
        }
        return true
    }

    /**
     * Validates and sanitizes [station]: trims all strings, enforces the length limits
     * (the stream URL and uuid must fit, descriptive fields are truncated or dropped),
     * drops non-http(s) homepage/favicon URLs and finally applies the content policy.
     */
    fun validate(station: Station): Result {
        val uuid = station.uuid.trim()
        if (uuid.isEmpty() || uuid.length > MAX_UUID_LENGTH || uuid.any { it.isISOControl() }) {
            return Result.Invalid
        }
        val streamUrl = station.streamUrl.trim()
        if (!isValidStreamUrl(streamUrl)) return Result.Invalid

        val sanitized = station.copy(
            uuid = uuid,
            name = cleanText(station.name, MAX_NAME_LENGTH) ?: streamUrl.take(MAX_NAME_LENGTH),
            streamUrl = streamUrl,
            homepageUrl = station.homepageUrl?.trim()?.takeIf(::isValidStreamUrl),
            faviconUrl = station.faviconUrl?.trim()?.takeIf(::isValidStreamUrl),
            country = cleanText(station.country, MAX_FIELD_LENGTH),
            countryCode = cleanText(station.countryCode, MAX_FIELD_LENGTH),
            language = cleanText(station.language, MAX_FIELD_LENGTH),
            codec = cleanText(station.codec, MAX_FIELD_LENGTH),
            tags = station.tags.mapNotNull { cleanText(it, MAX_TAG_LENGTH) }.distinct().take(MAX_TAGS),
            bitrate = station.bitrate?.takeIf { it >= 0 },
        )
        // Check the raw input too: a blocked homepage/favicon host must not slip through
        // just because sanitizing dropped that field.
        if (StationDirectoryBlocklist.isBlocked(station) || StationDirectoryBlocklist.isBlocked(sanitized)) {
            return Result.Blocked
        }
        return Result.Valid(sanitized)
    }

    /**
     * Reads [input] as UTF-8 text, returning null if it is larger than [maxBytes] — the
     * limit is enforced while reading, so an oversized file is never fully buffered.
     * Does not close [input].
     */
    fun readBoundedText(input: InputStream, maxBytes: Int = MAX_IMPORT_BYTES): String? {
        val buffer = ByteArray(8 * 1024)
        val out = java.io.ByteArrayOutputStream()
        var total = 0
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            total += read
            if (total > maxBytes) return null
            out.write(buffer, 0, read)
        }
        return out.toString(Charsets.UTF_8.name())
    }

    /** Trimmed, control characters removed, truncated to [maxLength]; null if blank. */
    private fun cleanText(value: String?, maxLength: Int): String? {
        if (value == null) return null
        val cleaned = value.filterNot { it.isISOControl() }.trim()
        return cleaned.take(maxLength).trim().ifEmpty { null }
    }
}
