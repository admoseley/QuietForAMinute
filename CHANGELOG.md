# Changelog

All notable changes to Quiet For A Minute are documented here.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), with one addition:
every release entry carries a **Known Issues** section, so a release states what is still broken
and not only what got fixed.

Versioning is [CalVer](https://calver.org/) as of `2026.09.03`:

- `versionName` is `YYYY.MM.DD`, dated to when the work was completed. A second release on the same
  day appends a sequence — `2026.09.03.1`, then `.2`.
- `versionCode` is `YYYYMMDD * 10 + N` (N = that day's 0-based sequence), e.g. `202609030`. It is
  derived rather than a literal timestamp because `versionCode` is a signed 32-bit int that Google
  Play caps at 2,100,000,000 — a full `YYYYMMDDhhmm` overflows that by roughly 100x.
- Tags are `vYYYY.MM.DD`, keeping the `v` prefix used by `v1.0.0`.

`1.0.0` below was the only [Semantic Versioning](https://semver.org/) release. Semver was dropped
because major/minor/patch is a signal aimed at consumers of an API; this is a standalone app with
no library consumers, so nothing acted on it, and it made frequent small releases awkward to number.
That release's tag and GitHub Release are left exactly as they were.

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
