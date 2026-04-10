// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.core.model

/**
 * Shared app settings keys for lightweight local preferences.
 */
object AppSettings {
    const val PREFS_NAME = "radiowave_settings"

    const val KEY_THEME_MODE = "theme_mode"
    const val KEY_APP_LANGUAGE = "app_language"
    const val KEY_DYNAMIC_COLORS = "dynamic_colors"

    const val KEY_SHOW_MINIPLAYER_METADATA = "show_miniplayer_metadata"
    const val KEY_KEEP_SCREEN_ON_FULLSCREEN = "keep_screen_on_fullscreen"
    const val KEY_SHOW_QUICK_TOASTS = "show_quick_toasts"
    const val KEY_SHOW_INSECURE_STREAMS = "show_insecure_streams"
    const val KEY_NOTIFICATION_SHOW_PLAY_PAUSE = "notification_show_play_pause"
    const val KEY_NOTIFICATION_SHOW_STOP = "notification_show_stop"
    const val KEY_NOTIFICATION_SHOW_PREVIOUS = "notification_show_previous"
    const val KEY_NOTIFICATION_SHOW_NEXT = "notification_show_next"

    const val KEY_DEFAULT_AUDIO_QUALITY = "default_audio_quality"
    const val KEY_ALLOW_MOBILE_DATA = "allow_mobile_data"
    const val KEY_BUFFER_PROFILE = "buffer_profile"
    const val KEY_TIMESHIFT_GUARD = "timeshift_guard"
    const val KEY_THERMAL_MODE = "thermal_mode"
    const val KEY_AUTO_PLAY_ON_ANDROID_AUTO_CONNECT = "auto_play_on_android_auto_connect"
    const val KEY_LIMIT_ANDROID_AUTO_QUALITY = "limit_android_auto_quality"
    const val KEY_FIRST_RUN_ONBOARDING_DONE = "first_run_onboarding_done"
    const val KEY_LAST_SEEN_WHATS_NEW_VERSION = "last_seen_whats_new_version"
    const val KEY_LAST_STATION_UUID = "last_station_uuid"
    const val KEY_LAST_STATION_NAME = "last_station_name"
    const val KEY_LAST_STATION_STREAM_URL = "last_station_stream_url"
    const val KEY_LAST_STATION_FAVICON_URL = "last_station_favicon_url"
    const val KEY_LAST_STATION_COUNTRY = "last_station_country"

    const val THEME_SYSTEM = "system"
    const val THEME_DARK = "dark"
    const val THEME_LIGHT = "light"
    const val LANGUAGE_SYSTEM = "system"
    const val LANGUAGE_DE = "de"
    const val LANGUAGE_EN = "en"

    const val QUALITY_AUTO = "auto"
    const val QUALITY_LOW = "low"
    const val QUALITY_MEDIUM = "medium"
    const val QUALITY_HIGH = "high"

    const val BUFFER_SMALL = "small"
    const val BUFFER_MEDIUM = "medium"
    const val BUFFER_LARGE = "large"
}

