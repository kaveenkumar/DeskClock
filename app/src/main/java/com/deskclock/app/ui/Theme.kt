package com.deskclock.app.ui

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import com.deskclock.app.settings.BackgroundMode
import com.deskclock.app.weather.WeatherKind

/**
 * Background gradients per weather kind and day/night. Night variants stay near-black on purpose:
 * the app runs 24/7 on an OLED panel, so dark pixels are both burn-in protection and the correct
 * bedside brightness. Day variants come in two flavours: [darkBackgrounds] keeps daytime as deep
 * weather-tinted shades (lit pixels age an OLED, blue subpixels fastest — a bright blue field all
 * day is the worst case), the bright palette is the classic colorful look for those who prefer it
 * over panel longevity, and BLACK emits nothing at all — weather then shows only through the
 * effect particles and the info row.
 */
fun backgroundBrush(kind: WeatherKind?, isNight: Boolean, background: BackgroundMode): Brush {
    if (background == BackgroundMode.BLACK) return SolidColor(Color.Black)
    val (top, bottom) = when {
        isNight -> when (kind) {
            WeatherKind.RAIN -> Color(0xFF04060C) to Color(0xFF0C1420)
            WeatherKind.SNOW -> Color(0xFF05070E) to Color(0xFF121826)
            WeatherKind.THUNDER -> Color(0xFF060409) to Color(0xFF160F24)
            WeatherKind.FOG -> Color(0xFF07090C) to Color(0xFF141A1E)
            WeatherKind.CLOUDY, WeatherKind.PARTLY -> Color(0xFF05070C) to Color(0xFF10141E)
            else -> Color(0xFF020308) to Color(0xFF0B1026) // clear night / no data yet
        }
        background == BackgroundMode.DARK -> when (kind) {
            WeatherKind.CLEAR -> Color(0xFF0A1B30) to Color(0xFF14395C)
            WeatherKind.PARTLY -> Color(0xFF0C1622) to Color(0xFF1D2F44)
            WeatherKind.CLOUDY -> Color(0xFF10141A) to Color(0xFF242C38)
            WeatherKind.FOG -> Color(0xFF12161A) to Color(0xFF272E34)
            WeatherKind.RAIN -> Color(0xFF0A1018) to Color(0xFF182430)
            WeatherKind.SNOW -> Color(0xFF10161E) to Color(0xFF2A3440)
            WeatherKind.THUNDER -> Color(0xFF0D0A18) to Color(0xFF201B36)
            else -> Color(0xFF0A121C) to Color(0xFF1A2A3C) // no data yet
        }
        else -> when (kind) {
            WeatherKind.CLEAR -> Color(0xFF1A5FA8) to Color(0xFF63A4D8)
            WeatherKind.PARTLY -> Color(0xFF3A6B9E) to Color(0xFF7FA8C9)
            WeatherKind.CLOUDY -> Color(0xFF4A5A6E) to Color(0xFF8595A8)
            WeatherKind.FOG -> Color(0xFF5A646E) to Color(0xFF98A2AB)
            WeatherKind.RAIN -> Color(0xFF2E4258) to Color(0xFF5C7690)
            WeatherKind.SNOW -> Color(0xFF5A7086) to Color(0xFF9FB4C8)
            WeatherKind.THUNDER -> Color(0xFF2A2E48) to Color(0xFF565C80)
            else -> Color(0xFF24415E) to Color(0xFF5E85A8) // no data yet
        }
    }
    return Brush.verticalGradient(listOf(top, bottom))
}

private val MOON_EMOJI =
    listOf("🌑", "🌒", "🌓", "🌔", "🌕", "🌖", "🌗", "🌘")

/**
 * Moon phase from the mean synodic month (29.5306 days) anchored at the 2000-01-06 18:14 UTC new
 * moon. Accurate to better than a day, which is all an emoji can express anyway.
 */
fun moonPhaseEmoji(date: java.time.LocalDate): String {
    val anchor = java.time.LocalDateTime.of(2000, 1, 6, 18, 14)
    val days = java.time.temporal.ChronoUnit.HOURS.between(anchor, date.atTime(12, 0)) / 24.0
    val synodic = 29.530588853
    val phase = (((days % synodic) + synodic) % synodic) / synodic
    return MOON_EMOJI[(phase * 8).toInt().coerceIn(0, 7)]
}

fun weatherEmoji(kind: WeatherKind, isNight: Boolean): String = when (kind) {
    WeatherKind.CLEAR -> if (isNight) "🌙" else "☀️"
    WeatherKind.PARTLY -> if (isNight) "☁️" else "⛅"
    WeatherKind.CLOUDY -> "☁️"
    WeatherKind.FOG -> "🌫️"
    WeatherKind.RAIN -> "🌧️"
    WeatherKind.SNOW -> "🌨️"
    WeatherKind.THUNDER -> "⛈️"
}
