// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.core.player.visualizer

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

// REA 95fcb33: komplett neue Datei.
// Fundstellen: p000/ll3.java (Klasse), p000/kl3.java (Window), p000/zo4.java (SpectrumFrame),
// p000/ba2.java:524-626 (Analyse-Koroutine, Fall 10), p000/jl3.java Fall 0 (lazy Job),
// p000/be3.java Fall 1 (AutoCloseable des Abonnements), p000/C0624q.java:478-486 (acquire).
// REA-UNSICHER: Klassen-, Methoden- und Paketnamen sind geraten (R8 hat alle entfernt);
// die Struktur (Felder, Konstanten, Algorithmus) ist sicher.

/**
 * One analysed spectrum frame: [BAND_COUNT] values in 0..1, low frequencies first.
 * Empty while the visualizer is idle (paused, disabled, nobody watching).
 */
data class SpectrumFrame(val bands: List<Float> = emptyList())

/**
 * Bridge between the audio thread ([VisualizerAudioProcessor]) and the player UI.
 *
 * Purely local: the processor copies already-decoded PCM of the running stream, there is no
 * microphone access and nothing leaves the device. Analysis only runs while
 *  - playback is running ([setPlaybackActive], driven by `onIsPlayingChanged`) **and**
 *  - at least one UI subscriber holds a handle from [acquire] (the player screen while it is
 *    visible, see `AudioVisualizerSection`).
 *
 * Every state change bumps [generation]; windows captured under an older generation are
 * dropped, so a stale spectrum never flashes up after pause/station change.
 */
@Singleton
class AudioVisualizerTap @Inject constructor() {

    /**
     * Sample window handed from the audio thread to the analyser (`kl3`).
     * REA: data class mit FloatArray — equals() vergleicht das Array per Referenz,
     * hashCode() per Inhalt; genau so steht es im Dekompilat (p000/kl3.java).
     */
    @Suppress("ArrayInDataClass")
    internal data class Window(
        val samples: FloatArray,
        val rate: Int,
        val generation: Long,
    )

    private val subscriberCount = AtomicInteger()

    @Volatile
    private var playbackActive: Boolean = false

    @Volatile
    internal var generation: Long = 0L
        private set

    private val _spectrum = MutableStateFlow(SpectrumFrame())
    val spectrum: StateFlow<SpectrumFrame> = _spectrum.asStateFlow()

    private val windows = Channel<Window>(Channel.CONFLATED)

    /** Started on the first submitted window, lives for the process (singleton). */
    private val analyzerJob: Job by lazy {
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            for (window in windows) {
                val bands = analyze(window.samples, window.rate)
                synchronized(this@AudioVisualizerTap) {
                    if (isCapturing && generation == window.generation) {
                        _spectrum.value = SpectrumFrame(bands)
                    }
                }
            }
        }
    }

    /** True while the audio processor should copy samples at all. */
    internal val isCapturing: Boolean
        get() = playbackActive && subscriberCount.get() > 0

    /**
     * Registers a visible consumer. Close the returned handle when the consumer goes away
     * (the player screen does this when it leaves STARTED). Closing twice is harmless.
     */
    fun acquire(): AutoCloseable = synchronized(this) {
        subscriberCount.incrementAndGet()
        val handle = Handle()
        AutoCloseable {
            synchronized(this) {
                if (!handle.closed) {
                    handle.closed = true
                    subscriberCount.decrementAndGet()
                    generation++
                    _spectrum.value = SpectrumFrame()
                }
            }
        }
    }

    /** Called by the player whenever `isPlaying` flips. Pausing clears the spectrum. */
    fun setPlaybackActive(active: Boolean) {
        synchronized(this) {
            playbackActive = active
            if (!active) {
                generation++
                _spectrum.value = SpectrumFrame()
            }
        }
    }

    /** Called when a new station starts: stop capturing until it is actually playing. */
    fun reset() {
        synchronized(this) {
            playbackActive = false
            generation++
            _spectrum.value = SpectrumFrame()
        }
    }

    /** Called from the audio thread with a mono window; never blocks (conflated channel). */
    internal fun submit(samples: FloatArray, sampleRate: Int) {
        if (!isCapturing) return
        analyzerJob // starts the analyser lazily
        windows.trySend(Window(samples.copyOf(), sampleRate, generation))
    }

    private class Handle {
        // REA: im Dekompilat ein eigenes Objekt (p000/z14, Feld f25504j) als "closed"-Flag.
        var closed: Boolean = false
    }

    internal companion object {
        const val BAND_COUNT = 16
        private const val MIN_FREQUENCY_HZ = 60.0
        private const val MAX_FREQUENCY_HZ = 5_000.0
        private const val NYQUIST_FRACTION = 0.45
        private const val DB_FLOOR = 60.0

        /**
         * Goertzel filter bank instead of a full FFT: [BAND_COUNT] logarithmically spaced
         * centre frequencies from 60 Hz up to min(5 kHz, 0.45 × rate), Hann window over the
         * whole block, magnitude → dBFS → mapped from −60..0 dB onto 0..1.
         */
        internal fun analyze(samples: FloatArray, rate: Int): List<Float> {
            if (samples.isEmpty() || rate <= 0) return List(BAND_COUNT) { 0f }
            val sampleRate = rate.toDouble()
            val maxFrequency = min(MAX_FREQUENCY_HZ, sampleRate * NYQUIST_FRACTION)
            val size = samples.size
            return List(BAND_COUNT) { band ->
                val frequency = MIN_FREQUENCY_HZ *
                    (maxFrequency / MIN_FREQUENCY_HZ).pow(band / (BAND_COUNT - 1).toDouble())
                val coefficient = 2.0 * cos(2.0 * PI * frequency / sampleRate)
                var s1 = 0.0
                var s2 = 0.0
                for (index in 0 until size) {
                    val raw = samples[index]
                    val sample = if (raw.isFinite()) raw.toDouble() else 0.0
                    val hann = 0.5 - 0.5 * cos(2.0 * PI * index / size)
                    val s0 = coefficient * s1 + hann * sample - s2
                    s2 = s1
                    s1 = s0
                }
                val power = max(0.0, s2 * s2 + s1 * s1 - coefficient * s1 * s2)
                val magnitude = sqrt(power) * 4.0 / size
                val db = 20.0 * log10(max(1.0e-6, magnitude))
                ((db + DB_FLOOR) / DB_FLOOR).toFloat().coerceIn(0f, 1f)
            }
        }
    }
}
