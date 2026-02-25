package de.radiowave.core.model

/**
 * Shared app settings keys for lightweight local preferences.
 */
object AppSettings {
    const val PREFS_NAME = "radiowave_settings"

    const val KEY_THEME_MODE = "theme_mode"
    const val KEY_DYNAMIC_COLORS = "dynamic_colors"

    const val KEY_SHOW_MINIPLAYER_METADATA = "show_miniplayer_metadata"
    const val KEY_KEEP_SCREEN_ON_FULLSCREEN = "keep_screen_on_fullscreen"
    const val KEY_SHOW_QUICK_TOASTS = "show_quick_toasts"
    const val KEY_SHOW_INSECURE_STREAMS = "show_insecure_streams"

    const val KEY_DEFAULT_AUDIO_QUALITY = "default_audio_quality"
    const val KEY_ALLOW_MOBILE_DATA = "allow_mobile_data"
    const val KEY_BUFFER_PROFILE = "buffer_profile"

    const val THEME_SYSTEM = "system"
    const val THEME_DARK = "dark"
    const val THEME_LIGHT = "light"

    const val QUALITY_AUTO = "auto"
    const val QUALITY_LOW = "low"
    const val QUALITY_MEDIUM = "medium"
    const val QUALITY_HIGH = "high"

    const val BUFFER_SMALL = "small"
    const val BUFFER_MEDIUM = "medium"
    const val BUFFER_LARGE = "large"
}
