package com.deskclock.app.settings

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalTime
import java.time.format.DateTimeFormatter

enum class BackgroundMode {
    /** Bright weather gradients by day (the classic look). */
    COLORFUL,

    /** Deep weather-tinted shades all day — kinder to the OLED. */
    DARK,

    /** Pure black at all times; only text and weather effects emit light. */
    BLACK,
}

enum class BlinkMode {
    /** Separator always on. */
    STEADY,

    /** Dips briefly within every second. */
    EVERY_SECOND,

    /** Bright on even seconds, dim on odd. */
    ALTERNATE,
}

enum class DimMode {
    /** Never dim. */
    OFF,

    /** Dim inside the user's fixed window. */
    SCHEDULE,

    /** Dim from sunset to sunrise. */
    SUN,
}

data class SettingsData(
    val latitude: Double?,
    val longitude: Double?,
    val placeName: String?,
    val use24h: Boolean,
    val dimMode: DimMode,
    val dimStart: LocalTime,
    val dimEnd: LocalTime,
    /** Screen brightness while dimmed: 1.0 = no dimming, lower = dimmer. */
    val dimBrightness: Float,
    val background: BackgroundMode,
    val blink: BlinkMode,
    /** Animated weather layer (rain, snow, stars, thunder flashes). */
    val weatherEffects: Boolean,
    /** Stored [com.deskclock.app.ui.ClockFont] name; resolved by the UI layer. */
    val clockFont: String,
)

/**
 * SharedPreferences-backed settings with a StateFlow mirror so the UI recomposes on change.
 * DataStore would be overkill for a handful of keys written from a single dialog.
 */
class Settings(context: Context) {

    private val prefs = context.getSharedPreferences("deskclock", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(read())
    val state: StateFlow<SettingsData> = _state

    private fun read() = SettingsData(
        latitude = prefs.getString(KEY_LAT, null)?.toDoubleOrNull(),
        longitude = prefs.getString(KEY_LON, null)?.toDoubleOrNull(),
        placeName = prefs.getString(KEY_PLACE, null),
        use24h = prefs.getBoolean(KEY_24H, true),
        dimMode = readDimMode(),
        dimStart = readTime(KEY_DIM_START, LocalTime.of(22, 0)),
        dimEnd = readTime(KEY_DIM_END, LocalTime.of(6, 0)),
        dimBrightness = prefs.getFloat(KEY_DIM_BRIGHTNESS, 0.15f),
        background = readBackground(),
        blink = readEnum(KEY_BLINK, BlinkMode.ALTERNATE),
        weatherEffects = prefs.getBoolean(KEY_WEATHER_EFFECTS, true),
        clockFont = prefs.getString(KEY_CLOCK_FONT, "") ?: "",
    )

    private fun readBackground(): BackgroundMode {
        prefs.getString(KEY_BACKGROUND, null)?.let { stored ->
            return runCatching { BackgroundMode.valueOf(stored) }.getOrDefault(BackgroundMode.DARK)
        }
        // Migration from the pre-0.7.0 "dark backgrounds" boolean.
        return if (prefs.getBoolean(KEY_DARK_BACKGROUNDS, true)) BackgroundMode.DARK
        else BackgroundMode.COLORFUL
    }

    private inline fun <reified T : Enum<T>> readEnum(key: String, fallback: T): T =
        prefs.getString(key, null)
            ?.let { stored -> runCatching { enumValueOf<T>(stored) }.getOrNull() }
            ?: fallback

    private fun readDimMode(): DimMode {
        prefs.getString(KEY_DIM_MODE, null)?.let { stored ->
            return runCatching { DimMode.valueOf(stored) }.getOrDefault(DimMode.SCHEDULE)
        }
        // Migration from the pre-0.4.0 boolean toggle.
        return if (prefs.getBoolean(KEY_DIM_ENABLED, true)) DimMode.SCHEDULE else DimMode.OFF
    }

    private fun readTime(key: String, fallback: LocalTime): LocalTime =
        prefs.getString(key, null)
            ?.let { runCatching { LocalTime.parse(it, TIME_FORMAT) }.getOrNull() }
            ?: fallback

    fun setLocation(latitude: Double, longitude: Double, placeName: String) {
        // Stored as strings: SharedPreferences has no putDouble and float precision truncates
        // coordinates enough to move the weather cell.
        prefs.edit {
            putString(KEY_LAT, latitude.toString())
            putString(KEY_LON, longitude.toString())
            putString(KEY_PLACE, placeName)
        }
        _state.value = read()
    }

    fun setUse24h(use24h: Boolean) {
        prefs.edit { putBoolean(KEY_24H, use24h) }
        _state.value = read()
    }

    fun setDimMode(mode: DimMode) {
        prefs.edit { putString(KEY_DIM_MODE, mode.name) }
        _state.value = read()
    }

    fun setDimWindow(start: LocalTime, end: LocalTime) {
        prefs.edit {
            putString(KEY_DIM_START, start.format(TIME_FORMAT))
            putString(KEY_DIM_END, end.format(TIME_FORMAT))
        }
        _state.value = read()
    }

    fun setClockFont(fontName: String) {
        prefs.edit { putString(KEY_CLOCK_FONT, fontName) }
        _state.value = read()
    }

    fun setBackground(mode: BackgroundMode) {
        prefs.edit { putString(KEY_BACKGROUND, mode.name) }
        _state.value = read()
    }

    fun setWeatherEffects(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_WEATHER_EFFECTS, enabled) }
        _state.value = read()
    }

    fun setBlink(mode: BlinkMode) {
        prefs.edit { putString(KEY_BLINK, mode.name) }
        _state.value = read()
    }

    fun setDimBrightness(brightness: Float) {
        prefs.edit { putFloat(KEY_DIM_BRIGHTNESS, brightness.coerceIn(0.05f, 1f)) }
        _state.value = read()
    }

    private companion object {
        val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        const val KEY_LAT = "latitude"
        const val KEY_LON = "longitude"
        const val KEY_PLACE = "place_name"
        const val KEY_24H = "use_24h"
        const val KEY_DIM_ENABLED = "dim_enabled" // legacy, read for migration only
        const val KEY_DIM_MODE = "dim_mode"
        const val KEY_DIM_START = "dim_start"
        const val KEY_DIM_END = "dim_end"
        const val KEY_DIM_BRIGHTNESS = "dim_brightness"
        const val KEY_DARK_BACKGROUNDS = "dark_backgrounds" // legacy, read for migration only
        const val KEY_BACKGROUND = "background_mode"
        const val KEY_BLINK = "blink_mode"
        const val KEY_WEATHER_EFFECTS = "weather_effects"
        const val KEY_CLOCK_FONT = "clock_font"
    }
}
