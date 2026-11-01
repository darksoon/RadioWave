// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.core.data.repository

import android.content.Context
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import de.darksoon.radiowave.core.model.AppSettings
import de.darksoon.radiowave.core.model.HomeGenres
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Snapshot of all RadioWave settings — exposed as a single Flow for consumers
 * who need multiple values at once (e.g. PlayerControllerImpl).
 *
 * Individual Flow<T> properties on [SettingsRepository] derive from this state
 * with [distinctUntilChanged] so a UI listening only to `themeMode` doesn't
 * recompose when an unrelated setting changes.
 *
 * REA 95fcb33: Feldreihenfolge und Defaults 1:1 aus dem Handy-Build (C0646ql: toString()
 * + No-Arg-Konstruktor). Die sechs neuen Felder stehen dort ganz vorne, wifi/mobile-
 * AudioQuality direkt hinter defaultAudioQuality.
 */
data class AppSettingsState(
    // Live audio spectrum in the full-screen player (local analysis, no microphone).
    val visualizerEnabled: Boolean = false,
    // AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK (e.g. navigation prompts): mute instead of ducking
    // to 25 %. Key name kept for existing installs; playback never pauses (FIX 2026-11-01).
    val pauseForNavigation: Boolean = false,
    // Customizable home screen. Stored as comma-separated string, see [HomeGenres.sanitize].
    val homeGenres: List<String> = HomeGenres.ALL,
    val homeStartGenre: String = HomeGenres.DEFAULT_START_GENRE,
    val homeShowRecent: Boolean = true,
    val homeShowDiscover: Boolean = true,
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
    // Only used when defaultAudioQuality == auto: chosen at station start by network type
    // (unmetered Wi-Fi/Ethernet vs. mobile or metered Wi-Fi). Values: low/medium/high.
    val wifiAudioQuality: String = AppSettings.QUALITY_HIGH,
    val mobileAudioQuality: String = AppSettings.QUALITY_LOW,
    val eqPreset: String = AppSettings.EQ_PRESET_FLAT,
    val allowMobileData: Boolean = true,
    val bufferProfile: String = AppSettings.BUFFER_MEDIUM,
    val timeshiftGuard: Boolean = true,
    val thermalMode: Boolean = false,
    val autoPlayOnAndroidAutoConnect: Boolean = true,
    val autoPlayOnAppStart: Boolean = false,
    val restoreLastView: Boolean = false,
    val lastViewRoute: String? = null,
    val lastViewPlayerOpen: Boolean = false,
    val limitAndroidAutoQuality: Boolean = true,
    val confirmRemoveFavorite: Boolean = false,
    // Outbound requests that aren't needed for playback itself — both on by default so
    // behaviour doesn't change silently for existing users; see README "Privacy".
    val reportStationClicks: Boolean = true,
    val onlineCoverLookup: Boolean = true,
    val firstRunOnboardingDone: Boolean = false,
    // Local station-directory mirror (full Radio Browser dump in Room).
    val stationMirrorEnabled: Boolean = false,
    val stationMirrorIntervalHours: Int = 168, // 24 = daily, 168 = weekly, 0 = manual only
    val stationMirrorWifiOnly: Boolean = true,
    val stationMirrorLastSyncAtMs: Long = 0L,
    val stationMirrorStationCount: Int = 0,
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
        val VISUALIZER_ENABLED = booleanPreferencesKey(AppSettings.KEY_VISUALIZER_ENABLED)
        val PAUSE_FOR_NAVIGATION = booleanPreferencesKey(AppSettings.KEY_PAUSE_FOR_NAVIGATION)
        // Comma-separated (no stringSet: order matters) — REA: Handy-Build liest mit split(",").
        val HOME_GENRES = stringPreferencesKey(AppSettings.KEY_HOME_GENRES)
        val HOME_START_GENRE = stringPreferencesKey(AppSettings.KEY_HOME_START_GENRE)
        val HOME_SHOW_RECENT = booleanPreferencesKey(AppSettings.KEY_HOME_SHOW_RECENT)
        val HOME_SHOW_DISCOVER = booleanPreferencesKey(AppSettings.KEY_HOME_SHOW_DISCOVER)
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
        val WIFI_AUDIO_QUALITY = stringPreferencesKey(AppSettings.KEY_WIFI_AUDIO_QUALITY)
        val MOBILE_AUDIO_QUALITY = stringPreferencesKey(AppSettings.KEY_MOBILE_AUDIO_QUALITY)
        val EQ_PRESET = stringPreferencesKey(AppSettings.KEY_EQ_PRESET)
        val ALLOW_MOBILE_DATA = booleanPreferencesKey(AppSettings.KEY_ALLOW_MOBILE_DATA)
        val BUFFER_PROFILE = stringPreferencesKey(AppSettings.KEY_BUFFER_PROFILE)
        val TIMESHIFT_GUARD = booleanPreferencesKey(AppSettings.KEY_TIMESHIFT_GUARD)
        val THERMAL_MODE = booleanPreferencesKey(AppSettings.KEY_THERMAL_MODE)
        val AUTO_PLAY_ON_ANDROID_AUTO_CONNECT = booleanPreferencesKey(AppSettings.KEY_AUTO_PLAY_ON_ANDROID_AUTO_CONNECT)
        val AUTO_PLAY_ON_APP_START = booleanPreferencesKey(AppSettings.KEY_AUTO_PLAY_ON_APP_START)
        val RESTORE_LAST_VIEW = booleanPreferencesKey(AppSettings.KEY_RESTORE_LAST_VIEW)
        val LAST_VIEW_ROUTE = stringPreferencesKey(AppSettings.KEY_LAST_VIEW_ROUTE)
        val LAST_VIEW_PLAYER_OPEN = booleanPreferencesKey(AppSettings.KEY_LAST_VIEW_PLAYER_OPEN)
        val LIMIT_ANDROID_AUTO_QUALITY = booleanPreferencesKey(AppSettings.KEY_LIMIT_ANDROID_AUTO_QUALITY)
        val CONFIRM_REMOVE_FAVORITE = booleanPreferencesKey(AppSettings.KEY_CONFIRM_REMOVE_FAVORITE)
        val REPORT_STATION_CLICKS = booleanPreferencesKey(AppSettings.KEY_REPORT_STATION_CLICKS)
        val ONLINE_COVER_LOOKUP = booleanPreferencesKey(AppSettings.KEY_ONLINE_COVER_LOOKUP)
        val FIRST_RUN_ONBOARDING_DONE = booleanPreferencesKey(AppSettings.KEY_FIRST_RUN_ONBOARDING_DONE)
        val LAST_STATION_UUID = stringPreferencesKey(AppSettings.KEY_LAST_STATION_UUID)
        val LAST_STATION_NAME = stringPreferencesKey(AppSettings.KEY_LAST_STATION_NAME)
        val LAST_STATION_STREAM_URL = stringPreferencesKey(AppSettings.KEY_LAST_STATION_STREAM_URL)
        val LAST_STATION_FAVICON_URL = stringPreferencesKey(AppSettings.KEY_LAST_STATION_FAVICON_URL)
        val LAST_STATION_COUNTRY = stringPreferencesKey(AppSettings.KEY_LAST_STATION_COUNTRY)
        val STATION_MIRROR_ENABLED = booleanPreferencesKey(AppSettings.KEY_STATION_MIRROR_ENABLED)
        val STATION_MIRROR_INTERVAL_HOURS = intPreferencesKey(AppSettings.KEY_STATION_MIRROR_INTERVAL_HOURS)
        val STATION_MIRROR_WIFI_ONLY = booleanPreferencesKey(AppSettings.KEY_STATION_MIRROR_WIFI_ONLY)
        val STATION_MIRROR_LAST_SYNC_AT = longPreferencesKey(AppSettings.KEY_STATION_MIRROR_LAST_SYNC_AT)
        val STATION_MIRROR_STATION_COUNT = intPreferencesKey(AppSettings.KEY_STATION_MIRROR_STATION_COUNT)
    }

    /** Single source of truth — all values bundled in one Flow. */
    open val data: Flow<AppSettingsState> = context.settingsDataStore.data
        .map { p -> p.toState() }
        .distinctUntilChanged()

    // Process-wide cache for [initialSnapshotBlocking] — multiple ViewModels and
    // services are constructed simultaneously at app start and all need an initial
    // value; without this they'd each runBlocking-read DataStore independently and
    // serialize on DataStore's internal read lock.
    @Volatile private var cachedInitialSnapshot: AppSettingsState? = null

    /**
     * Synchronous snapshot of the current settings — used as initial value for
     * `StateFlow`s so the UI doesn't flash with [AppSettingsState.DEFAULTS]
     * before DataStore has emitted the real value. Blocks the calling thread
     * on the first DataStore read only; subsequent calls return a cached value.
     *
     * DataStore's first read can take tens of milliseconds (longer if a
     * SharedPreferences migration is in flight); call this from `init`/`onCreate`
     * paths where a brief block is acceptable rather than mid-frame.
     */
    open fun initialSnapshotBlocking(): AppSettingsState {
        cachedInitialSnapshot?.let { return it }
        // Double-checked under synchronized so concurrent constructors don't all
        // hit DataStore.
        return synchronized(this) {
            cachedInitialSnapshot ?: runBlocking { data.first() }
                .also { cachedInitialSnapshot = it }
        }
    }

    /**
     * Fills the [initialSnapshotBlocking] cache from a background coroutine. Called
     * from `Application.onCreate` so the first ViewModel/service on the main thread
     * normally finds the snapshot ready instead of waiting on DataStore's first read
     * (and a possible SharedPreferences migration). The blocking path stays as the
     * fallback when a consumer is faster than this warm-up.
     */
    suspend fun warmUpInitialSnapshot() {
        if (cachedInitialSnapshot != null) return
        val snapshot = data.first()
        synchronized(this) {
            if (cachedInitialSnapshot == null) cachedInitialSnapshot = snapshot
        }
    }

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
    val eqPreset: Flow<String> = data.map { it.eqPreset }.distinctUntilChanged()

    // Setters — all suspend; safe to call from viewModelScope or any coroutine.
    suspend fun setVisualizerEnabled(v: Boolean) = edit { it[Keys.VISUALIZER_ENABLED] = v }
    suspend fun setPauseForNavigation(v: Boolean) = edit { it[Keys.PAUSE_FOR_NAVIGATION] = v }

    /**
     * Saves the home-screen layout in one transaction ("Startseite anpassen" → Speichern).
     * If the remembered start genre is no longer selected it falls back to the first one.
     *
     * REA 95fcb33: Logik aus jadx handy p000/C0749td.java (Fall 1), Aufruf aus dem
     * HomeViewModel (p000/fn0.java → qs1). REA-UNSICHER: Methoden-/Parametername geraten,
     * Parameterreihenfolge (genres, showRecent, showDiscover) gesichert.
     */
    suspend fun setHomeLayout(genres: List<String>, showRecent: Boolean, showDiscover: Boolean) = edit { p ->
        val sanitized = HomeGenres.sanitize(genres)
        p[Keys.HOME_GENRES] = sanitized.joinToString(",")
        val startGenre = p[Keys.HOME_START_GENRE] ?: HomeGenres.DEFAULT_START_GENRE
        if (startGenre !in sanitized) p[Keys.HOME_START_GENRE] = sanitized.first()
        p[Keys.HOME_SHOW_RECENT] = showRecent
        p[Keys.HOME_SHOW_DISCOVER] = showDiscover
    }

    /**
     * Remembers the last selected genre tab on the home screen; ignored if the genre is not
     * part of the current selection. REA 95fcb33: p000/tm0.java Fall 14. REA-UNSICHER: Name.
     */
    suspend fun setHomeStartGenre(genre: String) = edit { p ->
        val genres = HomeGenres.sanitize(p[Keys.HOME_GENRES]?.split(",").orEmpty())
        if (genre in genres) p[Keys.HOME_START_GENRE] = genre
    }

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

    // REA 95fcb33: beide Setter prüfen per require() auf low/medium/high ("Failed requirement."
    // in p000/ni4.java → Lambda hh4); "auto" ist hier nicht erlaubt.
    suspend fun setWifiAudioQuality(v: String) = edit {
        require(v in FIXED_QUALITIES)
        it[Keys.WIFI_AUDIO_QUALITY] = v
    }
    suspend fun setMobileAudioQuality(v: String) = edit {
        require(v in FIXED_QUALITIES)
        it[Keys.MOBILE_AUDIO_QUALITY] = v
    }
    suspend fun setEqPreset(v: String) = edit { it[Keys.EQ_PRESET] = v }
    suspend fun setAllowMobileData(v: Boolean) = edit { it[Keys.ALLOW_MOBILE_DATA] = v }
    suspend fun setBufferProfile(v: String) = edit { it[Keys.BUFFER_PROFILE] = v }
    suspend fun setTimeshiftGuard(v: Boolean) = edit { it[Keys.TIMESHIFT_GUARD] = v }
    suspend fun setThermalMode(v: Boolean) = edit { it[Keys.THERMAL_MODE] = v }
    suspend fun setAutoPlayOnAndroidAutoConnect(v: Boolean) = edit { it[Keys.AUTO_PLAY_ON_ANDROID_AUTO_CONNECT] = v }
    suspend fun setAutoPlayOnAppStart(v: Boolean) = edit { it[Keys.AUTO_PLAY_ON_APP_START] = v }
    suspend fun setRestoreLastView(v: Boolean) = edit { it[Keys.RESTORE_LAST_VIEW] = v }
    suspend fun setLastView(route: String, playerOpen: Boolean) = edit {
        it[Keys.LAST_VIEW_ROUTE] = route
        it[Keys.LAST_VIEW_PLAYER_OPEN] = playerOpen
    }
    suspend fun setLimitAndroidAutoQuality(v: Boolean) = edit { it[Keys.LIMIT_ANDROID_AUTO_QUALITY] = v }
    suspend fun setConfirmRemoveFavorite(v: Boolean) = edit { it[Keys.CONFIRM_REMOVE_FAVORITE] = v }
    suspend fun setReportStationClicks(v: Boolean) = edit { it[Keys.REPORT_STATION_CLICKS] = v }
    suspend fun setOnlineCoverLookup(v: Boolean) = edit { it[Keys.ONLINE_COVER_LOOKUP] = v }
    suspend fun setFirstRunOnboardingDone(v: Boolean) = edit { it[Keys.FIRST_RUN_ONBOARDING_DONE] = v }
    suspend fun setStationMirrorEnabled(v: Boolean) = edit { it[Keys.STATION_MIRROR_ENABLED] = v }
    suspend fun setStationMirrorIntervalHours(v: Int) = edit { it[Keys.STATION_MIRROR_INTERVAL_HOURS] = v }
    suspend fun setStationMirrorWifiOnly(v: Boolean) = edit { it[Keys.STATION_MIRROR_WIFI_ONLY] = v }
    open suspend fun setStationMirrorSyncResult(syncedAtMs: Long, stationCount: Int) = edit {
        it[Keys.STATION_MIRROR_LAST_SYNC_AT] = syncedAtMs
        it[Keys.STATION_MIRROR_STATION_COUNT] = stationCount
    }

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

    private fun Preferences.toState(): AppSettingsState {
        // REA 95fcb33: p000/C0648qn.java Fall 12 — Genres werden beim Lesen normalisiert, das
        // Start-Genre fällt auf das erste gewählte Genre zurück, wenn es nicht (mehr) enthalten ist.
        val homeGenres = HomeGenres.sanitize(this[Keys.HOME_GENRES]?.split(",").orEmpty())
        val storedStartGenre = this[Keys.HOME_START_GENRE] ?: HomeGenres.DEFAULT_START_GENRE
        return AppSettingsState(
            visualizerEnabled = this[Keys.VISUALIZER_ENABLED] ?: false,
            pauseForNavigation = this[Keys.PAUSE_FOR_NAVIGATION] ?: false,
            homeGenres = homeGenres,
            homeStartGenre = storedStartGenre.takeIf { it in homeGenres } ?: homeGenres.first(),
            homeShowRecent = this[Keys.HOME_SHOW_RECENT] ?: true,
            homeShowDiscover = this[Keys.HOME_SHOW_DISCOVER] ?: true,
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
            wifiAudioQuality = this[Keys.WIFI_AUDIO_QUALITY] ?: AppSettings.QUALITY_HIGH,
            mobileAudioQuality = this[Keys.MOBILE_AUDIO_QUALITY] ?: AppSettings.QUALITY_LOW,
            eqPreset = this[Keys.EQ_PRESET] ?: AppSettings.EQ_PRESET_FLAT,
            allowMobileData = this[Keys.ALLOW_MOBILE_DATA] ?: true,
            bufferProfile = this[Keys.BUFFER_PROFILE] ?: AppSettings.BUFFER_MEDIUM,
            timeshiftGuard = this[Keys.TIMESHIFT_GUARD] ?: true,
            thermalMode = this[Keys.THERMAL_MODE] ?: false,
            autoPlayOnAndroidAutoConnect = this[Keys.AUTO_PLAY_ON_ANDROID_AUTO_CONNECT] ?: true,
            autoPlayOnAppStart = this[Keys.AUTO_PLAY_ON_APP_START] ?: false,
            restoreLastView = this[Keys.RESTORE_LAST_VIEW] ?: false,
            lastViewRoute = this[Keys.LAST_VIEW_ROUTE],
            lastViewPlayerOpen = this[Keys.LAST_VIEW_PLAYER_OPEN] ?: false,
            limitAndroidAutoQuality = this[Keys.LIMIT_ANDROID_AUTO_QUALITY] ?: true,
            confirmRemoveFavorite = this[Keys.CONFIRM_REMOVE_FAVORITE] ?: false,
            reportStationClicks = this[Keys.REPORT_STATION_CLICKS] ?: true,
            onlineCoverLookup = this[Keys.ONLINE_COVER_LOOKUP] ?: true,
            firstRunOnboardingDone = this[Keys.FIRST_RUN_ONBOARDING_DONE] ?: false,
            stationMirrorEnabled = this[Keys.STATION_MIRROR_ENABLED] ?: false,
            stationMirrorIntervalHours = this[Keys.STATION_MIRROR_INTERVAL_HOURS] ?: 168,
            stationMirrorWifiOnly = this[Keys.STATION_MIRROR_WIFI_ONLY] ?: true,
            stationMirrorLastSyncAtMs = this[Keys.STATION_MIRROR_LAST_SYNC_AT] ?: 0L,
            stationMirrorStationCount = this[Keys.STATION_MIRROR_STATION_COUNT] ?: 0,
            lastStationUuid = this[Keys.LAST_STATION_UUID],
            lastStationName = this[Keys.LAST_STATION_NAME],
            lastStationStreamUrl = this[Keys.LAST_STATION_STREAM_URL],
            lastStationFaviconUrl = this[Keys.LAST_STATION_FAVICON_URL],
            lastStationCountry = this[Keys.LAST_STATION_COUNTRY],
        )
    }

    private companion object {
        val FIXED_QUALITIES = setOf(AppSettings.QUALITY_LOW, AppSettings.QUALITY_MEDIUM, AppSettings.QUALITY_HIGH)
    }
}
