// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.core.data.repository

import android.content.Context
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import de.darksoon.radiowave.core.model.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Snapshot of all RadioWave settings — exposed as a single Flow for consumers
 * who need multiple values at once (e.g. PlayerControllerImpl).
 *
 * Individual Flow<T> properties on [SettingsRepository] derive from this state
 * with [distinctUntilChanged] so a UI listening only to `themeMode` doesn't
 * recompose when an unrelated setting changes.
 */
data class AppSettingsState(
    val themeMode: String = AppSettings.THEME_SYSTEM,
    val appLanguage: String = AppSettings.LANGUAGE_SYSTEM,
    val dynamicColors: Boolean = false,
    val showMiniplayerMetadata: Boolean = true,
    val keepScreenOnFullscreen: Boolean = false,
    val showQuickToasts: Boolean = true,
    val showInsecureStreams: Boolean = true,
    val notificationShowPlayPause: Boolean = true,
    val notificationShowStop: Boolean = true,
    val notificationShowPrevious: Boolean = true,
    val notificationShowNext: Boolean = true,
    val defaultAudioQuality: String = AppSettings.QUALITY_AUTO,
    val allowMobileData: Boolean = true,
    val bufferProfile: String = AppSettings.BUFFER_MEDIUM,
    val timeshiftGuard: Boolean = true,
    val thermalMode: Boolean = false,
    val autoPlayOnAndroidAutoConnect: Boolean = true,
    val limitAndroidAutoQuality: Boolean = true,
    val confirmRemoveFavorite: Boolean = false,
    val firstRunOnboardingDone: Boolean = false,
    val lastStationUuid: String? = null,
    val lastStationName: String? = null,
    val lastStationStreamUrl: String? = null,
    val lastStationFaviconUrl: String? = null,
    val lastStationCountry: String? = null,
) {
    companion object {
        val DEFAULTS = AppSettingsState()
    }
}

// Top-level DataStore property — automatically migrates from the legacy
// SharedPreferences file on first access. After migration the original
// .xml is deleted; data is preserved.
private val Context.settingsDataStore by preferencesDataStore(
    name = "settings",
    produceMigrations = { ctx -> listOf(SharedPreferencesMigration(ctx, AppSettings.PREFS_NAME)) },
)

