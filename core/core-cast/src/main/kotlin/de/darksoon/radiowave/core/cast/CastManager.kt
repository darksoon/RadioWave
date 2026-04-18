// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.core.cast

import android.content.Context
import com.google.android.gms.cast.framework.CastContext
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CastManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun initialize() {
        runCatching {
            CastContext.getSharedInstance(context)
        }
    }
}

