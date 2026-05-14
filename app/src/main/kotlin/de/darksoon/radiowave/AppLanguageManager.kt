// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import de.darksoon.radiowave.core.data.repository.SettingsRepository
import de.darksoon.radiowave.core.model.AppSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Applies the user-selected app language via [AppCompatDelegate.setApplicationLocales].
 *
 * Reads the current language synchronously from [SettingsRepository] on app start —
 * required because [Application.onCreate] must finish before any locale-dependent
 * resource is loaded.
 */
@Singleton
class AppLanguageManager @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    /** Read the persisted language synchronously and apply it. Safe to call from [Application.onCreate]. */
    fun applyPersistedLanguage() {
        val language = runBlocking { settingsRepository.appLanguage.first() }
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
