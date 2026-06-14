# Edge Panel — Android App

A Samsung-style floating edge panel for any Android device (API 26+).

## Features
- 🔲 **Floating overlay panel** with smooth slide-in animation
- 🎨 **Handle customisation** — color, size, width, transparency, left/right position, draggable
- 📋 **Apps Panel** — launch pinned apps in one tap
- ✅ **Tasks Panel** — add/complete/delete to-dos
- 👥 **People Panel** — quick call or SMS pinned contacts
- 🌤 **Weather Panel** — live weather via OpenWeatherMap
- 🪟 **Floating window controls:**
  - **Top bar:** ✕ Close | ⛶ Fullscreen toggle
  - **Bottom:** ⬇/⬆ Minimize ↔ Maximize toggle
- 📌 **Draggable** — drag title bar to reposition anywhere
- 🔄 **Auto-start** on reboot when enabled
- 🔔 **Foreground service** notification with Stop action

## Setup

### 1. Open in Android Studio
```
File > Open > select /EdgePanel folder
```

### 2. Add Weather API Key
In `WeatherFetcher.kt`, replace:
```kotlin
private const val API_KEY = "YOUR_OPENWEATHER_API_KEY"
```
Get a free key at: https://openweathermap.org/api

### 3. Add launcher icons
Place `ic_launcher.png` and `ic_launcher_round.png` in `res/mipmap-*` folders,
or use Android Studio's Image Asset tool (right-click res > New > Image Asset).

### 4. Build & Install
```bash
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 5. First Run
1. Open **Edge Panel** app
2. Tap **Grant Overlay Permission** and allow it
3. Toggle **Enable Edge Panel** ON
4. Go to **Manage Panels** to enable/disable panels
5. Go to **Handle Settings** to customise the handle
6. Tap each panel's **Edit** button inside the panel to add apps/contacts

## Permissions Required
| Permission | Purpose |
|---|---|
| `SYSTEM_ALERT_WINDOW` | Draw overlay panel over other apps |
| `FOREGROUND_SERVICE` | Keep service running |
| `READ_CONTACTS` | Show contacts in People panel |
| `CALL_PHONE` | Quick-dial from People panel |
| `INTERNET` | Fetch weather data |
| `ACCESS_COARSE_LOCATION` | Auto-detect city for weather |
| `RECEIVE_BOOT_COMPLETED` | Auto-start on device reboot |

## Project Structure
```
EdgePanel/
├── app/src/main/
│   ├── java/com/edgepanel/
│   │   ├── service/
│   │   │   ├── EdgePanelService.kt   ← Core overlay service
│   │   │   └── BootReceiver.kt
│   │   ├── ui/
│   │   │   ├── MainActivity.kt
│   │   │   ├── HandleSettingsActivity.kt
│   │   │   ├── PanelManagerActivity.kt
│   │   │   ├── AppsPanelEditActivity.kt
│   │   │   └── PeoplePanelEditActivity.kt
│   │   ├── adapter/
│   │   │   ├── AppsPickerAdapter.kt
│   │   │   └── ContactsPickerAdapter.kt
│   │   ├── model/
│   │   │   └── Models.kt
│   │   └── utils/
│   │       ├── Prefs.kt
│   │       └── WeatherFetcher.kt
│   └── res/
│       ├── layout/       ← All XML layouts
│       ├── drawable/     ← Shapes and icons
│       └── values/       ← Strings, themes, dimens
```
