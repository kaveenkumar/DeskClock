package com.deskclock.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import com.deskclock.app.weather.WeatherKind
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

private enum class EffectMode { RAIN, SNOW, STARS }

/** Fixed per-particle randomness; positions are pure functions of (seed, time), so the animation
 *  loop allocates nothing per frame. */
private class Particle(val a: Float, val b: Float, val c: Float, val d: Float)

/**
 * Ambient weather layer drawn *under* the clock text: rain streaks, snowflakes, twinkling stars on
 * clear nights, and a faint occasional flash during thunderstorms. Alphas are kept low on purpose —
 * the time must stay readable from across the room — and lower still at night.
 */
@Composable
fun WeatherEffects(kind: WeatherKind?, isNight: Boolean, modifier: Modifier = Modifier) {
    val mode = when (kind) {
        WeatherKind.RAIN, WeatherKind.THUNDER -> EffectMode.RAIN
        WeatherKind.SNOW -> EffectMode.SNOW
        WeatherKind.CLEAR -> if (isNight) EffectMode.STARS else null
        else -> null
    } ?: return

    // Capped at ~30fps: the panel refreshes at 60, but redrawing every other frame halves the GPU
    // load on a device that runs 24/7, and ambient particles don't need more.
    var frameNanos by remember { mutableLongStateOf(0L) }
    LaunchedEffect(mode) {
        var lastDrawn = 0L
        while (true) withFrameNanos { now ->
            if (now - lastDrawn >= 32_000_000L) {
                lastDrawn = now
                frameNanos = now
            }
        }
    }

    val particles = remember(mode) {
        val random = Random(mode.ordinal + 1)
        List(if (mode == EffectMode.STARS) 70 else 60) {
            Particle(random.nextFloat(), random.nextFloat(), random.nextFloat(), random.nextFloat())
        }
    }

    Canvas(modifier) {
        val t = frameNanos / 1_000_000_000f
        val w = size.width
        val h = size.height

        when (mode) {
            EffectMode.RAIN -> {
                val alpha = if (isNight) 0.13f else 0.28f
                val slant = w * 0.015f
                particles.forEach { p ->
                    val length = h * (0.05f + p.d * 0.05f)
                    val speed = h * (0.55f + p.b * 0.55f)
                    val span = h + length
                    val y = ((t * speed + p.c * span * 4f) % span) - length
                    val x = p.a * w
                    drawLine(
                        color = Color.White.copy(alpha = alpha),
                        start = Offset(x + slant, y),
                        end = Offset(x, y + length),
                        strokeWidth = 2.2f,
                        cap = StrokeCap.Round,
                    )
                }
                if (kind == WeatherKind.THUNDER) {
                    // Two quick decaying blinks every 16 s; capped low so it never lights the room.
                    val phase = t % 16f
                    val flash = when {
                        phase < 0.25f -> 1f - phase / 0.25f
                        phase >= 0.45f && phase < 0.75f -> 1f - (phase - 0.45f) / 0.3f
                        else -> 0f
                    }
                    if (flash > 0f) {
                        drawRect(Color.White.copy(alpha = flash * if (isNight) 0.05f else 0.10f))
                    }
                }
            }

            EffectMode.SNOW -> {
                val alpha = if (isNight) 0.28f else 0.55f
                particles.forEach { p ->
                    val speed = h * (0.05f + p.b * 0.07f)
                    val y = (t * speed + p.c * h) % (h + 12f) - 6f
                    val sway = sin(t * (0.3f + p.d * 0.5f) + p.c * 6.28f) * w * 0.03f
                    val x = (p.a * w + sway + w) % w
                    drawCircle(
                        color = Color.White.copy(alpha = alpha * (0.5f + p.d * 0.5f)),
                        radius = 2f + p.d * 3.5f,
                        center = Offset(x, y),
                    )
                }
            }

            EffectMode.STARS -> {
                particles.forEach { p ->
                    val twinkle = max(0f, sin(t * (0.25f + p.c * 0.9f) + p.d * 6.28f))
                    drawCircle(
                        color = Color.White.copy(alpha = 0.10f + 0.35f * twinkle * twinkle),
                        radius = 1f + p.b * 2f,
                        center = Offset(p.a * w, p.b * h * 0.9f),
                    )
                }
            }
        }
    }
}
