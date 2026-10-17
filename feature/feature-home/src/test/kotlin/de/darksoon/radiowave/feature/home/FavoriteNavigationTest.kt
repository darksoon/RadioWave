// SPDX-License-Identifier: GPL-3.0-or-later
package de.darksoon.radiowave.feature.home

import de.darksoon.radiowave.core.model.Station
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FavoriteNavigationTest {
    private fun station(id: String, url: String = "https://example.test/$id") = Station(uuid = id, name = id, streamUrl = url)
    private val a = station("a")
    private val b = station("b")
    private val c = station("c")

    @Test fun `steps through favorites in their order`() {
        assertEquals(b, adjacentFavorite(listOf(a, b, c), "a", +1))
        assertEquals(a, adjacentFavorite(listOf(a, b, c), "b", -1))
    }

    @Test fun `wraps around at both ends`() {
        assertEquals(a, adjacentFavorite(listOf(a, b, c), "c", +1))
        assertEquals(c, adjacentFavorite(listOf(a, b, c), "a", -1))
    }

    @Test fun `from a non-favorite, next is the first and previous the last favorite`() {
        assertEquals(a, adjacentFavorite(listOf(a, b, c), "elsewhere", +1))
        assertEquals(c, adjacentFavorite(listOf(a, b, c), null, -1))
    }

    @Test fun `nothing to skip to`() {
        assertNull(adjacentFavorite(emptyList(), "a", +1))
        assertNull(adjacentFavorite(listOf(a), "a", +1))
    }

    @Test fun `favorites without a stream are skipped`() {
        val broken = station("broken", url = " ")
        assertEquals(c, adjacentFavorite(listOf(a, broken, c), "a", +1))
    }
}
