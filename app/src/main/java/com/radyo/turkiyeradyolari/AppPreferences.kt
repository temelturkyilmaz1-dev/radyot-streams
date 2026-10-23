package com.radyo.turkiyeradyolari

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

object AppPreferences {
    private const val PREFS = "radyot_settings"
    private const val KEY_THEME = "theme"
    private const val KEY_ACCENT = "accent"
    private const val KEY_RADIO_VIEW = "radio_view"
    private const val KEY_LAST_RADIO = "last_radio"
    private const val KEY_BLUR_LOGO_ENABLED = "blur_logo_enabled"
    private const val KEY_BLUR_LOGO_INTENSITY = "blur_logo_intensity"
    private const val KEY_SHOW_ALBUM = "show_album_art"
    private const val KEY_SHOW_STATION_LOGO = "show_station_logo"
    private const val KEY_SHOW_STATION_NAME = "show_station_name"
    private const val KEY_HISTORY_TRANSPARENCY = "history_panel_transparency"
    private const val KEY_EQ_PREFIX = "eq_band_"
    private const val KEY_CONTINUOUS_AUDIO = "continuous_audio"
    private const val KEY_EQ_ENABLED = "equalizer_enabled"
    private const val KEY_EQ_PRESET = "equalizer_preset"
    private const val KEY_RECENT_RADIOS = "recent_radios"
    private const val KEY_BASS_ENABLED = "bass_boost_enabled"
    private const val KEY_BASS_STRENGTH = "bass_boost_strength"
    private const val KEY_PLAYBACK_REQUESTED = "playback_requested"
    private const val KEY_WIFI_ONLY = "wifi_only"
    private const val KEY_BUFFER_PROFILE = "buffer_profile"
    private const val KEY_ALL_RADIOS_ANCHOR = "all_radios_anchor"
    private const val KEY_ALL_RADIOS_OFFSET = "all_radios_offset"
    private const val KEY_LOGO_COLORS = "logo_colors"
    private const val KEY_TV_AMBIENT = "tv_ambient"

    const val THEME_SYSTEM = "system"
    const val THEME_LIGHT = "light"
    const val THEME_DARK = "dark"
    const val THEME_METALLIC_GOLD = "metallic_gold"
    const val THEME_ROSE_GOLD = "rose_gold"
    const val THEME_TITANIUM = "titanium"
    const val VIEW_LIST = "list"
    const val VIEW_GRID_2 = "grid_2"
    const val VIEW_GRID_4 = "grid_4"
    const val BUFFER_FAST = "fast"
    const val BUFFER_BALANCED = "balanced"
    const val BUFFER_STABLE = "stable"
    @Deprecated("Use VIEW_GRID_2")
    const val VIEW_GRID = VIEW_GRID_2

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun theme(context: Context): String = prefs(context).getString(KEY_THEME, THEME_SYSTEM) ?: THEME_SYSTEM
    fun setTheme(context: Context, value: String) { prefs(context).edit().putString(KEY_THEME, value).apply(); applyTheme(value) }
    fun applySavedTheme(context: Context) {
        val savedTheme = theme(context)
        applyTheme(savedTheme)
        context.setTheme(
            when (savedTheme) {
                THEME_METALLIC_GOLD -> R.style.Theme_TurkiyeRadyolari_MetallicGold
                THEME_ROSE_GOLD -> R.style.Theme_TurkiyeRadyolari_RoseGold
                THEME_TITANIUM -> R.style.Theme_TurkiyeRadyolari_Titanium
                else -> R.style.Theme_TurkiyeRadyolari
            }
        )
    }
    private fun applyTheme(value: String) {
        AppCompatDelegate.setDefaultNightMode(
            when (value) {
                THEME_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                THEME_DARK -> AppCompatDelegate.MODE_NIGHT_YES
                THEME_METALLIC_GOLD, THEME_ROSE_GOLD, THEME_TITANIUM ->
                    AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
        )
    }

    fun accent(context: Context): Int {
        when (theme(context)) {
            THEME_METALLIC_GOLD -> return 0xFFD3AF37.toInt()
            THEME_ROSE_GOLD -> return 0xFFDEA193.toInt()
            THEME_TITANIUM -> return 0xFFC7CDD3.toInt()
        }
        val saved = prefs(context).getInt(KEY_ACCENT, 0xFF5F6368.toInt())
        // Migrate the former teal default to RadyoT 3.0's granite theme.
        return if (saved == 0xFF00796B.toInt()) 0xFF5F6368.toInt() else saved
    }
    fun setAccent(context: Context, color: Int) = prefs(context).edit().putInt(KEY_ACCENT, color).apply()
    fun radioView(context: Context): String = prefs(context).getString(KEY_RADIO_VIEW, VIEW_LIST)?.let { if (it == "grid") VIEW_GRID_2 else it } ?: VIEW_LIST
    fun setRadioView(context: Context, value: String) = prefs(context).edit().putString(KEY_RADIO_VIEW, value).apply()
    fun lastRadio(context: Context): String? = prefs(context).getString(KEY_LAST_RADIO, null)
    fun setLastRadio(context: Context, name: String) = prefs(context).edit().putString(KEY_LAST_RADIO, name).apply()
    fun playbackRequested(context: Context): Boolean = prefs(context).getBoolean(KEY_PLAYBACK_REQUESTED, false)
    fun setPlaybackRequested(context: Context, requested: Boolean) =
        prefs(context).edit().putBoolean(KEY_PLAYBACK_REQUESTED, requested).apply()

    fun wifiOnly(context: Context): Boolean = prefs(context).getBoolean(KEY_WIFI_ONLY, false)
    fun setWifiOnly(context: Context, enabled: Boolean) =
        prefs(context).edit().putBoolean(KEY_WIFI_ONLY, enabled).apply()

    fun bufferProfile(context: Context): String =
        prefs(context).getString(KEY_BUFFER_PROFILE, BUFFER_BALANCED) ?: BUFFER_BALANCED
    fun setBufferProfile(context: Context, profile: String) =
        prefs(context).edit().putString(KEY_BUFFER_PROFILE, profile).apply()

    fun allRadiosAnchor(context: Context): String? =
        prefs(context).getString(KEY_ALL_RADIOS_ANCHOR, null)
    fun allRadiosOffset(context: Context): Int =
        prefs(context).getInt(KEY_ALL_RADIOS_OFFSET, 0)
    fun setAllRadiosPosition(context: Context, radioName: String, offset: Int) =
        prefs(context).edit()
            .putString(KEY_ALL_RADIOS_ANCHOR, radioName)
            .putInt(KEY_ALL_RADIOS_OFFSET, offset)
            .apply()

    fun logoColors(context: Context): Boolean = prefs(context).getBoolean(KEY_LOGO_COLORS, true)
    fun setLogoColors(context: Context, enabled: Boolean) =
        prefs(context).edit().putBoolean(KEY_LOGO_COLORS, enabled).apply()
    fun tvAmbient(context: Context): Boolean = prefs(context).getBoolean(KEY_TV_AMBIENT, true)
    fun setTvAmbient(context: Context, enabled: Boolean) =
        prefs(context).edit().putBoolean(KEY_TV_AMBIENT, enabled).apply()

    fun recentRadios(context: Context): List<String> = prefs(context)
        .getString(KEY_RECENT_RADIOS, "").orEmpty()
        .split("|").filter(String::isNotBlank)

    fun addRecentRadio(context: Context, name: String) {
        val updated = listOf(name) + recentRadios(context).filterNot { it == name }
        prefs(context).edit().putString(KEY_RECENT_RADIOS, updated.take(20).joinToString("|")).apply()
    }

    fun blurLogoEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_BLUR_LOGO_ENABLED, true)
    fun setBlurLogoEnabled(context: Context, enabled: Boolean) = prefs(context).edit().putBoolean(KEY_BLUR_LOGO_ENABLED, enabled).apply()
    fun blurLogoIntensity(context: Context): Int = snapToTwenty(prefs(context).getInt(KEY_BLUR_LOGO_INTENSITY, 40))
    fun setBlurLogoIntensity(context: Context, intensity: Int) = prefs(context).edit().putInt(KEY_BLUR_LOGO_INTENSITY, snapToTwenty(intensity)).apply()

