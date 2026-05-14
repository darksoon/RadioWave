// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import de.darksoon.radiowave.core.model.AppSettings
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Applies the user-selected app language via [AppCompatDelegate.setApplicationLocales].
 *
 * Reads the language from the legacy SharedPreferences file directly on app start
 * to avoid blocking [Application.onCreate] on a DataStore read that could trigger
 * the SharedPreferences → DataStore migration. The migration runs synchronously on
 * first DataStore access, which can take long enough to ANR on slow devices.
 *
 * After the application is up, MainActivity collects [SettingsRepository.appLanguage]
 * via a `LaunchedEffect` and calls [applyLanguage] on changes, so DataStore writes
 * from Settings are still honoured at runtime.
 */
@Singleton
class AppLanguageManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /** Read the persisted language synchronously and apply it. Safe to call from [Application.onCreate]. */
    fun applyPersistedLanguage() {
        // Reading from the legacy file is safe even after the DataStore migration:
        // the migration only deletes the .xml after the FIRST DataStore read, which
        // hasn't happened yet at this point. On second+ launches after migration,
        // this returns null/default and the MainActivity LaunchedEffect picks up
        // the real value from DataStore as soon as it emits.
        val language = runCatching {
            context.getSharedPreferences(AppSettings.PREFS_NAME, Context.MODE_PRIVATE)
                .getString(AppSettings.KEY_APP_LANGUAGE, AppSettings.LANGUAGE_SYSTEM)
                ?: AppSettings.LANGUAGE_SYSTEM
        }.getOrDefault(AppSettings.LANGUAGE_SYSTEM)
        applyLanguage(language)
    }

    companion object {
        /** Apply the given language token without touching persistent storage. */
        fun applyLanguage(language: String) {
            val locales = when (language) {
                AppSettings.LANGUAGE_DE -> LocaleListCompat.forLanguageTags("de")
                AppSettings.LANGUAGE_EN -> LocaleListCompat.forLanguageTags("en")
                else -> LocaleListCompat.getEmptyLocaleList()
            }
            AppCompatDelegate.setApplicationLocales(locales)
        }
    }
}
