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
`VolumeReceiver.Companion.transitionFor()`, both exercised directly with primitive values rather
than a mocked `Intent`/`Context`.

## Architecture

**Package**: `com.admoseley.quietforaminute` | **Min SDK 26** | **Compile SDK 37.2** | **Target SDK 36**

The app has two main user flows:

### Manual Mute Flow
`VolumeReceiver` (dynamic; `VOLUME_CHANGED_ACTION` + `STREAM_MUTE_CHANGED_ACTION`, reports a `VolumeTransition` on either edge across zero on STREAM_MUSIC/STREAM_RING) → `OverlayService.handleVolumeTransition()` → `handleStreamMuted(streamType)` → `OverlayViewController.show(streamType)` shows `MuteDurationDialog` as system overlay → user picks duration (and optionally Do Not Disturb) → `MuteTimerService` runs countdown → restores the *same stream* + clears DND + plays chime

### Scheduled Mute Flow
`AlarmManager` → `AlarmReceiver` → **re-arms next occurrence immediately** via `ScheduleRepository.save()` → starts `MuteTimerService` with duration → mutes STREAM_MUSIC → countdown/restore flow

### Key Architectural Decisions
- **Do Not Disturb is additive, never a replacement** (issue #45). `DndController` wraps
  `NotificationManager.setInterruptionFilter(INTERRUPTION_FILTER_PRIORITY)`, gated on the
  `ACCESS_NOTIFICATION_POLICY` special access (`canControlDnd()`; every method no-ops without it).
  `enable()` returns whether *this call* changed DND — false both when it couldn't and when DND was
  already on — and that boolean, not a live "is DND on?" check, is what later authorises clearing it.
  Clearing DND the user set themselves would undo a setting the app never owned. The flag rides
  along in `PendingRestore` and in the backup alarm's extras so **all three** restore paths
  (`MuteTimerService`, `BackupRestoreReceiver`, `BootReceiver`) clear it — otherwise a killed process
  strands the device in DND with no timer left to end it. `VolumeRestorer.restore()` clears DND
  *before* touching volume: an active DND policy can make raising the ringer out of silent throw.
  When DND is on and the trigger stream was STREAM_RING, media is muted too (`mediaMuted`, same
  we-only-undo-what-we-did rule) since DND alone doesn't silence playback.
- **VolumeReceiver must be registered dynamically** inside `OverlayService` — the audio broadcasts cannot be received by statically declared receivers on API 26+
- **A manual volume restore cancels a running timer** (issue #42): `VolumeReceiver` reports `VolumeTransition.UNMUTED` as well as `MUTED`; `OverlayService.handleStreamUnmuted()` checks `PreferencesRepository.pendingRestore` (non-null only while a countdown runs) and, if a timer is live, sends `MuteTimerService.ACTION_CANCEL_TIMER`. The service tears down the countdown, backup alarm and persisted record, shows a Toast and plays the restore chime, but deliberately does **not** set the volume — the user already did. The `OverlayServiceBridge` programmatic window is what keeps the timer's *own* end-of-countdown restore from being misread as a manual one and cancelling the timer that just completed.
- **System overlay uses `ComposeView` added to `WindowManager`** — requires `ServiceLifecycleOwner` (custom class implementing `LifecycleOwner`, `ViewModelStoreOwner`, `SavedStateRegistryOwner`) set on the view before `setContent()`. `OverlayViewController.show()` flips its showing flag synchronously before suspending, to prevent a double-window race.
- **`OverlayServiceBridge`** (`service/OverlayServiceBridge.kt`) is a self-expiring 1.5 s time window, not a counter. `MuteTimerService` calls `markProgrammaticChange()` before every `setStreamVolume`/`adjustStreamVolume`; `OverlayService` ignores mute events inside the window. A counter was previously used and got stuck after restores, swallowing every other real mute.
- **`defaultVolume` preference is in STREAM_MUSIC index units**; scale it when restoring STREAM_RING via the pure `scaleVolumeUnits()` top-level function in `service/VolumeRestorer.kt` (unit-tested in `VolumeRestorerTest`)
- **Exact alarms use `SCHEDULE_EXACT_ALARM` only** (user-granted). `USE_EXACT_ALARM` is Play-restricted to alarm/calendar apps. `BootReceiver` also re-arms on `TIMEZONE_CHANGED`, `TIME_SET` and `SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED`.
- **`ScheduleRepository.save()`/`setEnabled()` return whether alarms actually got armed** (`ScheduleSaveResult` / `Boolean`), not just the saved entity. `ScheduleEditScreen` → `AppNavigation` passes a "saved without alarms" flag back to `ScheduleListScreen` via the previous back stack entry's `SavedStateHandle` (separate ViewModels, so a SharedFlow can't cross screens directly); the list screen shows a Snackbar with a Grant action either way — from that nav result or from `ScheduleListViewModel.alarmsNotArmed` when toggling a schedule on in place.
- **Schedule days stored as bitmask** in Room — `bit0=Monday` through `bit6=Sunday`, converted to/from `Set<DayOfWeek>` via extension functions in `ScheduleEntity.kt`
- **AlarmManager requestCode**: `(scheduleId * 7 + dayOfWeek.value).toInt()` — one `PendingIntent` per (schedule × day) combination
- **Mute-timer reliability (issue #8)**: `MuteTimerService` persists `{endEpochMillis, streamType, manualRestoreVolume}` to `PreferencesRepository.pendingRestore` (survives process death and reboot — DataStore, not memory) and arms a `BackupRestoreScheduler` exact alarm ~30s past the expected end. Under normal operation the service's own `restoreVolume()` finishes first and cancels both. If the process is killed first, `BackupRestoreReceiver` performs the restore instead. If the *device* reboots mid-timer (which also clears the AlarmManager alarm), `BootReceiver` reads the persisted record on `ACTION_BOOT_COMPLETED` and either restores immediately (end time already passed) or re-arms the backup alarm for what's left. The actual volume-setting logic is shared via `VolumeRestorer`, used by both the primary path (with Toast + chime layered on top) and the two fallback paths (deliberately without — `MediaPlayer` prepare/threading is machinery this safety net shouldn't depend on). Force-stopping the app is **not** covered — Android cancels an app's own `AlarmManager` alarms as part of force-stop.

### Layer Organization
- `domain/model/` — `Schedule` data class (pure Kotlin, no Android deps)
- `data/db/` — Room database (v2; `AppDatabase.MIGRATION_1_2` adds `dndEnabled`), DAO, entity with
  domain mapping extensions. The builder keeps `fallbackToDestructiveMigration`, so **any** new
  version bump needs a registered migration or every saved schedule is silently deleted on upgrade.
- `data/datastore/` — `PreferencesRepository` wrapping DataStore (default volume, overlay enabled, pending mute-restore state)
- `data/repository/` — `ScheduleRepository` facade over DAO + `AlarmScheduler`
- `scheduler/` — `AlarmScheduler` manages scheduled-mute `AlarmManager` alarms; `BackupRestoreScheduler` manages the one-off backup restore alarm
- `audio/` — `ChimePlayer` singleton wrapping `MediaPlayer` for mute/restore chimes
- `service/` — Two foreground services: `OverlayService` (always-on monitor) and `MuteTimerService` (on-demand countdown); `VolumeRestorer` holds the shared restore logic they and the receivers below all use; `DndController` owns the Do Not Disturb interruption filter
- `overlay/` — `OverlayViewController` + `ServiceLifecycleOwner` for WindowManager overlay
- `receiver/` — `VolumeReceiver`, `AlarmReceiver`, `BootReceiver`, `BackupRestoreReceiver`
- `ui/` — Jetpack Compose screens with `@HiltViewModel` ViewModels, plus shared `ui/components/` (e.g. `DurationPicker`)

### DI (Hilt)
- `@HiltAndroidApp`: `QuietApplication`
- `@AndroidEntryPoint`: `MainActivity`, `OverlayService`, `MuteTimerService`, `BootReceiver`, `BackupRestoreReceiver`
- Modules: `DatabaseModule` (Room DB + DAO), `AppModule` (ChimePlayer)
- `PreferencesRepository`, `ScheduleRepository`, `AlarmScheduler`, `BackupRestoreScheduler`, `VolumeRestorer`, `DndController` are `@Singleton` with `@Inject constructor`

### Navigation
Bottom nav with 2 tabs: `settings` and `schedules`. Plus `schedules/edit?id={id}` (pushed modal, `id=-1` for new).

## Tech Stack
- Kotlin 2.2.10 (bundled with AGP 9.4.0 — keep the Compose plugin version in step with it), Compose BOM 2026.08.00, Material3
- Room 2.8.4, DataStore 1.2.1, Hilt 2.60.1, Navigation 2.10.0, Lifecycle 2.11.0
- No icon library dependency — the ~16 icons the app actually uses are local vector drawables in
  `res/drawable/ic_*.xml`, referenced via `painterResource()`. Replaced the deprecated, frozen
  `material-icons-extended` artifact (issue #16); path data is copied verbatim from Google's
  Apache-2.0-licensed `material-design-icons` repo, not hand-authored.
- KSP (not kapt) for Room compiler and Hilt compiler
- Gradle 9.6, AGP 9.4.0. All versions in `gradle/libs.versions.toml`.

## Build environment notes
- No JDK on PATH; use Android Studio's bundled one: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug`
- Build artifacts (`.gradle/`, `app/build/`, `.idea/`, `local.properties`) are git-ignored.
- `org.gradle.jvmargs`/`kotlin.daemon.jvmargs` in `gradle.properties` are 3072m/1024m metaspace —
  release builds with R8 minification are memory-hungry; a long session running many builds can
  still hit a Metaspace OOM eventually (`./gradlew --stop` clears the wedged daemon if so).
- The `release` build type is minified/shrunk (R8 + resources) and signed only when
  `keystore.properties` exists at the repo root (gitignored, not committed — see README's
  "Release signing" section). `storePassword` and `keyPassword` in it must be identical: `keytool`
  defaults to PKCS12, which requires them to match, and a mismatch fails `bundleRelease` with
  "Given final block not properly padded" rather than a clear password error.
- Nothing publishes anywhere yet — no CI job builds/uploads a release build.
- compileSdk is 37 with `compileSdkMinor = 2` (the SDK only ships `platforms;android-37.2`, not a
  bare `android-37`) because the current Compose BOM's AAR metadata requires compiling against
  API 37+; targetSdk stays at 36, the current Play minimum. If Android Studio ever reports
  "Could not find compile target android-37.2" after this platform is genuinely installed via
  `sdkmanager`, it's a stale in-IDE cache from before the install — fully quit and reopen Studio
  rather than just re-syncing, which doesn't re-scan the SDK from disk.

## Permissions
`ACCESS_NOTIFICATION_POLICY` is a special access checked at runtime
(`NotificationManager.isNotificationPolicyAccessGranted`), granted via
`ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS` — a global list with no per-package `Uri`, unlike the
other permission rows. It gates the DND option only; the app is fully functional without it.
`SYSTEM_ALERT_WINDOW` is checked lazily at runtime (`Settings.canDrawOverlays()`). The Settings screen shows permission status with grant buttons. `POST_NOTIFICATIONS` is requested on first launch (Android 13+). `SCHEDULE_EXACT_ALARM` (user-granted, denied by default on 14+) gates exact alarm scheduling. `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` has a Settings row (`PowerManager.isIgnoringBatteryOptimizations()` + `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`) — OEM battery managers killing `OverlayService` is a likely cause of the popup not appearing consistently.

## Process
Full workflow (file an issue → branch → work with comments and doc updates → logical commits →
PR → merge → delete branch → confirm issue closed) lives in the global `~/.claude/CLAUDE.md`
instructions, not duplicated here. Project-specific: branch names are `fix/...` / `chore/...` /
`feat/...`, reference the issue with `Fixes #N` in the commit body, never commit directly to
`master`, and keep README.md / INSTRUCTIONS.md / this file in sync with behavior changes as part
of the same work — not a follow-up.

## Versioning
Calendar versioning (CalVer) from `2026.09.03` onward. `v1.0.0` was the one semver release; its
tag and GitHub Release stay as they are, and CHANGELOG.md records the switchover.

- **`versionName` = `YYYY.MM.DD`**, dated to when the work was completed — not to some planned
  release train. A second release on the same day appends a sequence: `2026.09.03.1`, then `.2`.
- **`versionCode` = `YYYYMMDD * 10 + N`** (N = that day's 0-based sequence), e.g. `202609030`.
  It is *derived*, not a literal timestamp: `versionCode` is a signed 32-bit int capped by Play at
  **2,100,000,000**, so a full `YYYYMMDDhhmm` overflows it by ~100x and even `YYMMDDhhmm`
  (`2609031415`) exceeds it. This form sits near 2.0e8, allows 10 releases/day, and keeps working
  past the year 9000.
- Semver was dropped because its major/minor/patch signal is aimed at consumers of an API. This is
  a standalone app with no library consumers, so nothing acted on it, and it made frequent small
  releases awkward to number.

Every release gets: a CHANGELOG.md entry (including a **Known Issues** section, so each release
ships with its outstanding problems stated and not just its fixes), `versionName`/`versionCode`
bumped in the same commit/PR, a git tag (`vYYYY.MM.DD`, keeping the `v` prefix for continuity with
`v1.0.0`) on the merge commit, and a GitHub Release — created after the PR merges, not before.

## CI & Security

`.github/workflows/ci.yml` runs lint + unit tests + a debug build on every PR and push to
`master`. `.github/workflows/secret-scan.yml` runs the open-source `gitleaks` CLI (not the
`gitleaks-action` wrapper, which needs a paid license for private repos) on every PR and push.
`.github/dependabot.yml` opens weekly PRs for outdated Gradle/Actions dependencies; Dependabot
security alerts are enabled repo-wide. None of these are enforced as *required* status checks —
branch protection needs GitHub Pro for a private repo, which this repo doesn't have. CodeQL and
native secret scanning are gated behind GitHub Advanced Security and are not set up (see issue
#21 for what was deliberately left out and why).
