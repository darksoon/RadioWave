// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.core.player.visualizer

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder

// REA 95fcb33: komplett neue Datei. Fundstelle: p000/ap4.java (extends BaseAudioProcessor = rt),
// Einbau über den RenderersFactory in PlayerControllerImpl (p000/qq2.java:514-574, Zeile 531).
// REA-UNSICHER: Klassenname geraten; Logik und Konstanten sicher.

/**
 * Pass-through [BaseAudioProcessor] that taps the decoded PCM for [AudioVisualizerTap].
 *
 * The audio itself is never modified: every buffer is copied unchanged to the output.
 * While capturing, the processor down-mixes to mono, keeps every 4th frame (so the effective
 * analysis rate is sampleRate / 4) and collects [WINDOW_SIZE] samples. A full window is
 * published at most every [MIN_PUBLISH_INTERVAL_NS] (≈ 15 fps); windows in between are
 * dropped. Only 16-bit PCM and float PCM are tapped — other encodings make the processor
 * inactive (Media3 then bypasses it).
 */
@UnstableApi
internal class VisualizerAudioProcessor(
    private val tap: AudioVisualizerTap,
) : BaseAudioProcessor() {

    private val window = FloatArray(WINDOW_SIZE)
    private var windowFill = 0
    private var frameCounter = 0
    private var lastPublishAtNs = 0L

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        val encoding = inputAudioFormat.encoding
        return if (encoding == C.ENCODING_PCM_16BIT || encoding == C.ENCODING_PCM_FLOAT) {
            inputAudioFormat
        } else {
            AudioProcessor.AudioFormat.NOT_SET
        }
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!inputBuffer.hasRemaining()) return
        if (!tap.isCapturing) {
            windowFill = 0
            frameCounter = 0
        } else {
            captureSamples(inputBuffer.duplicate().order(ByteOrder.nativeOrder()))
        }
        // Pass-through: the stream is played exactly as decoded.
        val output = replaceOutputBuffer(inputBuffer.remaining())
        output.put(inputBuffer)
        output.flip()
    }

    private fun captureSamples(buffer: ByteBuffer) {
        val format = inputAudioFormat
        val isFloat = format.encoding == C.ENCODING_PCM_FLOAT
        val bytesPerSample = if (isFloat) 4 else 2
        val channelCount = format.channelCount
        val frameSize = channelCount * bytesPerSample
        if (frameSize <= 0) return
        while (buffer.remaining() >= frameSize) {
            val framePosition = buffer.position()
            if (frameCounter++ % DECIMATION == 0) {
                var sum = 0f
                for (channel in 0 until channelCount) {
                    val offset = framePosition + channel * bytesPerSample
                    sum += if (isFloat) {
                        buffer.getFloat(offset)
                    } else {
                        buffer.getShort(offset) / 32_768f
                    }
                }
                window[windowFill++] = sum / channelCount
                if (windowFill == window.size) {
                    val now = System.nanoTime()
                    if (now - lastPublishAtNs >= MIN_PUBLISH_INTERVAL_NS) {
                        tap.submit(window, format.sampleRate / DECIMATION)
                        lastPublishAtNs = now
                    }
                    windowFill = 0
                }
            }
            buffer.position(framePosition + frameSize)
        }
    }

    // REA: ap4.mo696j = parameterloses onFlush(), das BaseAudioProcessor.onFlush(StreamMetadata)
    // standardmäßig aufruft (p000/AbstractC0691rt.mo2209k).
    @Suppress("OVERRIDE_DEPRECATION")
    override fun onFlush() {
        windowFill = 0
        frameCounter = 0
        lastPublishAtNs = 0L
    }

    private companion object {
        const val WINDOW_SIZE = 512
        const val DECIMATION = 4
        const val MIN_PUBLISH_INTERVAL_NS = 66_000_000L
    }
}
