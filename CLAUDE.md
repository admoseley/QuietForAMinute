# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

This is a single-module Android project using Gradle Kotlin DSL. Requires JDK 21+ and Android SDK (install Android Studio).

```bash
./gradlew assembleDebug          # Build debug APK
./gradlew installDebug           # Build & install on connected device/emulator
./gradlew clean                  # Clean build artifacts
./gradlew dependencies           # Show dependency tree
```

Unit tests: `./gradlew test` (pure-logic tests under `app/src/test/`, no device needed). No
instrumented tests yet; when added, use `./gradlew connectedAndroidTest`.

Where a class needed a device dependency (`Context`, `android.content.Intent`) just to test pure
logic, that logic was pulled into a plain Kotlin object/function instead: `AlarmScheduler`'s date
math lives in `scheduler/AlarmTiming.kt`, and `VolumeReceiver`'s edge-detection decision is
`VolumeReceiver.Companion.shouldTrigger()`, both exercised directly with primitive values rather
than a mocked `Intent`/`Context`.

## Architecture

**Package**: `com.admoseley.quietforaminute` | **Min SDK 26** | **Compile SDK 37.2** | **Target SDK 36**

The app has two main user flows:

### Manual Mute Flow
`VolumeReceiver` (dynamic; `VOLUME_CHANGED_ACTION` + `STREAM_MUTE_CHANGED_ACTION`, fires only on the transition into zero on STREAM_MUSIC/STREAM_RING) → `OverlayService.handleStreamMuted(streamType)` → `OverlayViewController.show(streamType)` shows `MuteDurationDialog` as system overlay → user picks duration → `MuteTimerService` runs countdown → restores the *same stream* + plays chime

### Scheduled Mute Flow
`AlarmManager` → `AlarmReceiver` → **re-arms next occurrence immediately** via `ScheduleRepository.save()` → starts `MuteTimerService` with duration → mutes STREAM_MUSIC → countdown/restore flow

### Key Architectural Decisions
- **VolumeReceiver must be registered dynamically** inside `OverlayService` — the audio broadcasts cannot be received by statically declared receivers on API 26+
- **System overlay uses `ComposeView` added to `WindowManager`** — requires `ServiceLifecycleOwner` (custom class implementing `LifecycleOwner`, `ViewModelStoreOwner`, `SavedStateRegistryOwner`) set on the view before `setContent()`. `OverlayViewController.show()` flips its showing flag synchronously before suspending, to prevent a double-window race.
- **`OverlayServiceBridge`** (`service/OverlayServiceBridge.kt`) is a self-expiring 1.5 s time window, not a counter. `MuteTimerService` calls `markProgrammaticChange()` before every `setStreamVolume`/`adjustStreamVolume`; `OverlayService` ignores mute events inside the window. A counter was previously used and got stuck after restores, swallowing every other real mute.
- **`defaultVolume` preference is in STREAM_MUSIC index units**; scale it when restoring STREAM_RING (see `MuteTimerService.scaleFromMusicUnits`)
- **Exact alarms use `SCHEDULE_EXACT_ALARM` only** (user-granted). `USE_EXACT_ALARM` is Play-restricted to alarm/calendar apps. `BootReceiver` also re-arms on `TIMEZONE_CHANGED`, `TIME_SET` and `SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED`.
- **`ScheduleRepository.save()`/`setEnabled()` return whether alarms actually got armed** (`ScheduleSaveResult` / `Boolean`), not just the saved entity. `ScheduleEditScreen` → `AppNavigation` passes a "saved without alarms" flag back to `ScheduleListScreen` via the previous back stack entry's `SavedStateHandle` (separate ViewModels, so a SharedFlow can't cross screens directly); the list screen shows a Snackbar with a Grant action either way — from that nav result or from `ScheduleListViewModel.alarmsNotArmed` when toggling a schedule on in place.
- **Schedule days stored as bitmask** in Room — `bit0=Monday` through `bit6=Sunday`, converted to/from `Set<DayOfWeek>` via extension functions in `ScheduleEntity.kt`
- **AlarmManager requestCode**: `(scheduleId * 7 + dayOfWeek.value).toInt()` — one `PendingIntent` per (schedule × day) combination

