# Quiet For A Minute — iOS

SwiftUI port of the Android app. Feature-equivalent with platform-appropriate implementations.

## iOS vs Android Differences

| Feature | Android | iOS |
|---|---|---|
| Volume monitoring | Background `BroadcastReceiver` | `AVAudioSession` KVO (foreground + audio background mode) |
| Mute popup | System overlay (`SYSTEM_ALERT_WINDOW`) | In-app bottom sheet |
| Set system volume | `AudioManager.setStreamVolume()` | Hidden `MPVolumeView` slider (widely-used workaround) |
| Scheduled mutes | Exact `AlarmManager` alarms | `UNCalendarNotificationTrigger` (repeating local notifications) |
| Persistence | Room + DataStore | UserDefaults + JSON (Codable) |
| DI | Hilt | SwiftUI `@EnvironmentObject` |

## Requirements

- **Xcode 15+**
- **iOS 16.0+** deployment target (iOS 17 for full SwiftUI features)
- macOS Ventura or later

## Project Setup (Xcode)

The source files are ready. You need to create the Xcode project wrapper:

### Step 1 — Create a new Xcode project

1. Open Xcode → **File → New → Project**
2. Choose **iOS → App**
3. Fill in:
   - **Product Name:** `QuietForAMinute`
   - **Bundle Identifier:** `com.admoseley.quietforaminute`
   - **Interface:** SwiftUI
   - **Language:** Swift
   - **Storage:** None (we use UserDefaults)
4. **Uncheck** "Include Tests" (no tests yet)
5. Save the project inside this `ios/` folder

### Step 2 — Replace generated files

Delete the generated `ContentView.swift` and `[AppName]App.swift` that Xcode creates. Then drag the entire `QuietForAMinute/` source folder from this repo into the Xcode project navigator, making sure **"Copy items if needed"** is **unchecked** (the files are already in the right place).

### Step 3 — Add capabilities

In Xcode, select the project target → **Signing & Capabilities** tab → click **+**:

1. **Background Modes** — check **"Audio, AirPlay, and Picture in Picture"**
   - This keeps the `AVAudioSession` alive so volume monitoring works when the app is backgrounded.

2. **Push Notifications** (optional — only needed if you add remote push later; local notifications work without this)

### Step 4 — Info.plist keys

Add the following to `Info.plist` (or the target's Info tab in Xcode 14+):

| Key | Value |
|---|---|
| `NSMicrophoneUsageDescription` | Not needed |
| `UIBackgroundModes` | `audio` (added automatically by the Background Modes capability) |

### Step 5 — Build & Run

Select a simulator or a connected iPhone, then press **⌘R**. The app will:
- Request notification permission on first launch
- Begin volume monitoring immediately
- Show the mute duration sheet the moment media volume hits zero

## Source File Overview

```
QuietForAMinute/
├── QuietForAMinuteApp.swift      — @main entry point, wires services together
├── AppDelegate.swift             — handles notification actions (Start Timer / Skip)
│
├── Models/
│   └── Schedule.swift            — Schedule data model (Codable, Identifiable)
│
├── Data/
│   ├── ScheduleStore.swift       — CRUD + persistence (UserDefaults/JSON)
│   └── PreferencesStore.swift    — all user preferences (@Published + UserDefaults)
│
├── Services/
│   ├── VolumeMonitor.swift       — AVAudioSession KVO, silent background audio
│   ├── VolumeController.swift    — sets system volume via hidden MPVolumeView
│   ├── MuteTimerService.swift    — countdown timer, notification updates, restore
│   ├── NotificationScheduler.swift — UNUserNotificationCenter schedule/cancel
│   └── ChimePlayer.swift         — plays one-shot system sounds via AudioServices
│
└── Views/
    ├── MainTabView.swift          — TabView (Settings / Schedules) + timer banner
    ├── Mute/
    │   └── MuteDurationSheet.swift — bottom sheet: duration picker + restore volume
    ├── Settings/
    │   ├── SettingsView.swift
    │   └── SettingsViewModel.swift
    └── Schedules/
        ├── ScheduleListView.swift  — list with swipe-to-delete, enable toggle
        ├── ScheduleEditView.swift  — create / edit / delete a schedule
        └── SchedulesViewModel.swift
```

## Architecture

Follows the same MVVM pattern as the Android app:

- **`ScheduleStore`** — equivalent of `ScheduleRepository` + Room DAO
- **`PreferencesStore`** — equivalent of `PreferencesRepository` (DataStore)
- **`VolumeMonitor`** — equivalent of `VolumeReceiver`
- **`VolumeController`** — equivalent of `AudioManager.setStreamVolume()`
- **`MuteTimerService`** — equivalent of `MuteTimerService` foreground service
- **`NotificationScheduler`** — equivalent of `AlarmScheduler` + `AlarmReceiver`
- **`ChimePlayer`** — direct equivalent

Services are shared via SwiftUI `@EnvironmentObject` — the equivalent of Hilt `@Singleton` injection.

## Known Limitations

1. **Volume monitoring in background** — requires the audio background mode and a silent looping audio session. Apple permits this for apps with legitimate audio use-cases. Review before App Store submission.

2. **Setting system volume** — uses the hidden `MPVolumeView` slider workaround. This is a grey-area technique (not a private API, but not officially documented). It works reliably on real devices and has been used by thousands of App Store apps.

3. **Scheduled mute popup** — on Android, a full-screen overlay appears at alarm time. On iOS, a local notification fires instead. The user taps "Start Timer" in the notification to begin the mute — the app opens and the timer starts automatically.

4. **Background exact timing** — `UNCalendarNotificationTrigger` is not guaranteed to fire at an exact second the way Android's `AlarmManager.ELAPSED_REALTIME_WAKEUP` does. In practice, it fires within a few seconds for foreground-delivered notifications.
