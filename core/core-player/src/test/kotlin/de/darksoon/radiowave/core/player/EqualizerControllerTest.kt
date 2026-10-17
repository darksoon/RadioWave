// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.core.player

import de.darksoon.radiowave.core.model.AppSettings
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EqualizerControllerTest {
    private val controller = EqualizerController()

    @After fun tearDown() = controller.release()

    @Test
    fun `flat preset attaches no effect to the session`() {
        controller.attach(SESSION_ID)

        assertFalse(controller.isAttached)
    }

    @Test
    fun `choosing a sound preset later attaches to the remembered session`() {
        controller.attach(SESSION_ID)

        controller.applyPreset(AppSettings.EQ_PRESET_BASS_BOOST)

        assertTrue(controller.isAttached)
    }

    @Test
    fun `switching back to flat removes the effect again`() {
        controller.applyPreset(AppSettings.EQ_PRESET_VOCAL)
        controller.attach(SESSION_ID)
        assertTrue(controller.isAttached)

        controller.applyPreset(AppSettings.EQ_PRESET_FLAT)

        assertFalse(controller.isAttached)
    }

    @Test
    fun `preset change after player teardown does not attach to the dead session`() {
        controller.attach(SESSION_ID)
        controller.release()

        controller.applyPreset(AppSettings.EQ_PRESET_TREBLE_BOOST)

        assertFalse(controller.isAttached)
    }

    @Test
    fun `unset session id never attaches`() {
        controller.applyPreset(AppSettings.EQ_PRESET_BASS_BOOST)
        controller.attach(0)

        assertFalse(controller.isAttached)
    }

    private companion object {
        const val SESSION_ID = 42
    }
}
