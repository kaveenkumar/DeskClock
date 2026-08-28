package com.deskclock.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.deskclock.app.battery.BatteryState
import com.deskclock.app.battery.batteryFlow
import com.deskclock.app.settings.Settings
import com.deskclock.app.settings.SettingsData
import com.deskclock.app.weather.Place
import com.deskclock.app.weather.WeatherRepository
import com.deskclock.app.weather.WeatherState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDateTime
import kotlin.time.Duration.Companion.minutes

class DeskClockViewModel(app: Application) : AndroidViewModel(app) {

    private val settings = Settings(app)
    private val weatherRepository = WeatherRepository()

    val settingsState: StateFlow<SettingsData> = settings.state

    private val _weather = MutableStateFlow<WeatherState?>(null)
    val weather: StateFlow<WeatherState?> = _weather

    val battery: StateFlow<BatteryState> = batteryFlow(app)
        .stateIn(viewModelScope, SharingStarted.Eagerly, BatteryState(percent = 0, charging = false, plugged = false))

    /** Second ticker, aligned to wall-clock second boundaries for the seconds counter. */
    val time: StateFlow<LocalDateTime> = secondTicker()
        .stateIn(viewModelScope, SharingStarted.Eagerly, LocalDateTime.now())

    init {
        viewModelScope.launch {
            settings.state
                .map { it.latitude to it.longitude }
                .distinctUntilChanged()
                .collectLatest { (latitude, longitude) ->
                    if (latitude == null || longitude == null) return@collectLatest
                    while (true) {
                        val fetched = runCatching { weatherRepository.fetch(latitude, longitude) }
                            .onSuccess { _weather.value = it }
                            .isSuccess
                        // Short retry when offline so the row fills in soon after Wi-Fi returns.
                        delay(if (fetched) 30.minutes else 2.minutes)
                    }
                }
        }
    }

    fun setLocation(place: Place) =
        settings.setLocation(place.latitude, place.longitude, place.displayName)

    fun setUse24h(use24h: Boolean) = settings.setUse24h(use24h)

    fun setDimMode(mode: com.deskclock.app.settings.DimMode) = settings.setDimMode(mode)

    fun setDimWindow(start: java.time.LocalTime, end: java.time.LocalTime) =
        settings.setDimWindow(start, end)

    fun setDimBrightness(brightness: Float) = settings.setDimBrightness(brightness)

    fun setBackground(mode: com.deskclock.app.settings.BackgroundMode) = settings.setBackground(mode)

    fun setBlink(mode: com.deskclock.app.settings.BlinkMode) = settings.setBlink(mode)

    fun setWeatherEffects(enabled: Boolean) = settings.setWeatherEffects(enabled)

    fun setClockFont(fontName: String) = settings.setClockFont(fontName)

    suspend fun searchCity(query: String): List<Place> =
        runCatching { weatherRepository.geocode(query) }.getOrDefault(emptyList())

    private fun secondTicker() = kotlinx.coroutines.flow.flow {
        while (true) {
            val now = LocalDateTime.now()
            emit(now)
            val nextSecond = now.withNano(0).plusSeconds(1)
            delay(Duration.between(now, nextSecond).toMillis().coerceAtLeast(10))
        }
    }
}
