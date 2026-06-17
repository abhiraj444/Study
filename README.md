# StudyFlow — Voice-First Android Study Tracker

**StudyFlow** is an Android app that lets you log study sessions by voice, hands-free, using Google Assistant / Gemini **App Actions**.

> Say *"Hey Google, start studying Math chapter 3 theory"* — a session starts automatically.
> Later, say *"Hey Google, I'm done studying"* — the session is logged with full metadata.

---

## How the Google Assistant / Gemini integration works

Google's voice assistant (Assistant on most devices, Gemini on newer Pixels) uses **App Actions** declared in [`shortcuts.xml`](app/src/main/res/xml/shortcuts.xml) to map natural-language queries to specific functionality inside your app. As of 2025 this is still the documented mechanism — the older `actions.xml` format was deprecated, but `shortcuts.xml` custom intents continue to be the supported path.

### Flow

1. **User speaks** → *"Hey Google, start studying Math chapter 3 theory"*
2. **Assistant matches the query** against `queryPatterns` declared in `strings.xml` (`start_study_queries` array)
3. **Assistant extracts parameters** (`$subject$`, `$chapter$`, `$mode$`) using `@schema.org/Text` typing
4. **Assistant fires an intent** — `android.intent.action.VIEW` targeting `com.studyflow.ui.SessionActivity`, with extras:
   - `subject` = "Math"
   - `chapter` = "chapter 3"
   - `mode` = "theory"
5. **`SessionActivity.handleVoiceIntent()`** reads the extras and calls `SessionViewModel.handleVoiceStart()`, which:
   - Inserts a new `StudySession` row (with `end_time = NULL`)
   - Starts the `StudyTimerService` foreground service (persistent notification with elapsed time)
6. When the user later says *"Hey Google, I'm done studying"*, the **STOP_STUDY** capability fires the same activity with `action = "STOP"`, which calls `StopSessionUseCase`:
   - Sets `end_time` and `duration_seconds` on the session row
   - Updates the user's streak
   - Updates the daily-goal progress
   - Stops the foreground service
   - Pops up a rating sheet (mood + focus + notes)

### Three custom capabilities

| Capability | Voice examples | Parameters |
|---|---|---|
| `com.studyflow.START_STUDY` | "start studying Math chapter 3 theory" | subject, chapter?, mode? |
| `com.studyflow.STOP_STUDY` | "I am done studying" | — |
| `com.studyflow.TAKE_BREAK` | "take a break for 10 minutes" | duration |

### Long-press launcher shortcuts

Two static shortcuts are also declared so the user can long-press the app icon to start/stop without voice.

---

## Project structure

```
StudyFlow/
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
└── app/
    ├── build.gradle.kts
    ├── proguard-rules.pro
    └── src/main/
        ├── AndroidManifest.xml
        ├── kotlin/com/studyflow/
        │   ├── StudyFlowApp.kt              # @HiltAndroidApp
        │   ├── data/
        │   │   ├── db/                      # Room entities + DAOs + database
        │   │   ├── repository/              # SessionRepository
        │   │   └── datastore/               # UserPreferencesDataStore
        │   ├── domain/
        │   │   ├── model/                   # SessionInfo, StreakInfo
        │   │   └── usecase/                 # Start/Stop/Stats/Streak use cases
        │   ├── service/                     # StudyTimerService (foreground)
        │   ├── receiver/                    # Boot, Alarm, Notification action receivers
        │   ├── work/                        # ReminderSchedulingWorker
        │   ├── di/AppModule.kt              # Hilt providers
        │   └── ui/
        │       ├── MainActivity.kt          # Dashboard + nav host
        │       ├── SessionActivity.kt       # ★ App Actions entry point ★
        │       ├── navigation/BottomNav.kt
        │       ├── home/                    # Dashboard
        │       ├── session/                 # Live timer screen + ViewModel
        │       ├── history/                 # Full session log
        │       ├── analytics/               # Charts + stats
        │       ├── subjects/                # Subject manager
        │       ├── settings/                # Settings + CSV export
        │       ├── onboarding/              # 4-screen intro flow
        │       ├── components/              # Reusable UI bits
        │       └── theme/                   # Color, Typography, Theme
        └── res/
            ├── values/strings.xml           # Voice query patterns ★
            ├── values/colors.xml
            ├── values/themes.xml
            ├── values-night/themes.xml
            ├── xml/shortcuts.xml            # ★ App Actions capabilities ★
            ├── xml/backup_rules.xml
            ├── xml/data_extraction_rules.xml
            ├── drawable/                    # Adaptive launcher icon
            └── mipmap-*/                    # Launcher icon densities
```

---

## How to build

### Prerequisites
- **Android Studio Koala (2024.1.1) or newer**
- **JDK 17** (bundled with Android Studio)
- **Android SDK** with `compileSdk = 35` and `minSdk = 26` (Android 8.0+)
- A physical Android device (App Actions testing does NOT work on emulators)

### Steps

1. Open the `StudyFlow/` folder in Android Studio.
2. Let Gradle sync finish (it will download Compose, Hilt, Room, Vico, etc.).
3. Plug in a physical device with USB debugging on.
4. Click **Run** ▶ to install on the device.

