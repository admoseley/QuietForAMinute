# Changelog

All notable changes to Quiet For A Minute are documented here.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and versioning
follows [Semantic Versioning](https://semver.org/): `versionName` is `major.minor.patch`;
`versionCode` increments by 1 on every release regardless of which part changed, since that's all
Google Play requires.

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
