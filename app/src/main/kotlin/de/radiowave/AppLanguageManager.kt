package de.radiowave

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import de.radiowave.core.model.AppSettings

object AppLanguageManager {
    fun applyFromPrefs(context: Context) {
        val prefs = context.getSharedPreferences(AppSettings.PREFS_NAME, Context.MODE_PRIVATE)
        val language = prefs.getString(AppSettings.KEY_APP_LANGUAGE, AppSettings.LANGUAGE_SYSTEM)
            ?: AppSettings.LANGUAGE_SYSTEM
        applyLanguage(language)
    }

    fun applyLanguage(language: String) {
        val locales = when (language) {
            AppSettings.LANGUAGE_DE -> LocaleListCompat.forLanguageTags("de")
            AppSettings.LANGUAGE_EN -> LocaleListCompat.forLanguageTags("en")
            else -> LocaleListCompat.getEmptyLocaleList()
        }
        AppCompatDelegate.setApplicationLocales(locales)
    }
}
