# Changelog

## 0.9.0 (2026-08-30) — versionCode 10

- The date/weather/battery row moved above the clock; a new conditions strip sits along the bottom
  edge: sunrise, sunset, precipitation probability, UV index, air quality (European AQI from
  Open-Meteo's air-quality endpoint), moon phase (computed locally), humidity, and wind.
- The screen is now one vertical column with weighted spacers, so no element can overlap another
  regardless of font or wrapping; time height budget adjusted for the two strips.

## 0.8.0 (2026-08-28) — versionCode 9

- The clock is now sized from the ACTUAL time string instead of an all-zeros template, so
  proportional fonts with narrow digits (1, 7) fill the screen; size changes on digit rollover
  animate smoothly.
- 14 more fonts (27 total): Anton, Fjalla One, Unica One, Righteous, Bungee, Michroma, Zen Dots,
  Aldrich, Quantico, Chakra Petch, Iceland, Wallpoet, Major Mono Display, DM Serif Display.
- Fixed the separator jumping sideways when blinking in kerned fonts (Poiret One after a 7): the
  colon is now its own text blinked via a pure alpha layer, instead of a color span that split the
  text shaping runs only in the dimmed state.
- Separator toned down to the seconds counter's subtlety: 70% alpha steady, dipping to 30% —
  a full-brightness blink at the centre of vision was distracting.

## 0.7.1 (2026-08-28) — versionCode 8

- "Weather effects" switch in settings to turn the rain/snow/stars/thunder animations on or off
  (on by default). Off also stops the 30 fps redraw loop entirely.

## 0.7.0 (2026-08-28) — versionCode 7

- Separator blink is now selectable: Steady (always on), Pulse (dips within every second), or
  Alternate (bright/dim on alternate seconds, the previous behaviour and still the default).
- Background is now a three-way choice: Colorful / Dark / Black. Black is pure black at all times —
  only the text and weather effects emit light. The old "Dark backgrounds" switch migrates.

## 0.6.0 (2026-08-28) — versionCode 6

- 13 selectable clock fonts (Roboto plus 12 bundled OFL faces from Google Fonts: Orbitron,
  Audiowide, Oswald, Bebas Neue, Rubik Mono One, Share Tech Mono, VT323, Press Start 2P, Doto,
  Monoton, Poiret One, Comfortaa), picked in settings with live previews.
- Fixed the info row overflowing the screen edges in portrait: it now wraps to a second line when
  needed.

## 0.5.0 (2026-08-27) — versionCode 5

- "Dark backgrounds" option, ON by default: daytime backgrounds become deep weather-tinted shades
  instead of bright gradients. Lit pixels age an OLED (blue subpixels fastest), so a bright blue
  field shown all day is the worst case for a 24/7 panel; turn the switch off for the colorful look.

## 0.4.0 (2026-08-27) — versionCode 4

- Dimming is now a three-way mode: Off / Schedule (fixed window) / Sunset (sunset→sunrise).
  Existing on/off choices migrate automatically.
- Bigger clock: trimmed Roboto's unused line padding around digits (~17% of the line) and raised
  the height budget, so the glyphs themselves now fill the space.
- The time separator blinks with the seconds (soft fade, no strobing).
- New launcher icon: seven-segment "12:34".

## 0.3.0 (2026-08-27) — versionCode 3

- Small seconds counter beside the big time (stacked under AM/PM in 12-hour mode).
- Dimming now happens ONLY inside the schedule window — never from sunset (early winter sunsets no
  longer darken the evening). Sun times still drive the color theme.
- "Dim strength" replaced by "Dim brightness": 100% = no dimming, lower = dimmer screen. The value
  now directly sets the backlight level inside the window.
- Weather animations capped at ~30 fps to halve the constant GPU load on an always-on device.

## 0.2.0 (2026-08-27) — versionCode 2

- Scheduled dimming: user-set window (default 22:00–06:00) with adjustable strength; when the
  schedule is off, dimming follows sunset→sunrise as before. Colors still follow the real sun.
- Animated weather backgrounds: rain streaks, snowflakes, twinkling stars on clear nights, faint
  thunderstorm flashes — all low-alpha and drawn behind the clock so the time stays readable.
- Plugged-in indicator: the battery bolt now shows whenever power is connected — solid while
  actively charging, faint while a charge limiter holds the level (e.g. an 80% charge stop).
- Bigger time: the font size is now computed from measured text so the clock fills the free space
  in either orientation.

## 0.1.0 (2026-08-27) — versionCode 1

Initial version.

- Fullscreen always-on clock: big auto-sized time, info row with date/day, weather, battery.
- Open-Meteo weather (keyless) with city search in settings; no location permission.
- Day/night theme from real sunrise/sunset; weather-reactive background gradients.
- OLED burn-in protection (per-minute layout drift) and night backlight dimming.
