package com.deskclock.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deskclock.app.settings.BackgroundMode
import com.deskclock.app.settings.BlinkMode
import com.deskclock.app.settings.DimMode
import com.deskclock.app.settings.SettingsData
import com.deskclock.app.weather.Place
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

private val TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm")

private fun parseTime(text: String): LocalTime? =
    runCatching { LocalTime.parse(text.trim(), DateTimeFormatter.ofPattern("H:mm")) }.getOrNull()

@Composable
fun SettingsDialog(
    settings: SettingsData,
    onUse24hChange: (Boolean) -> Unit,
    onSelectPlace: (Place) -> Unit,
    search: suspend (String) -> List<Place>,
    onDimModeChange: (DimMode) -> Unit,
    onBackgroundChange: (BackgroundMode) -> Unit,
    onBlinkChange: (BlinkMode) -> Unit,
    onWeatherEffectsChange: (Boolean) -> Unit,
    onFontChange: (ClockFont) -> Unit,
    onDimWindowChange: (LocalTime, LocalTime) -> Unit,
    onDimBrightnessChange: (Float) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Place>>(emptyList()) }
    var searched by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    var startText by remember { mutableStateOf(settings.dimStart.format(TIME_FORMAT)) }
    var endText by remember { mutableStateOf(settings.dimEnd.format(TIME_FORMAT)) }
    // Slider works on local state and commits on release, so a drag doesn't spam prefs writes.
    var brightness by remember { mutableFloatStateOf(settings.dimBrightness) }

    fun runSearch() {
        if (query.isBlank()) return
        scope.launch {
            results = search(query.trim())
            searched = true
        }
    }

    fun commitWindow() {
        val start = parseTime(startText) ?: return
        val end = parseTime(endText) ?: return
        onDimWindowChange(start, end)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
        title = { Text("DeskClock settings") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = "Location: ${settings.placeName ?: "not set"}",
                    style = MaterialTheme.typography.bodyMedium,
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        label = { Text("City") },
                    )
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = ::runSearch) { Text("Search") }
                }

                if (searched && results.isEmpty()) {
                    Text("No matches (or offline).", style = MaterialTheme.typography.bodySmall)
                }

                Column(Modifier.heightIn(max = 160.dp).verticalScroll(rememberScrollState())) {
                    results.forEach { place ->
                        TextButton(
                            onClick = {
                                onSelectPlace(place)
                                results = emptyList()
                                searched = false
                                query = ""
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(place.displayName, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("24-hour time", Modifier.weight(1f))
                    Switch(checked = settings.use24h, onCheckedChange = onUse24hChange)
                }

                Column {
                    Text("Clock font")
                    val selectedFont = runCatching { ClockFont.valueOf(settings.clockFont) }
                        .getOrDefault(ClockFont.ROBOTO)
                    Column(
                        Modifier
                            .heightIn(max = 220.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        ClockFont.entries.forEach { font ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onFontChange(font) }
                                    .padding(vertical = 2.dp),
                            ) {
                                RadioButton(
                                    selected = selectedFont == font,
                                    onClick = { onFontChange(font) },
                                )
                                Text(
                                    font.label,
                                    Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Text(
                                    "12:34",
                                    fontFamily = font.family(),
                                    fontWeight = font.weight,
                                    fontSize = 17.sp,
                                )
                            }
                        }
                    }
                }

                Column {
                    Text("Background")
                    Text(
                        "Dark and Black are kinder to the OLED panel",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                        BackgroundMode.entries.forEachIndexed { index, mode ->
                            SegmentedButton(
                                selected = settings.background == mode,
                                onClick = { onBackgroundChange(mode) },
                                shape = SegmentedButtonDefaults.itemShape(index, BackgroundMode.entries.size),
                            ) {
                                Text(
                                    when (mode) {
                                        BackgroundMode.COLORFUL -> "Colorful"
                                        BackgroundMode.DARK -> "Dark"
                                        BackgroundMode.BLACK -> "Black"
                                    },
                                )
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Text("Weather effects")
                        Text(
                            "Rain, snow, stars and thunder animations",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Switch(checked = settings.weatherEffects, onCheckedChange = onWeatherEffectsChange)
                }

                Column {
                    Text("Separator blink")
                    Text(
                        when (settings.blink) {
                            BlinkMode.STEADY -> "Always on"
                            BlinkMode.EVERY_SECOND -> "Dips within every second"
                            BlinkMode.ALTERNATE -> "Bright and dim on alternate seconds"
                        },
                        style = MaterialTheme.typography.bodySmall,
                    )
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                        BlinkMode.entries.forEachIndexed { index, mode ->
                            SegmentedButton(
                                selected = settings.blink == mode,
                                onClick = { onBlinkChange(mode) },
                                shape = SegmentedButtonDefaults.itemShape(index, BlinkMode.entries.size),
                            ) {
                                Text(
                                    when (mode) {
                                        BlinkMode.STEADY -> "Steady"
                                        BlinkMode.EVERY_SECOND -> "Pulse"
                                        BlinkMode.ALTERNATE -> "Alternate"
                                    },
                                )
                            }
                        }
                    }
                }

                HorizontalDivider()

                Column {
                    Text("Dimming")
                    Text(
                        when (settings.dimMode) {
                            DimMode.OFF -> "The screen never dims"
                            DimMode.SCHEDULE -> "The screen dims inside the window below"
                            DimMode.SUN -> "The screen dims from sunset to sunrise"
                        },
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    DimMode.entries.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = settings.dimMode == mode,
                            onClick = { onDimModeChange(mode) },
                            shape = SegmentedButtonDefaults.itemShape(index, DimMode.entries.size),
                        ) {
                            Text(
                                when (mode) {
                                    DimMode.OFF -> "Off"
                                    DimMode.SCHEDULE -> "Schedule"
                                    DimMode.SUN -> "Sunset"
                                },
                            )
                        }
                    }
                }

                if (settings.dimMode == DimMode.SCHEDULE) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = startText,
                            onValueChange = { startText = it; commitWindow() },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            label = { Text("From") },
                            isError = parseTime(startText) == null,
                        )
                        OutlinedTextField(
                            value = endText,
                            onValueChange = { endText = it; commitWindow() },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            label = { Text("To") },
                            isError = parseTime(endText) == null,
                        )
                    }
                }

                if (settings.dimMode != DimMode.OFF) {
                    Column(Modifier.padding(top = 4.dp)) {
                        Text(
                            "Dim brightness: ${(brightness * 100).roundToInt()}%" +
                                if (brightness >= 0.99f) " (no dimming)" else "",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Slider(
                            value = brightness,
                            onValueChange = { brightness = it },
                            onValueChangeFinished = { onDimBrightnessChange(brightness) },
                            valueRange = 0.05f..1f,
                        )
                    }
                }
            }
        },
    )
}
