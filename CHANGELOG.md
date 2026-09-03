# Changelog

All notable changes to Quiet For A Minute are documented here.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and versioning
follows [Semantic Versioning](https://semver.org/): `versionName` is `major.minor.patch`;
`versionCode` increments by 1 on every release regardless of which part changed, since that's all
Google Play requires.

## [Unreleased]

Merged to `master` but not yet tagged or released. `versionName`/`versionCode` are bumped as part
of cutting a release, not here.

### Added
- Do Not Disturb option (#45): an "Also turn on Do Not Disturb" switch in the mute popup and on
  each schedule. DND is applied *on top of* the volume mute for the same duration and lifted when
  the timer ends — including on the process-killed and reboot fallback paths, so a lost countdown
  can never strand the device in DND. DND the user enabled themselves is never cleared by the app.
  Requires the new `ACCESS_NOTIFICATION_POLICY` special access, surfaced as a Settings permission
  row; the app is fully functional without it.
- Duration fields are now editable (#41): type an exact number of hours or minutes instead of only
  using the presets or stepping by 5. Minutes range widened from 0–55 to 0–59.

### Changed
- Raising the volume by hand during a running timer now cancels it (#42), with a toast reading
  "Manual restore of volume, timer has been cancelled" and the restore chime. Your volume is left
  exactly where you set it rather than being snapped to the configured restore level.
- Room database is now version 2, with a real `MIGRATION_1_2` adding the schedules' `dndEnabled`
  column. Existing schedules are preserved and default to DND off.

## [1.0.0] — 2026-09-03

First versioned release.

### Added
- Manual mute detection: a system-overlay popup appears when the media or ring stream transitions
  to zero (volume keys, the on-screen slider, or the volume panel's mute icon), letting you pick a
  duration and a restore volume before the countdown starts.
- Scheduled mutes: recurring per-day-of-week schedules with a start time and duration, managed via
  exact `AlarmManager` alarms, re-armed automatically for their next occurrence and after reboots,
  clock/time-zone changes, or an exact-alarm permission grant.
- Settings: default restore volume, light/dark/system theme, independent mute/restore chime
  toggles with custom ringtone selection, and a permissions status panel with one-tap grant links.
- CI/security baseline: build+lint+test gate, Dependabot vulnerability alerts and version-update
  PRs, and secret scanning on every PR and push to `master`.

### Fixed
- The mute popup could silently stop appearing on every other mute — a suppression counter used to
  ignore the app's own volume changes never got reset after a restore. Replaced with a
  self-expiring time window.
- Re-enabling a schedule from the list didn't actually arm its alarm.
- A schedule could permanently stop repeating if its countdown was interrupted (process kill,
  reboot, or a manual mute replacing it) — schedules now re-arm the instant their alarm fires.
- Picking "Silent" for a chime played the default sound instead of nothing.
- `USE_EXACT_ALARM`, a Play-Store-restricted permission this app didn't qualify for, has been
  removed in favor of the user-granted `SCHEDULE_EXACT_ALARM`.
