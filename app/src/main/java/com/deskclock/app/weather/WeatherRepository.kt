package com.deskclock.app.weather

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.LocalDateTime

enum class WeatherKind { CLEAR, PARTLY, CLOUDY, FOG, RAIN, SNOW, THUNDER }

data class WeatherState(
    val temperatureC: Double,
    val kind: WeatherKind,
    /** Today's sun times in the location's own timezone (`timezone=auto` in the request). */
    val sunrise: LocalDateTime,
    val sunset: LocalDateTime,
    val humidity: Int,
    val windKmh: Double,
    val uvIndexMax: Double,
    /** Today's maximum precipitation probability in percent; null when the model omits it. */
    val precipProbabilityMax: Int?,
    /** European AQI from the separate air-quality endpoint; null when that call fails. */
    val aqi: Int?,
)

data class Place(
    val name: String,
    val region: String?,
    val country: String?,
    val latitude: Double,
    val longitude: Double,
) {
    val displayName: String
        get() = listOfNotNull(name, region, country).joinToString(", ")
}

/**
 * Open-Meteo: keyless and free for non-commercial use, which keeps the app free of secrets and
 * accounts. Weather, air quality, and geocoding are separate endpoints of the same service.
 */
class WeatherRepository {

    suspend fun fetch(latitude: Double, longitude: Double): WeatherState = withContext(Dispatchers.IO) {
        val url = "https://api.open-meteo.com/v1/forecast" +
            "?latitude=$latitude&longitude=$longitude" +
            "&current=temperature_2m,weather_code,relative_humidity_2m,wind_speed_10m" +
            "&daily=sunrise,sunset,uv_index_max,precipitation_probability_max" +
            "&forecast_days=1&timezone=auto"
        val json = JSONObject(httpGet(url))
        val current = json.getJSONObject("current")
        val daily = json.getJSONObject("daily")

        // Air quality is a different host; its failure must not take the weather down with it.
        val aqi = runCatching {
            val aqiUrl = "https://air-quality-api.open-meteo.com/v1/air-quality" +
                "?latitude=$latitude&longitude=$longitude&current=european_aqi"
            JSONObject(httpGet(aqiUrl)).getJSONObject("current").getInt("european_aqi")
        }.getOrNull()

        WeatherState(
            temperatureC = current.getDouble("temperature_2m"),
            kind = kindOf(current.getInt("weather_code")),
            sunrise = LocalDateTime.parse(daily.getJSONArray("sunrise").getString(0)),
            sunset = LocalDateTime.parse(daily.getJSONArray("sunset").getString(0)),
            humidity = current.optInt("relative_humidity_2m", 0),
            windKmh = current.optDouble("wind_speed_10m", 0.0),
            uvIndexMax = daily.optJSONArray("uv_index_max")?.optDouble(0, 0.0) ?: 0.0,
            precipProbabilityMax = daily.optJSONArray("precipitation_probability_max")
                ?.takeIf { !it.isNull(0) }?.optInt(0),
            aqi = aqi,
        )
    }

    suspend fun geocode(query: String): List<Place> = withContext(Dispatchers.IO) {
        val url = "https://geocoding-api.open-meteo.com/v1/search" +
            "?name=${URLEncoder.encode(query, "UTF-8")}&count=6&language=en&format=json"
        val json = JSONObject(httpGet(url))
        val results = json.optJSONArray("results") ?: return@withContext emptyList()
        (0 until results.length()).map { i ->
            val place = results.getJSONObject(i)
            Place(
                name = place.getString("name"),
                region = place.optString("admin1").takeIf { it.isNotBlank() },
                country = place.optString("country").takeIf { it.isNotBlank() },
                latitude = place.getDouble("latitude"),
                longitude = place.getDouble("longitude"),
            )
        }
    }

    private fun httpGet(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        return try {
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.inputStream.bufferedReader().readText()
        } finally {
            connection.disconnect()
        }
    }

    private fun kindOf(wmoCode: Int): WeatherKind = when (wmoCode) {
        0 -> WeatherKind.CLEAR
        1, 2 -> WeatherKind.PARTLY
        3 -> WeatherKind.CLOUDY
        45, 48 -> WeatherKind.FOG
        in 51..67, in 80..82 -> WeatherKind.RAIN
        in 71..77, 85, 86 -> WeatherKind.SNOW
        95, 96, 99 -> WeatherKind.THUNDER
        else -> WeatherKind.CLOUDY
    }
}