    fun showAlbumArt(context: Context): Boolean = prefs(context).getBoolean(KEY_SHOW_ALBUM, true)
    fun setShowAlbumArt(context: Context, value: Boolean) = prefs(context).edit().putBoolean(KEY_SHOW_ALBUM, value).apply()
    fun showStationLogo(context: Context): Boolean = prefs(context).getBoolean(KEY_SHOW_STATION_LOGO, true)
    fun setShowStationLogo(context: Context, value: Boolean) = prefs(context).edit().putBoolean(KEY_SHOW_STATION_LOGO, value).apply()
    fun showStationName(context: Context): Boolean = prefs(context).getBoolean(KEY_SHOW_STATION_NAME, true)
    fun setShowStationName(context: Context, value: Boolean) = prefs(context).edit().putBoolean(KEY_SHOW_STATION_NAME, value).apply()
    fun historyPanelTransparency(context: Context): Int = snapToTwenty(prefs(context).getInt(KEY_HISTORY_TRANSPARENCY, 20))
    fun setHistoryPanelTransparency(context: Context, value: Int) = prefs(context).edit().putInt(KEY_HISTORY_TRANSPARENCY, snapToTwenty(value)).apply()

    fun continuousAudio(context: Context): Boolean = prefs(context).getBoolean(KEY_CONTINUOUS_AUDIO, false)
    fun setContinuousAudio(context: Context, enabled: Boolean) = prefs(context).edit().putBoolean(KEY_CONTINUOUS_AUDIO, enabled).apply()

    fun equalizerEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_EQ_ENABLED, false)
    fun setEqualizerEnabled(context: Context, enabled: Boolean) = prefs(context).edit().putBoolean(KEY_EQ_ENABLED, enabled).apply()
    fun equalizerPreset(context: Context): String = prefs(context).getString(KEY_EQ_PRESET, "Düz") ?: "Düz"
    fun setEqualizerPreset(context: Context, value: String) = prefs(context).edit().putString(KEY_EQ_PRESET, value).apply()

    fun bassBoostEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_BASS_ENABLED, false)
    fun setBassBoostEnabled(context: Context, enabled: Boolean) = prefs(context).edit().putBoolean(KEY_BASS_ENABLED, enabled).apply()
    fun bassBoostStrength(context: Context): Short = prefs(context).getInt(KEY_BASS_STRENGTH, 500).coerceIn(0, 1000).toShort()
    fun setBassBoostStrength(context: Context, strength: Short) =
        prefs(context).edit().putInt(KEY_BASS_STRENGTH, strength.toInt().coerceIn(0, 1000)).apply()

    fun equalizerBand(context: Context, band: Int, defaultValue: Short): Short = prefs(context).getInt(KEY_EQ_PREFIX + band, defaultValue.toInt()).toShort()
    fun setEqualizerBand(context: Context, band: Int, value: Short) = prefs(context).edit().putInt(KEY_EQ_PREFIX + band, value.toInt()).apply()

    private fun snapToTwenty(value: Int): Int =
        ((value.coerceIn(0, 100) + 10) / 20 * 20).coerceIn(0, 100)
}