@Singleton
open class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey(AppSettings.KEY_THEME_MODE)
        val APP_LANGUAGE = stringPreferencesKey(AppSettings.KEY_APP_LANGUAGE)
        val DYNAMIC_COLORS = booleanPreferencesKey(AppSettings.KEY_DYNAMIC_COLORS)
        val SHOW_MINIPLAYER_METADATA = booleanPreferencesKey(AppSettings.KEY_SHOW_MINIPLAYER_METADATA)
        val KEEP_SCREEN_ON_FULLSCREEN = booleanPreferencesKey(AppSettings.KEY_KEEP_SCREEN_ON_FULLSCREEN)
        val SHOW_QUICK_TOASTS = booleanPreferencesKey(AppSettings.KEY_SHOW_QUICK_TOASTS)
        val SHOW_INSECURE_STREAMS = booleanPreferencesKey(AppSettings.KEY_SHOW_INSECURE_STREAMS)
        val NOTIFICATION_SHOW_PLAY_PAUSE = booleanPreferencesKey(AppSettings.KEY_NOTIFICATION_SHOW_PLAY_PAUSE)
        val NOTIFICATION_SHOW_STOP = booleanPreferencesKey(AppSettings.KEY_NOTIFICATION_SHOW_STOP)
        val NOTIFICATION_SHOW_PREVIOUS = booleanPreferencesKey(AppSettings.KEY_NOTIFICATION_SHOW_PREVIOUS)
        val NOTIFICATION_SHOW_NEXT = booleanPreferencesKey(AppSettings.KEY_NOTIFICATION_SHOW_NEXT)
        val DEFAULT_AUDIO_QUALITY = stringPreferencesKey(AppSettings.KEY_DEFAULT_AUDIO_QUALITY)
        val ALLOW_MOBILE_DATA = booleanPreferencesKey(AppSettings.KEY_ALLOW_MOBILE_DATA)
        val BUFFER_PROFILE = stringPreferencesKey(AppSettings.KEY_BUFFER_PROFILE)
        val TIMESHIFT_GUARD = booleanPreferencesKey(AppSettings.KEY_TIMESHIFT_GUARD)
        val THERMAL_MODE = booleanPreferencesKey(AppSettings.KEY_THERMAL_MODE)
        val AUTO_PLAY_ON_ANDROID_AUTO_CONNECT = booleanPreferencesKey(AppSettings.KEY_AUTO_PLAY_ON_ANDROID_AUTO_CONNECT)
        val LIMIT_ANDROID_AUTO_QUALITY = booleanPreferencesKey(AppSettings.KEY_LIMIT_ANDROID_AUTO_QUALITY)
        val CONFIRM_REMOVE_FAVORITE = booleanPreferencesKey(AppSettings.KEY_CONFIRM_REMOVE_FAVORITE)
        val FIRST_RUN_ONBOARDING_DONE = booleanPreferencesKey(AppSettings.KEY_FIRST_RUN_ONBOARDING_DONE)
        val LAST_STATION_UUID = stringPreferencesKey(AppSettings.KEY_LAST_STATION_UUID)
        val LAST_STATION_NAME = stringPreferencesKey(AppSettings.KEY_LAST_STATION_NAME)
        val LAST_STATION_STREAM_URL = stringPreferencesKey(AppSettings.KEY_LAST_STATION_STREAM_URL)
        val LAST_STATION_FAVICON_URL = stringPreferencesKey(AppSettings.KEY_LAST_STATION_FAVICON_URL)
        val LAST_STATION_COUNTRY = stringPreferencesKey(AppSettings.KEY_LAST_STATION_COUNTRY)
    }

    /** Single source of truth — all values bundled in one Flow. */
    open val data: Flow<AppSettingsState> = context.settingsDataStore.data
        .map { p -> p.toState() }
        .distinctUntilChanged()

    // Granular Flow projections — these distinctUntilChanged so consumers only
    // react to the slice they care about.
    val themeMode: Flow<String> = data.map { it.themeMode }.distinctUntilChanged()
    val appLanguage: Flow<String> = data.map { it.appLanguage }.distinctUntilChanged()
    val dynamicColors: Flow<Boolean> = data.map { it.dynamicColors }.distinctUntilChanged()
    val showMiniplayerMetadata: Flow<Boolean> = data.map { it.showMiniplayerMetadata }.distinctUntilChanged()
    val keepScreenOnFullscreen: Flow<Boolean> = data.map { it.keepScreenOnFullscreen }.distinctUntilChanged()
    val showQuickToasts: Flow<Boolean> = data.map { it.showQuickToasts }.distinctUntilChanged()
    val showInsecureStreams: Flow<Boolean> = data.map { it.showInsecureStreams }.distinctUntilChanged()
    val confirmRemoveFavorite: Flow<Boolean> = data.map { it.confirmRemoveFavorite }.distinctUntilChanged()
    val firstRunOnboardingDone: Flow<Boolean> = data.map { it.firstRunOnboardingDone }.distinctUntilChanged()

    // Setters — all suspend; safe to call from viewModelScope or any coroutine.
    suspend fun setThemeMode(v: String) = edit { it[Keys.THEME_MODE] = v }
    suspend fun setAppLanguage(v: String) = edit { it[Keys.APP_LANGUAGE] = v }
    suspend fun setDynamicColors(v: Boolean) = edit { it[Keys.DYNAMIC_COLORS] = v }
    suspend fun setShowMiniplayerMetadata(v: Boolean) = edit { it[Keys.SHOW_MINIPLAYER_METADATA] = v }
    suspend fun setKeepScreenOnFullscreen(v: Boolean) = edit { it[Keys.KEEP_SCREEN_ON_FULLSCREEN] = v }
    suspend fun setShowQuickToasts(v: Boolean) = edit { it[Keys.SHOW_QUICK_TOASTS] = v }
    suspend fun setShowInsecureStreams(v: Boolean) = edit { it[Keys.SHOW_INSECURE_STREAMS] = v }
    suspend fun setNotificationShowPlayPause(v: Boolean) = edit { it[Keys.NOTIFICATION_SHOW_PLAY_PAUSE] = v }
    suspend fun setNotificationShowStop(v: Boolean) = edit { it[Keys.NOTIFICATION_SHOW_STOP] = v }
    suspend fun setNotificationShowPrevious(v: Boolean) = edit { it[Keys.NOTIFICATION_SHOW_PREVIOUS] = v }
    suspend fun setNotificationShowNext(v: Boolean) = edit { it[Keys.NOTIFICATION_SHOW_NEXT] = v }
    suspend fun setDefaultAudioQuality(v: String) = edit { it[Keys.DEFAULT_AUDIO_QUALITY] = v }
    suspend fun setAllowMobileData(v: Boolean) = edit { it[Keys.ALLOW_MOBILE_DATA] = v }
    suspend fun setBufferProfile(v: String) = edit { it[Keys.BUFFER_PROFILE] = v }
    suspend fun setTimeshiftGuard(v: Boolean) = edit { it[Keys.TIMESHIFT_GUARD] = v }
    suspend fun setThermalMode(v: Boolean) = edit { it[Keys.THERMAL_MODE] = v }
    suspend fun setAutoPlayOnAndroidAutoConnect(v: Boolean) = edit { it[Keys.AUTO_PLAY_ON_ANDROID_AUTO_CONNECT] = v }
    suspend fun setLimitAndroidAutoQuality(v: Boolean) = edit { it[Keys.LIMIT_ANDROID_AUTO_QUALITY] = v }
    suspend fun setConfirmRemoveFavorite(v: Boolean) = edit { it[Keys.CONFIRM_REMOVE_FAVORITE] = v }
    suspend fun setFirstRunOnboardingDone(v: Boolean) = edit { it[Keys.FIRST_RUN_ONBOARDING_DONE] = v }

    /** Atomically persist the last played station's identification fields. */
    open suspend fun setLastStation(
        uuid: String?,
        name: String?,
        streamUrl: String?,
        faviconUrl: String?,
        country: String?,
    ) = edit { p ->
        if (uuid != null) p[Keys.LAST_STATION_UUID] = uuid else p.remove(Keys.LAST_STATION_UUID)
        if (name != null) p[Keys.LAST_STATION_NAME] = name else p.remove(Keys.LAST_STATION_NAME)
        if (streamUrl != null) p[Keys.LAST_STATION_STREAM_URL] = streamUrl else p.remove(Keys.LAST_STATION_STREAM_URL)
        if (faviconUrl != null) p[Keys.LAST_STATION_FAVICON_URL] = faviconUrl else p.remove(Keys.LAST_STATION_FAVICON_URL)
        if (country != null) p[Keys.LAST_STATION_COUNTRY] = country else p.remove(Keys.LAST_STATION_COUNTRY)
    }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.settingsDataStore.edit(block)
    }

    private fun Preferences.toState(): AppSettingsState = AppSettingsState(
        themeMode = this[Keys.THEME_MODE] ?: AppSettings.THEME_SYSTEM,
        appLanguage = this[Keys.APP_LANGUAGE] ?: AppSettings.LANGUAGE_SYSTEM,
        dynamicColors = this[Keys.DYNAMIC_COLORS] ?: false,
        showMiniplayerMetadata = this[Keys.SHOW_MINIPLAYER_METADATA] ?: true,
        keepScreenOnFullscreen = this[Keys.KEEP_SCREEN_ON_FULLSCREEN] ?: false,
        showQuickToasts = this[Keys.SHOW_QUICK_TOASTS] ?: true,
        showInsecureStreams = this[Keys.SHOW_INSECURE_STREAMS] ?: true,
        notificationShowPlayPause = this[Keys.NOTIFICATION_SHOW_PLAY_PAUSE] ?: true,
        notificationShowStop = this[Keys.NOTIFICATION_SHOW_STOP] ?: true,
        notificationShowPrevious = this[Keys.NOTIFICATION_SHOW_PREVIOUS] ?: true,
        notificationShowNext = this[Keys.NOTIFICATION_SHOW_NEXT] ?: true,
        defaultAudioQuality = this[Keys.DEFAULT_AUDIO_QUALITY] ?: AppSettings.QUALITY_AUTO,
        allowMobileData = this[Keys.ALLOW_MOBILE_DATA] ?: true,
        bufferProfile = this[Keys.BUFFER_PROFILE] ?: AppSettings.BUFFER_MEDIUM,
        timeshiftGuard = this[Keys.TIMESHIFT_GUARD] ?: true,
        thermalMode = this[Keys.THERMAL_MODE] ?: false,
        autoPlayOnAndroidAutoConnect = this[Keys.AUTO_PLAY_ON_ANDROID_AUTO_CONNECT] ?: true,
        limitAndroidAutoQuality = this[Keys.LIMIT_ANDROID_AUTO_QUALITY] ?: true,
        confirmRemoveFavorite = this[Keys.CONFIRM_REMOVE_FAVORITE] ?: false,
        firstRunOnboardingDone = this[Keys.FIRST_RUN_ONBOARDING_DONE] ?: false,
        lastStationUuid = this[Keys.LAST_STATION_UUID],
        lastStationName = this[Keys.LAST_STATION_NAME],
        lastStationStreamUrl = this[Keys.LAST_STATION_STREAM_URL],
        lastStationFaviconUrl = this[Keys.LAST_STATION_FAVICON_URL],
        lastStationCountry = this[Keys.LAST_STATION_COUNTRY],
    )
}
