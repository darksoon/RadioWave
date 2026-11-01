// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.core.player

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Network type as seen by the automatic quality choice: "metered" means the mobile/
 * metered preference (`mobile_audio_quality`) applies, otherwise the Wi-Fi preference.
 *
 * REA 95fcb33: neue Klasse, p000/js4.java (nur ein Context-Feld); der Rumpf von
 * isMetered() ist in p000/C0486m9.java Fall 7 inlined. Methodenname `isMetered` ist aus
 * der Funktionsreferenz "isMetered()Z" belegt.
 * REA-UNSICHER: Klassenname geraten; @Singleton angenommen (Provider im Singleton-Switch
 * p000/C0498ml.java).
 */
@Singleton
class StreamNetworkInfo @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /**
     * True for cellular transports, for networks Android reports as metered (e.g. a
     * hotspot) and whenever the state can't be determined — unknown means "be frugal".
     */
    fun isMetered(): Boolean {
        val manager = context.getSystemService(ConnectivityManager::class.java) ?: return true
        val network = manager.activeNetwork ?: return true
        val capabilities = manager.getNetworkCapabilities(network) ?: return true
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
            manager.isActiveNetworkMetered
    }
}
