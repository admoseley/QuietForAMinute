# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

This is a single-module Android project using Gradle Kotlin DSL. Requires JDK 17+ and Android SDK (install Android Studio).

```bash
./gradlew assembleDebug          # Build debug APK
./gradlew installDebug           # Build & install on connected device/emulator
./gradlew clean                  # Clean build artifacts
./gradlew dependencies           # Show dependency tree
```

No tests exist yet. When added, use `./gradlew test` (unit) and `./gradlew connectedAndroidTest` (instrumented).

## Architecture

**Package**: `com.admoseley.quietforaminute` | **Min SDK 26** | **Target SDK 34**

The app has two main user flows:

### Manual Mute Flow
`VolumeReceiver` (dynamic) → `OverlayService.handleMuteDetected()` → `OverlayViewController` shows `MuteDurationDialog` as system overlay → user picks duration → `MuteTimerService` runs countdown → restores volume + plays chime

### Scheduled Mute Flow
`AlarmManager` → `AlarmReceiver` → starts `MuteTimerService` with schedule ID + duration → same countdown/restore flow → re-schedules next occurrence via `ScheduleRepository.save()`

### Key Architectural Decisions
- **VolumeReceiver must be registered dynamically** inside `OverlayService` — `android.media.VOLUME_CHANGED_ACTION` cannot be received by statically declared receivers on API 26+
- **System overlay uses `ComposeView` added to `WindowManager`** — requires `ServiceLifecycleOwner` (custom class implementing `LifecycleOwner`, `ViewModelStoreOwner`, `SavedStateRegistryOwner`) set on the view before `setContent()`
- **`OverlayServiceBridge`** (in-process singleton object in `MuteTimerService.kt`) prevents `VolumeReceiver` from re-triggering when `MuteTimerService` programmatically restores volume
- **Schedule days stored as bitmask** in Room — `bit0=Monday` through `bit6=Sunday`, converted to/from `Set<DayOfWeek>` via extension functions in `ScheduleEntity.kt`
- **AlarmManager requestCode**: `(scheduleId * 7 + dayOfWeek.value).toInt()` — one `PendingIntent` per (schedule × day) combination

### Layer Organization
- `domain/model/` — `Schedule` data class (pure Kotlin, no Android deps)
- `data/db/` — Room database, DAO, entity with domain mapping extensions
- `data/datastore/` — `PreferencesRepository` wrapping DataStore (default volume, overlay enabled)
- `data/repository/` — `ScheduleRepository` facade over DAO + `AlarmScheduler`
- `scheduler/` — `AlarmScheduler` manages `AlarmManager` with exact alarms
- `service/` — Two foreground services: `OverlayService` (always-on monitor) and `MuteTimerService` (on-demand countdown)
- `overlay/` — `OverlayViewController` + `ServiceLifecycleOwner` for WindowManager overlay
- `receiver/` — `VolumeReceiver`, `AlarmReceiver`, `BootReceiver`
- `ui/` — Jetpack Compose screens with `@HiltViewModel` ViewModels

### DI (Hilt)
- `@HiltAndroidApp`: `QuietApplication`
- `@AndroidEntryPoint`: `MainActivity`, `OverlayService`, `MuteTimerService`, `BootReceiver`
- Modules: `DatabaseModule` (Room DB + DAO), `AppModule` (ChimePlayer)
- `PreferencesRepository`, `ScheduleRepository`, `AlarmScheduler` are `@Singleton` with `@Inject constructor`

### Navigation
Bottom nav with 2 tabs: `settings` and `schedules`. Plus `schedules/edit?id={id}` (pushed modal, `id=-1` for new).

## Tech Stack
- Kotlin 1.9.23, Compose BOM 2024.04.01, Material3
- Room 2.6.1, DataStore 1.1.0, Hilt 2.51.1
- kapt for Room compiler and Hilt compiler
- Gradle 8.6, AGP 8.3.2

## Permissions
`SYSTEM_ALERT_WINDOW` is checked lazily at runtime (`Settings.canDrawOverlays()`). The Settings screen shows permission status with grant buttons. `POST_NOTIFICATIONS` is requested on first launch (Android 13+). `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM` gates exact alarm scheduling.
