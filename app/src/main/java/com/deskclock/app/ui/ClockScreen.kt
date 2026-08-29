package com.deskclock.app.ui

import android.app.Activity
import android.view.WindowManager
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.deskclock.app.DeskClockViewModel
import com.deskclock.app.settings.BlinkMode
import com.deskclock.app.settings.DimMode
import com.deskclock.app.weather.WeatherState
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlinx.coroutines.delay

@Composable
fun ClockScreen(viewModel: DeskClockViewModel = viewModel()) {
    val time by viewModel.time.collectAsStateWithLifecycle()
    val battery by viewModel.battery.collectAsStateWithLifecycle()
    val weather by viewModel.weather.collectAsStateWithLifecycle()
    val settings by viewModel.settingsState.collectAsStateWithLifecycle()

    // Colors always follow the sun; when the screen dims is the user's choice of mode. At 100% dim
    // brightness no mode changes anything.
    val isNight = isNight(time, weather)
    val dimActive = settings.dimBrightness < 0.99f && when (settings.dimMode) {
        DimMode.OFF -> false
        DimMode.SCHEDULE -> inWindow(time.toLocalTime(), settings.dimStart, settings.dimEnd)
        DimMode.SUN -> isNight
    }

    // Dimming drops the hardware backlight to the chosen brightness instead of only painting darker
    // pixels: less light in the room and less OLED wear. Restored to the system value otherwise.
    val view = LocalView.current
    LaunchedEffect(dimActive, settings.dimBrightness) {
        (view.context as? Activity)?.window?.let { window ->
            window.attributes = window.attributes.apply {
                screenBrightness =
                    if (dimActive) settings.dimBrightness.coerceIn(0.01f, 1f)
                    else WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            }
        }
    }

    // Burn-in protection: the whole layout drifts to a new position (±12dp) once a minute, slowly
    // enough to be invisible. No pixel holds the same content for more than a minute.
    var driftTarget by remember { mutableStateOf(Pair(0f, 0f)) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            driftTarget = Pair(Random.nextFloat() * 24f - 12f, Random.nextFloat() * 24f - 12f)
        }
    }
    val driftX by animateDpAsState(driftTarget.first.dp, tween(8_000), label = "driftX")
    val driftY by animateDpAsState(driftTarget.second.dp, tween(8_000), label = "driftY")

    // Gentle overlay on top of the backlight drop, scaled with how low the brightness is set, so
    // very low settings get truly dark even at the panel's minimum backlight.
    val dimAlpha by animateFloatAsState(
        if (dimActive) (1f - settings.dimBrightness) * 0.35f else 0f,
        tween(2_000),
        label = "dim",
    )

    var showSettings by remember { mutableStateOf(false) }
    // First run: no location yet, so open settings instead of showing an empty weather slot.
    LaunchedEffect(settings.latitude) {
        if (settings.latitude == null) showSettings = true
    }

    MaterialTheme(colorScheme = darkColorScheme()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(backgroundBrush(weather?.kind, isNight, settings.background))
                .pointerInput(Unit) {
                    detectTapGestures(onLongPress = { showSettings = true })
                },
        ) {
            if (settings.weatherEffects) {
                WeatherEffects(
                    kind = weather?.kind,
                    isNight = isNight,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            ClockContent(
                time = time,
                use24h = settings.use24h,
                font = runCatching { ClockFont.valueOf(settings.clockFont) }
                    .getOrDefault(ClockFont.ROBOTO),
                blink = settings.blink,
                weather = weather,
                hasLocation = settings.latitude != null,
                batteryPercent = battery.percent,
                batteryCharging = battery.charging,
                batteryPlugged = battery.plugged,
                isNight = isNight,
                modifier = Modifier
                    .fillMaxSize()
                    .offset(x = driftX, y = driftY),
            )

            // Extra darkening on top of the backlight drop, so the scheduled window stays gentle
            // even at the panel's minimum brightness.
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = dimAlpha)),
            )

            if (showSettings) {
                SettingsDialog(
                    settings = settings,
                    onUse24hChange = viewModel::setUse24h,
                    onSelectPlace = viewModel::setLocation,
                    search = viewModel::searchCity,
                    onDimModeChange = viewModel::setDimMode,
                    onBackgroundChange = viewModel::setBackground,
                    onBlinkChange = viewModel::setBlink,
                    onWeatherEffectsChange = viewModel::setWeatherEffects,
                    onFontChange = { viewModel.setClockFont(it.name) },
                    onDimWindowChange = viewModel::setDimWindow,
                    onDimBrightnessChange = viewModel::setDimBrightness,
                    onDismiss = { showSettings = false },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ClockContent(
    time: LocalDateTime,
    use24h: Boolean,
    font: ClockFont,
    blink: BlinkMode,
    weather: WeatherState?,
    hasLocation: Boolean,
    batteryPercent: Int,
    batteryCharging: Boolean,
    batteryPlugged: Boolean,
    isNight: Boolean,
    modifier: Modifier = Modifier,
) {
    val textColor = if (isNight) Color(0xFFDDE3F0) else Color.White

    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val timeText = time.format(DateTimeFormatter.ofPattern(if (use24h) "HH:mm" else "h:mm"))
        // Tabular figures keep every digit the same width, so neither the big time nor the ticking
        // seconds counter shifts the centred layout as values change. The line-height trim matters
        // even more: Roboto reserves ~17% of the line above/below digits for ascenders/descenders
        // that digits never use; trimming it lets the glyphs themselves fill the height budget.
        val timeStyle = TextStyle(
            fontFamily = font.family(),
            fontWeight = font.weight,
            letterSpacing = if (font == ClockFont.ROBOTO) (-1).sp else 0.sp,
            fontFeatureSettings = "tnum",
            platformStyle = PlatformTextStyle(includeFontPadding = false),
            lineHeight = font.lineTrim.em,
            lineHeightStyle = LineHeightStyle(
                alignment = LineHeightStyle.Alignment.Center,
                trim = LineHeightStyle.Trim.Both,
            ),
        )

        // Maximise the font: measure the ACTUAL time string at a reference size, then scale so it
        // fills the width (or the height budget that leaves room for the info row and the burn-in
        // drift). Proportional fonts render narrow digits ('1', '7') much slimmer than a '0'
        // template would suggest, so sizing from the real string is what lets them fill the
        // screen; the size change when digits roll over is animated below. Width is the sum of
        // the hour/colon/minute parts (they are separate texts), plus the seconds/AM-PM column.
        val textMeasurer = rememberTextMeasurer()
        val density = LocalDensity.current
        val hourText = timeText.substringBefore(':')
        val minuteText = timeText.substringAfter(':')
        val timeSize = remember(timeText, maxWidth, maxHeight, use24h, font) {
            val base = timeStyle.copy(fontSize = 100.sp)
            val timeWidth = listOf(hourText, ":", minuteText)
                .sumOf { textMeasurer.measure(it, base).size.width }
                .toFloat()
            val timeHeight = textMeasurer.measure(timeText, base).size.height.toFloat()
            val secondsWidth = textMeasurer.measure("00", base).size.width * 0.17f
            val ampmWidth =
                if (use24h) 0f else textMeasurer.measure("AM", base).size.width * 0.15f
            val sideWidth = maxOf(secondsWidth, ampmWidth)
            val paddingPx = with(density) { 10.dp.toPx() }
            val widthScale =
                (with(density) { maxWidth.toPx() } * 0.94f - paddingPx) / (timeWidth + sideWidth)
            val heightScale = with(density) { maxHeight.toPx() } * 0.74f / timeHeight
            (100f * min(widthScale, heightScale))
        }
        val animatedTimeSize by animateFloatAsState(timeSize, tween(500), label = "timeSize")
        val infoSize = (animatedTimeSize * 0.135f).coerceIn(16f, 38f).sp
        val secondsSize = (animatedTimeSize * 0.17f).sp
        val iconHeight: Dp = with(density) { (infoSize.value * 0.85f).dp }

        // The colon dims rather than disappearing, so the rhythm reads without strobing.
        // EVERY_SECOND rides the per-second recomposition: each new `time` restarts the effect,
        // which dips the colon mid-second.
        var colonDim by remember { mutableStateOf(false) }
        LaunchedEffect(time, blink) {
            when (blink) {
                BlinkMode.STEADY -> colonDim = false
                BlinkMode.ALTERNATE -> colonDim = time.second % 2 == 1
                BlinkMode.EVERY_SECOND -> {
                    colonDim = false
                    delay(500)
                    colonDim = true
                }
            }
        }
        // Separator stays as subtle as the seconds counter (0.7), dipping to 0.3 — a bright
        // full-alpha blink right at the centre of vision was distracting.
        val colonAlpha = if (colonDim) 0.3f else 0.7f

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.Bottom) {
                // Hour, colon, and minutes are separate texts so the blink can be a pure
                // graphics-layer alpha. Blinking via a color span inside one string splits the
                // shaping runs only in the dimmed state, which drops digit:colon kerning and made
                // the colon jump sideways in kerned fonts (Poiret One after a 7).
                val bigStyle = timeStyle.copy(fontSize = animatedTimeSize.sp)
                Text(text = hourText, color = textColor, style = bigStyle)
                Text(
                    text = ":",
                    color = textColor,
                    style = bigStyle,
                    modifier = Modifier.alpha(colonAlpha),
                )
                Text(text = minuteText, color = textColor, style = bigStyle)
                Column(
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier.padding(
                        start = 10.dp,
                        bottom = with(density) { (animatedTimeSize * 0.05f).dp },
                    ),
                ) {
                    if (!use24h) {
                        Text(
                            text = time.format(DateTimeFormatter.ofPattern("a")),
                            color = textColor.copy(alpha = 0.7f),
                            fontSize = infoSize,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                    Text(
                        text = time.format(DateTimeFormatter.ofPattern("ss")),
                        color = textColor.copy(alpha = 0.7f),
                        style = timeStyle.copy(
                            fontSize = secondsSize,
                            fontWeight = FontWeight.Medium,
                        ),
                    )
                }
            }

            // FlowRow rather than Row: in portrait the full date plus weather plus battery can be
            // wider than the screen, and clipping at the edges beats nothing — wrapping beats both.
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                itemVerticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp),
            ) {
                val secondary = textColor.copy(alpha = 0.85f)

                Text(
                    text = time.format(DateTimeFormatter.ofPattern("EEEE, d MMMM")),
                    color = secondary,
                    fontSize = infoSize,
                )

                Text("·", color = secondary, fontSize = infoSize)

                when {
                    weather != null -> Text(
                        text = "${weatherEmoji(weather.kind, isNight)} ${weather.temperatureC.roundToInt()}°",
                        color = secondary,
                        fontSize = infoSize,
                    )
                    hasLocation -> Text("…", color = secondary, fontSize = infoSize)
                    else -> Text("long-press for settings", color = secondary, fontSize = infoSize)
                }

                Text("·", color = secondary, fontSize = infoSize)

                // Icon and percentage stay glued together across wraps.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    BatteryIcon(
                        percent = batteryPercent,
                        charging = batteryCharging,
                        plugged = batteryPlugged,
                        color = secondary,
                        modifier = Modifier.size(width = iconHeight * 1.9f, height = iconHeight),
                    )
                    Text("$batteryPercent%", color = secondary, fontSize = infoSize)
                }
            }
        }
    }
}

/** True when [now] falls inside [start]..[end], including windows that cross midnight. */
internal fun inWindow(now: LocalTime, start: LocalTime, end: LocalTime): Boolean = when {
    start == end -> false
    start.isBefore(end) -> !now.isBefore(start) && now.isBefore(end)
    else -> !now.isBefore(start) || now.isBefore(end)
}

/**
 * Day/night from today's sun times when weather data is present; a 07:00/19:00 fallback otherwise
 * (first launch, or offline). Sun times refresh with every weather fetch, so the date-match guard
 * only matters in the minutes right after midnight.
 */
private fun isNight(now: LocalDateTime, weather: WeatherState?): Boolean {
    val sunrise = weather?.sunrise
    val sunset = weather?.sunset
    return if (sunrise != null && sunset != null && sunrise.toLocalDate() == now.toLocalDate()) {
        now.isBefore(sunrise) || !now.isBefore(sunset)
    } else {
        now.hour < 7 || now.hour >= 19
    }
}
