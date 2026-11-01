// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.feature.player

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle

// REA 95fcb33: neue Datei.
// AudioVisualizerSection = p000/za0.java m11516d (Zeilen 1164-1330), Effekt p000/C0983zo.java,
// Schalter-Text p000/C0946yo.java Fall 0, Zeichnung Lambda "vo" in za0.java:1271-1295.
// AudioOutputButton = p000/za0.java m11528l (2863-2903), Dialoginhalt p000/C0637qc.java Fall 1,
// Texte p000/C0580ot.java Fälle 2-5, Intents p000/C0908xo.java Fälle 0/1.
// REA-UNSICHER: Funktionsnamen geraten. collectAsState vs. collectAsStateWithLifecycle ist im
// Dekompilat nicht unterscheidbar (AbstractC0067ax.m834y); hier wie im übrigen Player-Code
// collectAsStateWithLifecycle.

/** Bar colour of the spectrum (0xFFBA94FF, za0.java:1273). */
private val SpectrumBarColor = Color(0xFFBA94FF)
private const val SPECTRUM_BAR_COUNT = 32

/**
 * Spectrum toggle, audio-output shortcut and — when enabled — the live spectrum.
 *
 * Capturing only happens while all of these hold: the setting is on, thermal mode is off,
 * the stream is playing locally ([isPlaying], not [isCasting]) and the screen is at least
 * STARTED. Leaving any of these stops the analysis (the tap handle is closed).
 */
@Composable
fun AudioVisualizerSection(
    isPlaying: Boolean,
    isCasting: Boolean,
    viewModel: VisualizerViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val enabled = settings.visualizerEnabled

    LaunchedEffect(lifecycle, enabled, settings.thermalMode, isPlaying, isCasting) {
        if (enabled && !settings.thermalMode && isPlaying && !isCasting) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.keepCapturing()
            }
        }
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            TextButton(onClick = { viewModel.setVisualizerEnabled(!enabled) }) {
                Text(
                    text = stringResource(
                        if (enabled) R.string.audio_visualizer_off else R.string.audio_visualizer_on,
                    ),
                    color = Color.White,
                )
            }
            AudioOutputButton()
        }

        if (enabled) {
            val spectrum by viewModel.spectrum.collectAsStateWithLifecycle()
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
            ) {
                // Idle (paused or casting) → all bars collapse to the 1 px baseline and dim.
                val bands = if (!isPlaying || isCasting) emptyList() else spectrum.bands
                val slotWidth = size.width / SPECTRUM_BAR_COUNT
                val centerY = size.height / 2f
                for (index in 0 until SPECTRUM_BAR_COUNT) {
                    // Mirrored: lowest band in the middle, highest at both edges.
                    val band = if (index < 16) 15 - index else index - 16
                    val value = bands.getOrElse(band) { 0f }
                    val halfHeight = size.height * value * 0.46f
                    val x = (index + 0.5f) * slotWidth
                    drawLine(
                        color = SpectrumBarColor.copy(alpha = if (value > 0f) 0.9f else 0.22f),
                        start = Offset(x, centerY - halfHeight),
                        end = Offset(x, centerY + halfHeight + 1f),
                        strokeWidth = 0.55f * slotWidth,
                    )
                }
            }
            Text(
                text = stringResource(
                    if (isCasting) R.string.audio_visualizer_cast else R.string.audio_visualizer_hint,
                ),
                color = Color.White.copy(alpha = 0.65f),
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

/**
 * "Audio output" shortcut: explains that the output is chosen in Android (Cast stays in the
 * player's Cast button) and opens the system volume panel or the Bluetooth settings.
 * Also used by the car mode header.
 */
@Composable
fun AudioOutputButton() {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }

    TextButton(onClick = { showDialog = true }) {
        Text(stringResource(R.string.audio_devices))
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            title = { Text(stringResource(R.string.audio_devices)) },
            text = {
                Column {
                    Text(stringResource(R.string.audio_devices_help))
                    TextButton(onClick = { openSystemAudioControls(context) }) {
                        Text(stringResource(R.string.audio_system_controls))
                    }
                    TextButton(onClick = { openBluetoothSettings(context) }) {
                        Text(stringResource(R.string.audio_bluetooth))
                    }
                }
            },
        )
    }
}

private fun openSystemAudioControls(context: Context) {
    val action = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        "android.settings.panel.action.VOLUME" // Settings.Panel.ACTION_VOLUME
    } else {
        android.provider.Settings.ACTION_SOUND_SETTINGS
    }
    runCatching { context.startActivity(Intent(action)) }
}

private fun openBluetoothSettings(context: Context) {
    runCatching { context.startActivity(Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS)) }
}
