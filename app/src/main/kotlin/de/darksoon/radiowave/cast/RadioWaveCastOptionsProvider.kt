// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.cast

import android.content.Context
import androidx.media3.cast.DefaultCastOptionsProvider
import androidx.media3.common.util.UnstableApi
import com.google.android.gms.cast.framework.CastOptions
import com.google.android.gms.cast.framework.OptionsProvider
import com.google.android.gms.cast.framework.SessionProvider
import com.google.android.gms.cast.framework.media.CastMediaOptions
import com.google.android.gms.cast.framework.media.NotificationOptions

@UnstableApi
class RadioWaveCastOptionsProvider : OptionsProvider {
    override fun getCastOptions(context: Context): CastOptions {
        val expandedControllerClassName = RadioWaveCastExpandedControllerActivity::class.java.name
        val notificationOptions = NotificationOptions.Builder()
            .setTargetActivityClassName(expandedControllerClassName)
            .build()
        val mediaOptions = CastMediaOptions.Builder()
            .setNotificationOptions(notificationOptions)
            .setExpandedControllerActivityClassName(expandedControllerClassName)
            .build()

        return CastOptions.Builder()
            .setReceiverApplicationId(DefaultCastOptionsProvider.APP_ID_DEFAULT_RECEIVER_WITH_DRM)
            .setCastMediaOptions(mediaOptions)
            .build()
    }

    override fun getAdditionalSessionProviders(context: Context): List<SessionProvider> = emptyList()
}
