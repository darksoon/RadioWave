// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.core.data.policy

import de.darksoon.radiowave.core.model.Station

/**
 * Content-policy blocklist for the station directory.
 *
 * Radio Browser is an open, community-maintained directory and contains stations
 * operated by organizations designated as terrorist by the US, the EU, or Germany
 * (e.g. Hezbollah's Radio Al-Nour — the subject of a Google Play "Violent
 * Extremism" enforcement against this app in July 2026). Distributing their
 * broadcasts violates Google Play policy and sanctions law, so these stations are
 * removed from every directory surface: search (exact and fuzzy), browse lists,
 * station variants, similar stations, the local mirror sync, and — via
 * [blockedUuids]-based purging — data already cached on users' devices.
 *
 * Matching is deliberately layered so re-added or renamed directory entries stay
 * blocked without an app update being able to rely on UUIDs alone:
 *  1. exact station UUIDs of known entries,
 *  2. web domains owned by the designated organizations (checked against stream,
 *     homepage, and favicon URLs),
 *  3. normalized name patterns (diacritics stripped, so Arabic hamza variants
 *     match), optionally scoped by country to avoid false positives — "Nour"
 *     ("light") and "Manar" ("lighthouse") are common, legitimate station names.
 */
object StationDirectoryBlocklist {

    /** Known directory entries — also used to purge already-cached local data. */
    val blockedUuids: Set<String> = setOf(
        // "91.7FM Al Nour Radio" (Lebanon) — Hezbollah's radio station, US-designated
        // SDGT since 2006 as part of the Lebanese Media Group.
        "03ecebf1-7164-11e9-af37-52543be04c81",
    )

    // Domains owned by designated organizations. Suffix match on the host of
    // stream/homepage/favicon URLs.
    private val blockedHostSuffixes = listOf(
        "alnour.com.lb", // Radio Al-Nour (Hezbollah)
        "almanar.com.lb", // Al-Manar TV (Hezbollah)
        "manartv.com.lb", // Al-Manar TV (Hezbollah)
        "alaqsavoice.ps", // Al-Aqsa Voice Radio (Hamas)
        "aqsatv.ps", // Al-Aqsa TV (Hamas)
        "andalus.so", // Radio Al-Andalus (al-Shabaab)
    )

    private data class NameRule(
        /** Matched as substring of the normalized station name. */
        val needle: String,
        /** null = any country; otherwise lowercase ISO codes or country-name needles. */
        val countries: Set<String>? = null,
    )

    private val lebanonOnly = setOf("lb", "lebanon", "libanon")

    private val nameRules = listOf(
        // Radio Al-Nour (Hezbollah) — Lebanon-scoped: "Nour" is a common legitimate
        // station name elsewhere (Noursat, Noura FM Mali, ...).
        NameRule("al nour", lebanonOnly),
        NameRule("alnour", lebanonOnly),
        NameRule("اذاعة النور", lebanonOnly),
        // Al-Manar TV (Hezbollah) — Lebanon-scoped to spare e.g. Radio Manarat (MA).
        NameRule("al manar", lebanonOnly),
        NameRule("المنار", lebanonOnly),
        // Al-Aqsa Voice / Al-Aqsa TV (Hamas) — scoped to the broadcaster names, not
        // the mosque name alone (legitimate Quran stations reference the mosque).
        NameRule("aqsa voice"),
        NameRule("sawt al aqsa"),
        NameRule("صوت الاقصى"),
        NameRule("al aqsa tv"),
        // Radio Al-Andalus (al-Shabaab, Somalia).
        NameRule("radio andalus", setOf("so", "somalia")),
    )

    fun isBlocked(station: Station): Boolean {
        if (station.uuid in blockedUuids) return true
        if (hasBlockedHost(station.streamUrl) ||
            hasBlockedHost(station.homepageUrl) ||
            hasBlockedHost(station.faviconUrl)
        ) {
            return true
        }
        val name = normalize(station.name)
        if (name.isEmpty()) return false
        val countryCode = station.countryCode?.trim()?.lowercase().orEmpty()
        val country = normalize(station.country.orEmpty())
        return nameRules.any { rule ->
            name.contains(rule.needle) && (
                rule.countries == null ||
                    countryCode in rule.countries ||
                    rule.countries.any { it.length > 2 && country.contains(it) }
                )
        }
    }

    private fun hasBlockedHost(url: String?): Boolean {
        val host = hostOf(url) ?: return false
        return blockedHostSuffixes.any { host == it || host.endsWith(".$it") }
    }

    private fun hostOf(url: String?): String? {
        if (url.isNullOrBlank()) return null
        val afterScheme = url.substringAfter("://", missingDelimiterValue = "")
        if (afterScheme.isEmpty()) return null
        return afterScheme
            .substringBefore('/')
            .substringBefore('?')
            .substringAfterLast('@') // strip userinfo
            .substringBefore(':') // strip port
            .trim()
            .lowercase()
            .ifEmpty { null }
    }

    /**
     * lowercase, NFD-decompose, drop combining marks (this also folds Arabic hamza
     * carriers: إذاعة → اذاعة), collapse everything non-alphanumeric to single spaces.
     */
    private fun normalize(value: String): String {
        val decomposed = java.text.Normalizer.normalize(value.lowercase(), java.text.Normalizer.Form.NFD)
        val sb = StringBuilder(decomposed.length)
        var lastWasSpace = true
        for (c in decomposed) {
            when {
                c.isLetterOrDigit() -> {
                    sb.append(c)
                    lastWasSpace = false
                }
                Character.getType(c) == Character.NON_SPACING_MARK.toInt() -> Unit
                else -> {
                    if (!lastWasSpace) {
                        sb.append(' ')
                        lastWasSpace = true
                    }
                }
            }
        }
        return sb.toString().trim()
    }
}
