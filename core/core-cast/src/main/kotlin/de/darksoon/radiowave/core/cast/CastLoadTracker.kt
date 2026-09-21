// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.core.cast

/**
 * Bookkeeping for station loads on the Cast receiver, kept free of Cast SDK types so the
 * decisions are unit-testable. Not thread-safe — [CastManager] only touches it on the main
 * thread (Cast SDK requirement).
 *
 * A station only counts as loaded once the receiver confirmed the load. A failed load is
 * not retried on every player-state emission (metadata ticks would hammer the receiver),
 * only when the local player starts loading that station again after having settled (the
 * user re-tapped it, or a reconnect), a different station is picked, or a new/resumed
 * session forces a load.
 */
internal class CastLoadTracker {
    private var loadedStationUuid: String? = null
    private var failedStationUuid: String? = null
    private var pendingStationUuid: String? = null
    private var pendingToken = 0L
    private var nextToken = 0L
    // Edge detection for "local player restarted": armed once the local player was seen
    // not loading after the last failure, so the loading phase that was already running
    // (or the fallback resume right after the failure) doesn't immediately retry.
    private var retryArmed = false

    /** True while the last finished load failed — playback is not on the receiver. */
    var lastLoadFailed: Boolean = false
        private set

    fun shouldLoad(stationUuid: String, force: Boolean, isLocalLoading: Boolean): Boolean {
        if (!isLocalLoading) retryArmed = true
        if (force) return true
        if (stationUuid == pendingStationUuid || stationUuid == loadedStationUuid) return false
        if (stationUuid == failedStationUuid) return isLocalLoading && retryArmed
        return true
    }

    /** Returns a token to hand back to [onLoadResult]; supersedes any load still in flight. */
    fun onLoadRequested(stationUuid: String): Long {
        pendingStationUuid = stationUuid
        retryArmed = false
        pendingToken = ++nextToken
        return pendingToken
    }

    /**
     * Records the receiver's answer. Returns false when the result is stale (a newer load
     * was requested meanwhile, or the session was reset) and must be ignored by the caller.
     */
    fun onLoadResult(token: Long, success: Boolean): Boolean {
        if (pendingStationUuid == null || token != pendingToken) return false
        val stationUuid = pendingStationUuid
        pendingStationUuid = null
        lastLoadFailed = !success
        if (success) {
            loadedStationUuid = stationUuid
            failedStationUuid = null
        } else {
            // The receiver's state is unknown after a failed load — nothing counts as loaded.
            loadedStationUuid = null
            failedStationUuid = stationUuid
            // Only a loading phase that starts after this answer counts as a restart.
            retryArmed = false
        }
        return true
    }

    fun reset() {
        loadedStationUuid = null
        failedStationUuid = null
        pendingStationUuid = null
        retryArmed = false
        lastLoadFailed = false
    }
}
