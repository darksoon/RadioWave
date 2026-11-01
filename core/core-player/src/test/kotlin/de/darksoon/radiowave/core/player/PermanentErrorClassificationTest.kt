// SPDX-License-Identifier: GPL-3.0-or-later
package de.darksoon.radiowave.core.player

import androidx.media3.common.PlaybackException
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// PlaybackException reads SystemClock, hence Robolectric.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PermanentErrorClassificationTest {
    // FIX 2026-11-01 (Review P2-2)
    @Test fun `container and decoder errors are permanent only before the stream played`() {
        val malformed = PlaybackException("x", null, PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED)
        val decoding = PlaybackException("x", null, PlaybackException.ERROR_CODE_DECODING_FAILED)
        val unsupported = PlaybackException("x", null, PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED)
        val network = PlaybackException("x", null, PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED)
        assertTrue(isPermanentStreamError(malformed))
        assertTrue(isPermanentStreamError(decoding))
        assertFalse(isPermanentStreamError(malformed, streamWasReady = true))
        assertFalse(isPermanentStreamError(decoding, streamWasReady = true))
        assertTrue(isPermanentStreamError(unsupported, streamWasReady = true))
        assertFalse(isPermanentStreamError(network))
    }
}
