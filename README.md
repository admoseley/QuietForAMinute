# Quiet For A Minute

**Mute when you need it. Restore when you forget.**

Quiet For A Minute is an Android app that takes the friction out of temporary muting. Whether you're stepping into a meeting, a class, or just need a moment of quiet, the app mutes your device for a set duration — then automatically restores your volume when the time is up, so you never miss another call or notification because you forgot to unmute.

## Features

### Manual Mute (On-Demand)
When you mute your device by pressing the volume-down button all the way to zero, Quiet For A Minute intercepts that action and pops up a dialog asking how long you want to stay muted. Pick your duration, and the app handles the rest — counting down in a foreground notification and restoring your volume automatically when the timer expires.

- Set mute duration with a time picker (hours and minutes)
- Choose the volume level to restore to (adjustable per-mute)
- Skip the timer if you just want a manual mute with no auto-restore
- Optional chime sounds on mute and on restore, so you know exactly when it happened

### Scheduled Mutes (Set It and Forget It)
Create recurring mute schedules for events that happen on a regular basis — a weekly team meeting, a daily class, a recurring appointment. Each schedule fires at a precise time, mutes your device for the configured duration, and re-arms itself for the next occurrence automatically.

- Schedule by day of week (weekdays, weekends, or any combination)
- Set a specific start time and mute duration per schedule
- Enable or disable individual schedules without deleting them
- Schedules survive device reboots (re-armed on boot via `BootReceiver`)

### Settings
- **Restore Volume** — set a default volume level that all mute timers restore to
- **Overlay toggle** — enable or disable the automatic popup when you mute manually
- **Chime on mute / Chime on restore** — independently toggle audio feedback, with your choice of any system ringtone
- **Theme** — Light, Dark, or System default

### Permissions
The app requires two special permissions to operate:

| Permission | Purpose |
|---|---|
| Display over other apps | Show the mute timer popup over the lock screen / volume panel |
| Schedule exact alarms | Fire scheduled mutes at a precise time |

Both are requested only when needed and explained in the Settings screen with direct links to the relevant system settings page.

## How It Works

The app runs two foreground services:

- **OverlayService** — always-on monitor that listens for volume changes and shows the mute duration dialog when your volume hits zero
- **MuteTimerService** — on-demand countdown timer that mutes the device, manages a notification with the remaining time, and restores volume when done

Scheduled mutes are managed by `AlarmManager` with exact-alarm PendingIntents, one per (schedule × day-of-week) combination, so each occurrence fires independently and reliably.

## Building

Requires **Android Studio** with JDK 21+ and Android SDK (min SDK 26, target SDK 34).

```bash
# Build a debug APK
./gradlew assembleDebug

# Build and install on a connected device or emulator
./gradlew installDebug

# Clean build artifacts
./gradlew clean
```

## Tech Stack

| Layer | Library |
|---|---|
| Language | Kotlin 2.1.0 |
| UI | Jetpack Compose + Material 3 (BOM 2024.12.01) |
| Dependency Injection | Hilt 2.59.2 |
| Local Database | Room 2.7.0-alpha11 |
| Preferences | DataStore 1.1.0 |
| Build | AGP 9.1.0, KSP |

## License

This project is personal / private software. All rights reserved.
