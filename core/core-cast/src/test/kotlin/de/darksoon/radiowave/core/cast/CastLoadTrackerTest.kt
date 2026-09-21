// SPDX-License-Identifier: GPL-3.0-or-later
package de.darksoon.radiowave.core.cast

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CastLoadTrackerTest {
    private val tracker = CastLoadTracker()

    @Test fun `station counts as loaded only after the receiver confirmed it`() {
        assertTrue(tracker.shouldLoad("a", force = false, isLocalLoading = true))
        val token = tracker.onLoadRequested("a")
        // In flight: further state emissions must not fire a second load.
        assertFalse(tracker.shouldLoad("a", force = false, isLocalLoading = true))
        assertTrue(tracker.onLoadResult(token, success = true))
        assertFalse(tracker.lastLoadFailed)
        assertFalse(tracker.shouldLoad("a", force = false, isLocalLoading = false))
    }

    @Test fun `failed load is not retried on every emission but after a local restart`() {
        val token = tracker.onLoadRequested("a")
        assertTrue(tracker.onLoadResult(token, success = false))
        assertTrue(tracker.lastLoadFailed)
        // Metadata ticks / the fallback resume right after the failure: no retry.
        assertFalse(tracker.shouldLoad("a", force = false, isLocalLoading = true))
        assertFalse(tracker.shouldLoad("a", force = false, isLocalLoading = false))
        assertFalse(tracker.shouldLoad("a", force = false, isLocalLoading = false))
        // User starts the station again -> local player loads -> retry.
        assertTrue(tracker.shouldLoad("a", force = false, isLocalLoading = true))
    }

    @Test fun `another station or a new session loads despite an earlier failure`() {
        tracker.onLoadResult(tracker.onLoadRequested("a"), success = false)
        assertTrue(tracker.shouldLoad("b", force = false, isLocalLoading = true))
        assertTrue(tracker.shouldLoad("a", force = true, isLocalLoading = false))
        tracker.reset()
        assertFalse(tracker.lastLoadFailed)
        assertTrue(tracker.shouldLoad("a", force = false, isLocalLoading = false))
    }

    @Test fun `stale results are ignored`() {
        val first = tracker.onLoadRequested("a")
        val second = tracker.onLoadRequested("b")
        assertFalse(tracker.onLoadResult(first, success = false))
        assertFalse(tracker.lastLoadFailed)
        assertTrue(tracker.onLoadResult(second, success = true))
        assertFalse(tracker.shouldLoad("b", force = false, isLocalLoading = false))

        val afterReset = tracker.onLoadRequested("c")
        tracker.reset()
        assertFalse(tracker.onLoadResult(afterReset, success = true))
    }

    @Test fun `success after a failure clears the failed state`() {
        tracker.onLoadResult(tracker.onLoadRequested("a"), success = false)
        tracker.onLoadResult(tracker.onLoadRequested("a"), success = true)
        assertFalse(tracker.lastLoadFailed)
        assertFalse(tracker.shouldLoad("a", force = false, isLocalLoading = true))
    }
}
