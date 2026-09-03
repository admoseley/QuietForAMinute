# Quiet For A Minute

**Mute when you need it. Restore when you forget.**

Quiet For A Minute is an Android app that takes the friction out of temporary muting. Whether you're stepping into a meeting, a class, or just need a moment of quiet, the app mutes your device for a set duration — then automatically restores your volume when the time is up, so you never miss another call or notification because you forgot to unmute.

## Features

### Manual Mute (On-Demand)
When you mute your device — volume-down to zero, dragging the volume slider to zero, or tapping the mute icon in the volume panel — Quiet For A Minute pops up a dialog asking how long you want to stay muted. Pick your duration, and the app handles the rest — counting down in a foreground notification and restoring your volume automatically when the timer expires.

- Triggers on the **media** stream and the **ring** stream, and restores whichever one you muted
- Set mute duration with preset chips (15m/30m/1h/2h), +/- steppers, or by typing an exact value
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
The app requests these special permissions as needed, each explained in the Settings screen with a direct link to the relevant system settings page:

| Permission | Purpose |
|---|---|
| Display over other apps | Show the mute timer popup on top of whatever app is open |
| Schedule exact alarms | Fire scheduled mutes at a precise time (denied by default on Android 14+, grant from Settings) |
| Ignore battery optimization | Keeps the volume-monitor service from being killed by aggressive OEM battery managers — a likely cause of the popup not appearing consistently |

## How It Works

The app runs two foreground services:

- **OverlayService** — always-on monitor that listens for volume changes and shows the mute duration dialog when your volume hits zero
- **MuteTimerService** — on-demand countdown timer that mutes the device, manages a notification with the remaining time, and restores volume when done

### Mute detection
`OverlayService` registers a dynamic `VolumeReceiver` for two system broadcasts: `VOLUME_CHANGED_ACTION` (volume keys, panel slider) and `STREAM_MUTE_CHANGED_ACTION` (the mute icon in the volume panel, ringer → vibrate). Only a *transition* into zero on the media or ring stream counts, so repeated zero broadcasts cannot double-fire. When the app changes volume itself (scheduled mute, timer restore) it opens a short time window via `OverlayServiceBridge`; broadcasts inside that window are ignored. The window expires by itself, so it can never get "stuck" and swallow a real mute.

### Scheduling
Scheduled mutes are managed by `AlarmManager` with exact-alarm PendingIntents, one per (schedule × day-of-week) combination. Each alarm is one-shot and is re-armed for the following week by `AlarmReceiver` the moment it fires, so a killed process or an interrupted countdown cannot stop a schedule from repeating. `BootReceiver` re-arms everything after a reboot, app update, clock/time-zone change, or when the exact-alarm permission is granted.

## Building

Requires **Android Studio** with JDK 21+ and Android SDK (min SDK 26, compile SDK 37.2, target SDK 36 — see CLAUDE.md for why those two differ).

```bash
# Build a debug APK
./gradlew assembleDebug

# Build and install on a connected device or emulator
./gradlew installDebug

# Clean build artifacts
./gradlew clean

# Build a signed, minified release bundle (needs keystore.properties — see below)
./gradlew bundleRelease
```

### Release signing

`bundleRelease` / `assembleRelease` are minified (R8 + resource shrinking) and signed only when
a `keystore.properties` file exists at the repo root, pointing at a local `.jks` keystore:

```properties
storeFile=release-keystore.jks
storePassword=...
keyAlias=...
keyPassword=...
```

Both passwords **must be identical** — `keytool` defaults to a PKCS12 keystore, which requires
`storePassword == keyPassword`; a mismatched pair fails signing with a cryptic error rather than
a clear one. Neither the keystore nor `keystore.properties` are committed (see `.gitignore`);
without them, debug builds still work normally and `bundleRelease` just produces an unsigned
bundle. This repo does not currently publish anywhere — no CI job builds or uploads a release yet.

## Tech Stack

| Layer | Library |
|---|---|
| Language | Kotlin 2.2.10 (bundled with AGP) |
| UI | Jetpack Compose + Material 3 (BOM 2026.08.00) |
| Navigation | Navigation Compose 2.10.0, Lifecycle 2.11.0 |
| Dependency Injection | Hilt 2.60.1 |
| Local Database | Room 2.8.4 |
| Preferences | DataStore 1.2.1 |
| Build | AGP 9.4.0, Gradle 9.6, KSP 2.3.6 |

Versions live in `gradle/libs.versions.toml`.

## Development Workflow

Work happens on a branch per fix/feature/chore, tracked by a GitHub issue, merged via pull
request — never committed directly to `master`. See `CLAUDE.md` for the full cycle.

Every PR and push to `master` runs through GitHub Actions:

- **CI** (`.github/workflows/ci.yml`) — lint, unit tests, and a debug build. Doesn't sign or
  publish anything yet; that's blocked on the release signing config.
- **Secret scan** (`.github/workflows/secret-scan.yml`) — the open-source `gitleaks` CLI checks
  every diff for accidentally-committed keys, tokens, or credentials.
- **Dependabot** (`.github/dependabot.yml`) — weekly PRs for outdated Gradle and Actions
  dependencies; security alerts for known-vulnerable dependencies are enabled repo-wide.

Branch protection on `master` and GitHub's native code/secret scanning both require GitHub
Advanced Security, which isn't available on this private repo's current plan — the checks above
are the free-tier equivalent. Upgrading (or making the repo public) would unlock enforcing these
as required status checks instead of just running them.

## Known Limitations

- The mute countdown itself lives in `MuteTimerService`'s process memory, but a backup `AlarmManager` alarm and a DataStore-persisted restore target now cover the cases that used to leave volume stuck muted: the OS killing the process (Doze, an OEM battery manager, low memory) or the device rebooting mid-timer. **Force-stopping the app is the one case still not covered** — Android cancels an app's `AlarmManager` alarms as part of force-stop itself, so the backup alarm goes with it; raise your volume manually if you force-stop the app mid-timer.
- The overlay uses `TYPE_APPLICATION_OVERLAY`, which draws above other apps but **not** above the lock screen.
- Only one countdown runs at a time; a new mute replaces the running one.

## Documentation

For detailed step-by-step usage instructions, a full feature list, and answers to common questions, see the **[User Guide](INSTRUCTIONS.md)**.

## Versioning

Current version: **1.0.0**. Follows [Semantic Versioning](https://semver.org/) — see [CHANGELOG.md](CHANGELOG.md) for what changed in each release. Every release gets a CHANGELOG entry, a git tag, and a GitHub Release.

## License

This project is personal / private software. All rights reserved.
