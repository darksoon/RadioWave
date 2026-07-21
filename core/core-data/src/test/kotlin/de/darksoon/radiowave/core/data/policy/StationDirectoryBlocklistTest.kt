// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.core.data.policy

import de.darksoon.radiowave.core.model.Station
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StationDirectoryBlocklistTest {

    private fun station(
        uuid: String = "uuid-1",
        name: String = "Some Station",
        streamUrl: String = "https://stream.example.com/live",
        homepageUrl: String? = null,
        faviconUrl: String? = null,
        country: String? = null,
        countryCode: String? = null,
    ) = Station(
        uuid = uuid,
        name = name,
        streamUrl = streamUrl,
        homepageUrl = homepageUrl,
        faviconUrl = faviconUrl,
        country = country,
        countryCode = countryCode,
    )

    @Test
    fun `blocks the known Al-Nour directory entry by uuid`() {
        val alNour = station(uuid = "03ecebf1-7164-11e9-af37-52543be04c81")
        assertTrue(StationDirectoryBlocklist.isBlocked(alNour))
    }

    @Test
    fun `blocks by organization-owned domain regardless of name`() {
        assertTrue(
            StationDirectoryBlocklist.isBlocked(
                station(name = "Renamed Station", homepageUrl = "http://www.alnour.com.lb/"),
            ),
        )
        assertTrue(
            StationDirectoryBlocklist.isBlocked(
                station(name = "Renamed", streamUrl = "https://cdn.almanar.com.lb:8000/live"),
            ),
        )
        assertTrue(
            StationDirectoryBlocklist.isBlocked(
                station(name = "Renamed", faviconUrl = "https://static.alaqsavoice.ps/logo.png"),
            ),
        )
    }

    @Test
    fun `does not block unrelated domains that merely contain a blocked string`() {
        assertFalse(
            StationDirectoryBlocklist.isBlocked(
                station(homepageUrl = "https://notalnour.com.lb.example.com/"),
            ),
        )
    }

    @Test
    fun `blocks Al-Nour by name only when scoped to Lebanon`() {
        val name = "91.7FM Al Nour Radio - 91.9 / 92.2 إذاعة النور بيروت"
        assertTrue(
            StationDirectoryBlocklist.isBlocked(
                station(name = name, country = "Lebanon", countryCode = "LB"),
            ),
        )
        // Same-name pattern outside Lebanon stays available ("Nour" is a common name).
        assertFalse(
            StationDirectoryBlocklist.isBlocked(
                station(name = "Al Nour FM", country = "Morocco", countryCode = "MA"),
            ),
        )
    }

    @Test
    fun `matches Arabic name with hamza variants folded`() {
        assertTrue(
            StationDirectoryBlocklist.isBlocked(
                station(name = "إذاعة النور", countryCode = "lb"),
            ),
        )
    }

    @Test
    fun `does not block legitimate similarly-named stations`() {
        val legit = listOf(
            station(name = "Nour Mariam TV", country = "Lebanon", countryCode = "LB"),
            station(name = "Noura FM 94.5 Bamako", country = "Mali", countryCode = "ML"),
            station(name = "Radio Manarat", country = "Morocco", countryCode = "MA"),
            station(name = "Albayan Radio", country = "Australia", countryCode = "AU"),
            station(name = "Sawt El Ghad", country = "Lebanon", countryCode = "LB"),
        )
        for (s in legit) {
            assertFalse("should not block ${s.name}", StationDirectoryBlocklist.isBlocked(s))
        }
    }

    @Test
    fun `blocks Hamas Al-Aqsa broadcaster names in any country`() {
        assertTrue(StationDirectoryBlocklist.isBlocked(station(name = "Al-Aqsa Voice Radio")))
        assertTrue(StationDirectoryBlocklist.isBlocked(station(name = "صوت الأقصى")))
    }
}
