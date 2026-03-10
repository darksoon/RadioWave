// SPDX-License-Identifier: GPL-3.0-or-later

package de.radiowave.core.data.update

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build

object AppUpdateSupport {
    private const val META_DATA_IN_APP_UPDATER_ENABLED = "de.radiowave.IN_APP_UPDATER_ENABLED"

    fun isInAppUpdaterEnabled(context: Context): Boolean {
        val appInfo = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getApplicationInfo(
                    context.packageName,
                    PackageManager.ApplicationInfoFlags.of(PackageManager.GET_META_DATA.toLong()),
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
            }
        }.getOrNull() ?: return false

        return appInfo.metaData?.getBoolean(META_DATA_IN_APP_UPDATER_ENABLED, false) == true
    }
}