### Layer Organization
- `domain/model/` — `Schedule` data class (pure Kotlin, no Android deps)
- `data/db/` — Room database, DAO, entity with domain mapping extensions
- `data/datastore/` — `PreferencesRepository` wrapping DataStore (default volume, overlay enabled)
- `data/repository/` — `ScheduleRepository` facade over DAO + `AlarmScheduler`
- `scheduler/` — `AlarmScheduler` manages `AlarmManager` with exact alarms
- `audio/` — `ChimePlayer` singleton wrapping `MediaPlayer` for mute/restore chimes
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
- Kotlin 2.2.10 (bundled with AGP 9.4.0 — keep the Compose plugin version in step with it), Compose BOM 2026.08.00, Material3
- Room 2.8.4, DataStore 1.2.1, Hilt 2.60.1, Navigation 2.10.0, Lifecycle 2.11.0
- KSP (not kapt) for Room compiler and Hilt compiler
- Gradle 9.6, AGP 9.4.0. All versions in `gradle/libs.versions.toml`.

## Build environment notes
- No JDK on PATH; use Android Studio's bundled one: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug`
- Build artifacts (`.gradle/`, `app/build/`, `.idea/`, `local.properties`) are git-ignored.
- compileSdk is 37 with `compileSdkMinor = 2` (the SDK only ships `platforms;android-37.2`, not a
  bare `android-37`) because the current Compose BOM's AAR metadata requires compiling against
  API 37+; targetSdk stays at 36, the current Play minimum. If Android Studio ever reports
  "Could not find compile target android-37.2" after this platform is genuinely installed via
  `sdkmanager`, it's a stale in-IDE cache from before the install — fully quit and reopen Studio
  rather than just re-syncing, which doesn't re-scan the SDK from disk.

## Permissions
`SYSTEM_ALERT_WINDOW` is checked lazily at runtime (`Settings.canDrawOverlays()`). The Settings screen shows permission status with grant buttons. `POST_NOTIFICATIONS` is requested on first launch (Android 13+). `SCHEDULE_EXACT_ALARM` (user-granted, denied by default on 14+) gates exact alarm scheduling. `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` has a Settings row (`PowerManager.isIgnoringBatteryOptimizations()` + `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`) — OEM battery managers killing `OverlayService` is a likely cause of the popup not appearing consistently.

## Process
Full workflow (file an issue → branch → work with comments and doc updates → logical commits →
PR → merge → delete branch → confirm issue closed) lives in the global `~/.claude/CLAUDE.md`
instructions, not duplicated here. Project-specific: branch names are `fix/...` / `chore/...` /
`feat/...`, reference the issue with `Fixes #N` in the commit body, never commit directly to
`master`, and keep README.md / INSTRUCTIONS.md / this file in sync with behavior changes as part
of the same work — not a follow-up.

## Versioning
Semantic versioning from v1.0.0 onward (see CHANGELOG.md). `versionName` in `app/build.gradle.kts` is `major.minor.patch`; `versionCode` increments by 1 on every release regardless of which part of `versionName` changed. Every release gets: a CHANGELOG.md entry, `versionName`/`versionCode` bumped in the same commit/PR, a git tag (`vX.Y.Z`) on the merge commit, and a GitHub Release — created after the PR merges, not before.

## CI & Security

`.github/workflows/ci.yml` runs lint + unit tests + a debug build on every PR and push to
`master`. `.github/workflows/secret-scan.yml` runs the open-source `gitleaks` CLI (not the
`gitleaks-action` wrapper, which needs a paid license for private repos) on every PR and push.
`.github/dependabot.yml` opens weekly PRs for outdated Gradle/Actions dependencies; Dependabot
security alerts are enabled repo-wide. None of these are enforced as *required* status checks —
branch protection needs GitHub Pro for a private repo, which this repo doesn't have. CodeQL and
native secret scanning are gated behind GitHub Advanced Security and are not set up (see issue
#21 for what was deliberately left out and why).