### Generating a signed APK / AAB

```bash
cd StudyFlow
./gradlew assembleRelease     # → app/build/outputs/apk/release/app-release-unsigned.apk
./gradlew bundleRelease       # → app/build/outputs/bundle/release/app-release.aab
```

For a quick debug APK:

```bash
./gradlew assembleDebug        # → app/build/outputs/apk/debug/app-debug.apk
```

---

## How to test the Google Assistant / Gemini integration

### Option A — Local testing via the App Actions Test Tool (recommended during dev)

1. In Android Studio: **Tools → Google Assistant → App Actions Test Tool**.
2. Sign in with the same Google account that's on your phone.
3. Select `com.studyflow` as the package, then pick a capability (e.g. `START_STUDY`).
4. Fill in the parameter preview (`subject=Physics`, `chapter=Optics`, `mode=theory`).
5. Click **Run**. Assistant will open on your phone and invoke the app.

### Option B — ADB intent simulation (no Google account needed, fastest)

You can simulate exactly what Assistant sends by firing the same intent:

```bash
# Start a session
adb shell am start -n com.studyflow/.ui.SessionActivity \
    --es subject "Math" --es chapter "Trigonometry" --es mode "theory"

# Stop the active session
adb shell am start -n com.studyflow/.ui.SessionActivity \
    --es action "STOP"

# Take a 10-minute break
adb shell am start -n com.studyflow/.ui.SessionActivity \
    --es breakDuration "10 minutes"
```

### Option C — Production voice testing

For end users to actually say "Hey Google, start studying Math":

1. Publish the app to Google Play (even **internal testing track** is enough).
2. Use the **same Google account** in Android Studio, on the device's Google app, and in Play Console.
3. Opt in your account to App Actions testing in the Play Console under **Setup → Advanced Settings → App Actions testers**.
4. Within ~30 minutes, say *"Hey Google, start studying Math"* on your device.

> **Note:** Google is gradually migrating Assistant to Gemini. As of late 2025, **Gemini continues to honor `shortcuts.xml` App Actions** — the same `shortcuts.xml` file works for both. There is also a newer "App Functions" API emerging for richer Gemini integration (see roadmap below).

---

## Tech stack (matches the spec)

| Layer | Tech |
|---|---|
| Language | Kotlin 1.9.24 |
| UI | Jetpack Compose (BOM 2024.06.00) + Material 3 |
| Architecture | MVVM + Repository + Clean Architecture |
| Local DB | Room 2.6.1 |
| DI | Hilt 2.51 |
| Voice | Google App Actions (`shortcuts.xml` + custom intents) |
| Background | WorkManager 2.9.0 + AlarmManager |
| Foreground timer | `StudyTimerService` (FOREGROUND_SERVICE_TYPE_SPECIAL_USE) |
| Preferences | DataStore (Preferences) |
| Navigation | Jetpack Navigation Compose |
| Charts | Vico (compose-m3) — included but a simple text-bar fallback is used in AnalyticsScreen |

---

## Permissions explained (spec §10)

| Permission | Why |
|---|---|
| `FOREGROUND_SERVICE` | Keep `StudyTimerService` running while you study |
| `FOREGROUND_SERVICE_SPECIAL_USE` | Android 14+ requires a typed FGS — study timer doesn't fit media/location/health categories |
| `POST_NOTIFICATIONS` | Show the persistent session notification (runtime prompt on Android 13+) |
| `WAKE_LOCK` | Prevent timer drift when the screen is off |
| `SCHEDULE_EXACT_ALARM` + `USE_EXACT_ALARM` | Fire streak-at-risk reminders at the right minute |
| `RECEIVE_BOOT_COMPLETED` | Reschedule reminders after reboot |
| `VIBRATE` | Optional haptic feedback on stop |

---

## Edge cases handled (spec §11)

- **Voice starts while a session is already active** → previous session auto-stops, snackbar shown.
- **Voice stops with no active session** → toast "No active session found".
- **App killed mid-session** → on next launch, the dashboard banner shows the still-open session; the foreground service auto-restarts the timer on `onStartCommand`.
- **Session < 1 minute** → still logged, but excluded from streak calculation.
- **Streak rule** → a day counts only if total study time ≥ 15 minutes.
- **Midnight crossover** → attributed to the start date.

---

## Roadmap — moving beyond App Actions

Google has signalled a future "App Functions" API for richer Gemini integration (functional calling rather than intent dispatch). When that API stabilizes, the migration path is:

1. Keep `shortcuts.xml` (works for both Assistant and Gemini today).
2. Add an `AppFunctionsProvider` that exposes `startStudy(subject, chapter, mode)`, `stopStudy()`, `takeBreak(duration)` as Kotlin functions annotated with `@AppFunction`.
3. Gemini will then be able to *call* these functions conversationally (e.g. "I want to study trigonometry for about 30 minutes then take a 5-minute break" — multiple function calls in one utterance).

The current code architecture is already compatible — the use cases (`StartSessionUseCase`, `StopSessionUseCase`) are pure entry points that any future function wrapper can call directly.

---

## License

MIT — see headers. Built to the supplied spec (`StudyTracker_AppSpec.md`).
# Study
