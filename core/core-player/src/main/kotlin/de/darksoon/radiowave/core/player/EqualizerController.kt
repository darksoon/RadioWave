// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.core.player

import android.media.audiofx.Equalizer
import android.util.Log
import androidx.annotation.VisibleForTesting
import de.darksoon.radiowave.core.model.AppSettings
import kotlin.math.roundToInt

/**
 * Thin wrapper around the platform [Equalizer] audio effect.
 *
 * Audio effects are a notoriously flaky API surface across OEM skins — some
 * devices throw on [Equalizer] construction, report absurd band counts, or
 * simply don't support the effect at all. Every platform call here is
 * defensive: on any failure we log a warning and leave EQ inactive rather
 * than letting a broken effect take playback down with it.
 *
 * The effect is only inserted while a preset actually changes the sound. With
 * Flat (the default) no [Equalizer] exists at all: an enabled-but-flat effect
 * adds nothing audible, yet it still sits in the platform effect chain — and
 * on some OEM audio stacks (reported: Samsung with Bluetooth earbuds) that
 * chain is exactly what goes silent.
 *
 * Not a `@Singleton` / Hilt-managed type — [PlayerControllerImpl] owns a
 * single instance directly and drives its lifecycle (attach on session
 * change, release on player teardown).
 */
internal class EqualizerController {

    private var equalizer: Equalizer? = null

    /** Session of the live player — kept so a later preset change can attach on demand. */
    private var audioSessionId: Int = 0

    /** Last preset requested via [applyPreset] — re-applied on the next [attach]. */
    private var currentPreset: String = AppSettings.EQ_PRESET_FLAT

    /** True while an [Equalizer] is inserted into the session. */
    @get:VisibleForTesting
    internal val isAttached: Boolean get() = equalizer != null

    /**
     * Remembers [audioSessionId] and (re)creates the [Equalizer] for it when
     * [currentPreset] needs one.
     *
     * A session id of `0` (Media3/AudioTrack's "unset" sentinel) is skipped —
     * there's no session to attach to yet.
     */
    fun attach(audioSessionId: Int) {
        releaseEffect()
        this.audioSessionId = audioSessionId
        syncEffect()
    }

    /**
     * Applies [preset] (one of [AppSettings]'s `EQ_PRESET_*` constants): inserts
     * the effect if it's now needed, removes it again for Flat. The preset is
     * remembered even without a session, so the next [attach] picks it up.
     */
    fun applyPreset(preset: String) {
        currentPreset = preset
        syncEffect()
    }

    private fun syncEffect() {
        if (!presetNeedsEffect(currentPreset) || audioSessionId == 0) {
            releaseEffect()
            return
        }
        if (equalizer == null) {
            equalizer = try {
                Equalizer(EQUALIZER_PRIORITY, audioSessionId).apply { enabled = true }
            } catch (error: RuntimeException) {
                logWarning("Unable to create Equalizer for session $audioSessionId: ${error.message}")
                null
            }
        }
        applyBandGains(currentPreset)
    }

    private fun presetNeedsEffect(preset: String): Boolean = preset in EFFECTIVE_PRESETS

    /**
     * Player teardown: releases the [Equalizer] and forgets the session, which dies
     * with the player — a preset change before the next [attach] must not touch it.
     */
    fun release() {
        audioSessionId = 0
        releaseEffect()
    }

    /** Releases the underlying [Equalizer], guarding against platform flakiness. */
    private fun releaseEffect() {
        val current = equalizer ?: return
        equalizer = null
        try {
            current.enabled = false
            current.release()
        } catch (error: RuntimeException) {
            logWarning("Equalizer release failed: ${error.message}")
        }
    }

    private fun applyBandGains(preset: String) {
        val eq = equalizer ?: return
        try {
            val numberOfBands = eq.numberOfBands.toInt()
            if (numberOfBands <= 0) return
            val range = eq.bandLevelRange
            if (range.size < 2) return
            val minLevel = range[0].toInt()
            val maxLevel = range[1].toInt()
            if (maxLevel <= minLevel) return

            for (band in 0 until numberOfBands) {
                val level = computeBandLevel(preset, band, numberOfBands, minLevel, maxLevel)
                eq.setBandLevel(band.toShort(), level.toShort())
            }
        } catch (error: RuntimeException) {
            logWarning("Applying EQ preset '$preset' failed: ${error.message}")
        }
    }

    /**
     * Computes the band level (millibels) for [band] under [preset], scaled to
     * the device-reported [minLevel]/[maxLevel] range — devices report wildly
     * different band counts (as few as 4-5, or 10+), so this reads the count
     * at runtime rather than assuming a fixed layout.
     *
     * Deliberately not using [Equalizer.usePreset] — OEM-provided preset
     * indices/names aren't guaranteed to exist or mean the same thing across
     * devices.
     */
    private fun computeBandLevel(
        preset: String,
        band: Int,
        numberOfBands: Int,
        minLevel: Int,
        maxLevel: Int,
    ): Int {
        // Number of bands at each edge of the spectrum treated as "bass"/"treble" —
        // 2 for typical 5+ band EQs, shrinking to 1 on devices reporting very few bands.
        val edgeBandCount = if (numberOfBands <= 4) 1 else 2
        val isLowEdge = band < edgeBandCount
        val isHighEdge = band >= numberOfBands - edgeBandCount

        return when (preset) {
            AppSettings.EQ_PRESET_BASS_BOOST ->
                if (isLowEdge) (maxLevel * BOOST_FRACTION).roundToInt() else 0

            AppSettings.EQ_PRESET_TREBLE_BOOST ->
                if (isHighEdge) (maxLevel * BOOST_FRACTION).roundToInt() else 0

            AppSettings.EQ_PRESET_VOCAL -> when {
                isLowEdge || isHighEdge -> (minLevel * VOCAL_CUT_FRACTION).roundToInt()
                else -> (maxLevel * VOCAL_BOOST_FRACTION).roundToInt()
            }

            // Flat/Normal (and any unrecognized preset) — no adjustment.
            else -> 0
        }
    }

    private fun logWarning(message: String) {
        Log.w(LOG_TAG, message)
    }

    private companion object {
        const val LOG_TAG = "RadioWaveEqualizer"

        // AudioEffect priority — 0 is the default/neutral priority used elsewhere
        // in the platform APIs when there's no need to outrank other effects.
        const val EQUALIZER_PRIORITY = 0

        // Presets that change the sound — everything else (Flat, unknown values)
        // runs without an effect in the chain.
        val EFFECTIVE_PRESETS = setOf(
            AppSettings.EQ_PRESET_BASS_BOOST,
            AppSettings.EQ_PRESET_TREBLE_BOOST,
            AppSettings.EQ_PRESET_VOCAL,
        )

        // Fractions of the available positive/negative gain range applied per
        // preset — kept comfortably below the device max so the effect is
        // noticeable without clipping or sounding harsh on most hardware.
        const val BOOST_FRACTION = 0.65f
        const val VOCAL_BOOST_FRACTION = 0.35f
        const val VOCAL_CUT_FRACTION = 0.30f
    }
}
