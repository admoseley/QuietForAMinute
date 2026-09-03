# Quiet For A Minute — User Guide

## Table of Contents

1. [Overview](#1-overview)
2. [First Launch & Setup](#2-first-launch--setup)
   - 2.1 [Granting Required Permissions](#21-granting-required-permissions)
   - 2.2 [Configuring Your Restore Volume](#22-configuring-your-restore-volume)
3. [Manual Mute (On-Demand)](#3-manual-mute-on-demand)
   - 3.1 [How It Gets Triggered](#31-how-it-gets-triggered)
   - 3.2 [Using the Mute Duration Popup](#32-using-the-mute-duration-popup)
   - 3.3 [The Timer Notification](#33-the-timer-notification)
   - 3.4 [Volume Restore & Chime](#34-volume-restore--chime)
   - 3.5 [Edge Cases & Behavior Notes](#35-edge-cases--behavior-notes)
4. [Scheduled Mutes](#4-scheduled-mutes)
   - 4.1 [Viewing Your Schedules](#41-viewing-your-schedules)
   - 4.2 [Creating a New Schedule](#42-creating-a-new-schedule)
   - 4.3 [Editing a Schedule](#43-editing-a-schedule)
   - 4.4 [Enabling and Disabling Schedules](#44-enabling-and-disabling-schedules)
   - 4.5 [Deleting a Schedule](#45-deleting-a-schedule)
   - 4.6 [How Scheduled Mutes Fire](#46-how-scheduled-mutes-fire)
5. [Settings](#5-settings)
   - 5.1 [Restore Volume](#51-restore-volume)
   - 5.2 [App Theme](#52-app-theme)
   - 5.3 [Show Timer Popup on Mute](#53-show-timer-popup-on-mute)
   - 5.4 [Chime on Mute](#54-chime-on-mute)
   - 5.5 [Chime on Restore](#55-chime-on-restore)
   - 5.6 [Permissions Status Panel](#56-permissions-status-panel)
6. [Complete Feature List](#6-complete-feature-list)
7. [Frequently Asked Questions](#7-frequently-asked-questions)

---

## 1. Overview

Quiet For A Minute solves a simple but frustrating problem: you silence your phone for a meeting, a class, or a quiet moment — and then forget to turn the volume back on. Hours later you discover you missed calls, alarms, or messages.

The app works in two complementary ways:

- **Manual mute** — when you press the volume-down button all the way to zero, a popup instantly appears asking how long you want to stay muted. Set a duration, tap Start, and the app automatically restores your volume when the time is up.
- **Scheduled mutes** — for recurring events (a daily standup, a weekly class, Friday afternoon focus time), you create a named schedule with a start time, repeat days, and duration. The app fires silently at that time, mutes your device, and restores everything when it's done — with no action needed from you.

---

## 2. First Launch & Setup

### 2.1 Granting Required Permissions

Quiet For A Minute needs a few special permissions to function fully. Each is explained inside the app on the **Settings** screen, under the **Permissions** section.

#### Display Over Other Apps (Overlay Permission)

This permission allows the app to show the mute-duration popup over the lock screen and the system volume panel — the moment you mute, before you even unlock your phone.

**Steps to grant:**

1. Open the app and tap the **Settings** tab (bottom navigation).
2. Scroll down to the **Permissions** section.
3. Find the row labeled **"Display over other apps"**. If it shows a warning icon and a **Grant** button, the permission has not been granted yet.
4. Tap **Grant**. Your device's system settings will open directly to the correct page for this app.
5. Toggle **"Allow display over other apps"** to ON.
6. Press the back button or navigate back to the app. The permission row will now show a green check and the text "Granted".

> **Note:** Without this permission, the mute duration popup will not appear. Instead, the app will post a notification reminding you to grant the permission the next time you mute.

#### Schedule Exact Alarms (Android 12+ only)

This permission is required for scheduled mutes to fire at a precise time. Without it, Android may delay your scheduled mutes by minutes or longer.

**Steps to grant (Android 12 and above only):**

1. On the **Settings** screen, scroll to **Permissions**.
2. Find the row labeled **"Exact alarms"**. If it shows a **Grant** button, tap it.
3. The system settings page for alarm permissions will open.
4. Find Quiet For A Minute in the list and enable it.
5. Return to the app. The row will show "Granted".

> **Note:** On Android 11 and below, exact alarm permission is automatically available and this row will not appear.

#### Ignore Battery Optimization

Some manufacturers' battery managers aggressively kill background apps to save power, which can stop the always-on volume monitor from running — a likely cause of the mute popup showing up only sometimes. Granting this exemption keeps the monitor alive.

**Steps to grant:**

1. On the **Settings** screen, scroll to **Permissions**.
2. Find the row labeled **"Ignore battery optimization"**. If it shows a **Grant** button, tap it.
3. A system dialog appears asking to allow the app to ignore battery optimizations. Confirm it.
4. Return to the app. The row will show "Granted".

#### Notification Permission (Android 13+)

On first launch on Android 13 or newer, the app will ask for permission to post notifications. Tap **Allow**. This permission is required for:

- The persistent "Volume monitoring active" notification (confirms the app is running in the background).
- The countdown timer notification that shows remaining mute time.
- Alerts if an overlay permission is missing when you try to mute.

---

### 2.2 Configuring Your Restore Volume

Before using the app, it's worth setting the volume level you want your device to return to after a mute timer expires.

1. Open the **Settings** tab.
2. Find the **Restore Volume** section near the top.
3. Drag the slider left or right. The percentage displayed above the slider updates in real time.
4. The setting is saved automatically — there is no Save button.

This value is used as the default restore volume for both manual and scheduled mutes. You can also override it per-mute directly in the duration popup.

---

## 3. Manual Mute (On-Demand)

Manual mute is the core on-demand feature of Quiet For A Minute. It lets you silence your device instantly using the physical volume buttons — exactly as you normally would — but adds an automatic restore timer so your volume comes back on its own when the quiet period ends. There is nothing new to learn; you mute your phone the same way you always have.

---

### 3.1 How It Gets Triggered

#### What the App Listens For

Quiet For A Minute runs a persistent background service called `OverlayService`. This service registers a listener for two of Android's internal audio broadcasts and watches the **media stream** and the **ring stream** at all times. The moment either stream *transitions* to **zero** (or is flagged as muted), the service acts.

This means the trigger fires regardless of how you muted:

- Pressing the **physical volume-down button** on the side of the device repeatedly until the slider shows zero.
- Dragging the **on-screen volume slider** (the panel that appears when you press a volume key) all the way to the left.
- Tapping the **mute / bell icon** in the volume panel, or switching the ringer to vibrate.
- Any third-party app or shortcut that sets media or ring volume to zero programmatically.

> **Important:** The app watches the **media stream** (music, videos, podcasts, games) and the **ring stream**. Whichever one you muted is the one the timer restores. It does not trigger on notification-only or alarm volume changes, and it does not trigger on Do Not Disturb by itself unless DND also mutes the ring stream on your device.

> **Note:** Only the *transition* into zero counts. If your volume is already at zero and you press volume-down again, nothing happens — raise it and mute again to get the popup back.

#### The Sequence of Events on Mute

When zero media volume is detected, the following happens in order:

1. The service checks whether **"Show timer popup on mute"** is enabled in Settings. If it is off, the flow stops here — your phone is simply muted and nothing else happens.
2. The service checks whether the **overlay permission** ("Display over other apps") is granted. If it is not granted, the app posts a notification alerting you that the permission is needed, and the flow stops.
3. The service checks whether the **mute chime** is enabled. If it is, the selected chime sound plays immediately — before the popup appears — giving you instant audio confirmation that the mute was detected.
4. The **Mute Duration Dialog** slides onto the screen as a floating card above all other content, including the lock screen.

> **Note:** If the duration popup is already visible on screen (e.g., you muted, saw the dialog, then unmuted and re-muted without dismissing it), the app will not open a second instance. The existing dialog stays open.

#### When the Popup Does Not Appear

There are a few situations where the popup intentionally does not appear even when volume hits zero:

- **"Show timer popup on mute" is turned off** in Settings. This lets you keep scheduled mutes active while opting out of the manual popup entirely.
- **The overlay permission is not granted.** A notification will appear in the shade instead, prompting you to grant it.
- **The app itself is changing your volume.** When a scheduled mute fires or a timer expires, the service changes the volume programmatically. It opens a short (1.5 second) window first, and any volume event inside that window is ignored so the app cannot trigger itself. The window expires on its own — it cannot get stuck and swallow your next real mute.

---

### 3.2 Using the Mute Duration Popup

The **Mute Duration Dialog** is a rounded floating card that appears centered on your screen, overlaid on top of whatever app or screen was visible. It stays on top even if you switch apps or let the screen turn off and back on.

The card has four distinct areas, from top to bottom:

#### The Header

At the very top of the card is a muted-volume icon and the prompt: **"Mute for how long?"** This is a visual confirmation that the app has detected your mute and is ready to set up the timer.

#### Choosing a Duration

The center of the card shows a **duration picker**: four preset chips (**15m**, **30m**, **1h**, **2h**) above a pair of compact steppers for **hours** and **minutes**. An earlier version of this screen reused Android's clock-face time picker for this, which read as "what time is it" rather than "how long" — the preset-and-stepper layout replaced it for that reason.

- **Default value:** The picker opens at **0 hours, 30 minutes** every time (the same as tapping the **30m** preset). This covers most short meetings or focused sessions without requiring any adjustment.
- **Using a preset:** Tap **15m**, **30m**, **1h**, or **2h** to set both fields at once. Whichever preset matches the current hours/minutes is highlighted.
- **Using the steppers:** Tap **+** or **−** under **Hours** to adjust by 1, or under **Minutes** to adjust by 5. Minutes step by 5 rather than 1 — single-minute precision isn't meaningful for a mute timer, and 5-minute steps get you to any common duration in a few taps.
- **Valid range:** Hours can be 0–23. Minutes can be 0–55, in steps of 5. Any combination is valid as long as the total duration is greater than zero (i.e., you cannot start a zero-length timer).
- **Examples of common durations:**
  - 30-minute meeting → tap the **30m** preset
  - 1-hour class → tap the **1h** preset
  - 90-minute film → tap **1h**, then **+** once under Minutes
  - Overnight silence → **+** the Hours stepper to 8

#### Adjusting the Restore Volume (Per-Mute Override)

Below the duration picker, separated by a divider line, is a **Restore Volume** row. This lets you fine-tune the volume level the app will restore to when *this specific* timer ends, without changing the global default saved in Settings.

- The slider starts at the value set in Settings (your configured default restore volume).
- Drag the slider right to increase the restore level, or left to decrease it.
- The icon to the left of the label updates dynamically: a crossed-out speaker at zero, a low speaker at low volumes, and a full speaker at high volumes.
- The percentage (e.g., **65%**) is displayed to the right and updates as you drag.
- This override is local to this mute only. The next time the popup appears, it will again start from the global default.

**When would you use this?**
You might want to mute fully for a meeting, then restore to a quieter level than usual (e.g., 20%) because you'll still be in a shared space afterward. Or you might want to restore to maximum (100%) because you're about to take a call you don't want to miss. The per-mute override makes this adjustment without touching your global preferences.

#### Starting or Skipping

At the bottom of the card are two buttons:

- **Skip** (text button, left side) — Dismisses the popup without starting any timer. Your phone remains muted at zero volume. No automatic restore will happen. Use this if you want to silence your phone indefinitely and restore volume manually later.
- **Start** (filled button, right side) — Confirms the selected duration and restore volume, dismisses the popup, and begins the countdown. This button is **disabled** (grayed out and unresponsive) if both the hours and minutes fields are set to zero. You must select at least one minute of mute time before Start becomes active.

> **Tip:** You do not need to interact with the Start button immediately. The popup will stay on screen while you finish whatever you were doing. Take your time selecting the duration — the popup will not auto-dismiss.

---

### 3.3 The Timer Notification

The moment you tap **Start**, two things happen simultaneously:

1. The Mute Duration Dialog dismisses from the screen.
2. A **persistent foreground notification** appears in your notification shade.

#### What the Notification Shows

- **Icon:** The app's volume monitor icon in the status bar and notification row.
- **Title:** "Phone muted"
- **Body:** A countdown message in the format "Restoring volume in Xh Ym" — for example, "Restoring volume in 1h 29m" or "Restoring volume in 4m".

The body text updates approximately every 10 seconds as the countdown progresses, so you can pull down the notification shade at any time and see exactly how much quiet time remains.

#### Why the Notification Cannot Be Dismissed

The mute timer notification has the **ongoing** flag set, which means Android's swipe-to-dismiss gesture does not work on it. This is intentional and important: Android uses ongoing notifications as the anchor for foreground services. If the notification were dismissible, Android could kill the background service and your volume would never be restored automatically.

You can, however, minimize it by collapsing the notification shade — it will remain in the status bar as a small icon.

#### Tapping the Notification

Tapping anywhere on the notification row opens the Quiet For A Minute app. There is no way to cancel or extend the timer from the notification itself — to do that, open the app while a timer is running. (Note: in-app timer controls are not currently implemented; the only way to end a mute early is to manually raise your volume back up.)

#### The Notification Disappears Automatically

When the countdown reaches zero, the timer notification removes itself from the shade without any action from you. You do not need to swipe it away.

---

### 3.4 Volume Restore & Chime

When the countdown finishes, the restore sequence runs in this exact order:

#### Step 1 — Volume Is Raised

The app reads the restore volume set in the mute dialog (or the global default if no override was entered) and sets the stream you muted — media or ring — to that level. If you muted with the volume-panel icon, the mute flag is cleared first so sound actually comes back. The change is instantaneous.

The app also opens its self-change window for this step so that the volume change does not re-trigger the mute popup.

#### Step 2 — Toast Notification

A small toast message appears briefly at the bottom of the screen: **"System volume has been restored"**. This gives you a visual cue even if you have no audio feedback configured.

The toast appears on top of whatever is on screen at the time — you do not need to have the app open to see it.

#### Step 3 — Restore Chime (If Enabled)

If **Chime on restore** is turned on in Settings, a brief 200-millisecond delay is introduced before the chime plays. This gap ensures the volume has fully taken effect before the sound fires — so you actually hear the chime at the restored volume level rather than at zero.

The chime plays once and then stops. The service waits an additional second after the chime starts before fully shutting down, to ensure the sound completes without being cut off.

#### Step 4 — Notification Is Removed

The persistent "Phone muted" foreground notification is removed from the notification shade. The status bar icon disappears. The mute timer service stops running.

At this point, your device is fully back to normal — volume restored, notification gone, services idle — until you mute again.

---

### 3.5 Edge Cases & Behavior Notes

#### What if I manually raise my volume before the timer ends?

The timer continues running in the background. When it expires, the app will still attempt to set your volume to the configured restore level, potentially overriding whatever level you had manually set. If you decide to unmute yourself early, be aware that the timer is still active until it naturally expires.

#### What if I mute again while a timer is already running?

Muting again will trigger the popup a second time (if the overlay is enabled). Tapping **Start** on the second popup starts a new `MuteTimerService`, which cancels the previous countdown and replaces it with the new duration. Only one timer can run at a time.

Tapping **Skip** on the second popup leaves the original timer running undisturbed.

#### What if the app's process is killed, or the device restarts mid-timer?

The countdown itself runs inside a background service, but a backup safety net covers most of the ways that service can be cut short:

- **The OS kills the app's process** (an aggressive OEM battery manager, low memory, Doze) — a backup alarm fires shortly after the timer was due to end and restores your volume anyway, even though the countdown itself stopped running.
- **The device restarts mid-timer** — the app remembers what to restore (which stream and to what level) in on-device storage that survives a reboot. If the timer's end time had already passed by the time you turn the phone back on, volume is restored immediately; if there's still time left, the backup alarm is simply re-armed for what remains. This applies to both manual mutes and scheduled ones.
- **You force-stop the app** — this is the one case that isn't covered. Android cancels an app's scheduled alarms as part of force-stopping it, which takes the backup alarm down with it. If you force-stop the app while a mute timer is running, you'll need to raise your volume manually.

#### What if the device has a very low maximum volume?

The restore volume is always clamped to the device's actual maximum. If the value stored in Settings is higher than the device maximum (e.g., due to a device change), the app will restore to the maximum available level rather than crashing or producing an error.

---

## 4. Scheduled Mutes

### 4.1 Viewing Your Schedules

Tap the **Schedules** tab in the bottom navigation bar. This screen lists all your saved schedules.

Each schedule card displays:

- **Schedule name** — the label you gave it (e.g., "Morning standup", "Friday class").
- **Active days** — a row of day-of-week chips, highlighted for the days this schedule is active.
- **Start time** — shown in 12-hour format (e.g., 9:00 AM).
- **Mute duration** — shown as hours and minutes (e.g., 1h 30m).
- **Enable/disable toggle** — a switch on the right side of the card. Flipping it off suspends the schedule without deleting it.

If you have no schedules yet, the screen shows an empty-state illustration with a **"Create First Schedule"** button.

---

### 4.2 Creating a New Schedule

1. On the **Schedules** screen, tap the **New Schedule** floating button in the bottom-right corner.
2. The **New Schedule** editor opens. Fill in each field:

#### Schedule Name

Type a descriptive name in the **"Schedule name"** field. This label appears on the schedule card and in any related notifications.

Examples: `Morning standup`, `Weekly class`, `Friday focus block`

The name is required — saving will show an error if this field is empty.

#### Repeat Days

Tap individual day chips (Mon, Tue, Wed, Thu, Fri, Sat, Sun) to toggle them on or off. Active days are highlighted.

For common patterns, use the quick-preset chips below the day selector:

- **Weekdays** — selects Monday through Friday, clears Saturday and Sunday.
- **Weekends** — selects Saturday and Sunday, clears weekdays.
- **Every day** — selects all seven days. Does not deselect anything already selected.

At least one day must be selected — saving will show an error if no days are chosen.

#### Start Time

The current start time is displayed in large text (e.g., **9:00 AM**). Tap **Change** to open a time picker dialog and set the exact hour and minute you want the mute to begin.

#### Mute Duration

The current duration is displayed in large text (e.g., **1h 30m**). Tap **Change** to open the same preset-chips-and-stepper duration picker used in the manual mute popup (see [Section 3.2](#32-using-the-mute-duration-popup)) — presets for 15m/30m/1h/2h, plus steppers for anything else (hours by 1, minutes by 5).

- Example: to mute for 90 minutes, tap the **1h** preset, then **+** once under Minutes.
- Example: to mute for 45 minutes, tap **+** nine times under Minutes from zero, or start from the **30m** preset and add 15 more.

A duration of at least **5 minutes** is required — saving will show an error for anything shorter.

3. Tap **Create Schedule** at the bottom of the screen. The schedule is saved and alarms are set immediately for all selected days.

> **Note:** If the "Exact alarms" permission isn't granted, the schedule still saves, but a message appears on the Schedules screen explaining that nothing was actually armed, with a button that opens the permission page directly.

---

### 4.3 Editing a Schedule

1. On the **Schedules** screen, tap any schedule card to open the editor.
2. All fields (name, days, start time, duration) are editable.
3. Tap **Save Changes** when finished. The existing alarms for that schedule are cancelled and new ones are set based on your updated configuration.

---

### 4.4 Enabling and Disabling Schedules

Each schedule card has a toggle switch. Flipping it:

- **Off** — cancels all pending alarms for that schedule. The schedule remains saved and can be re-enabled at any time.
- **On** — immediately re-arms all pending alarms for the schedule based on its days, start time, and duration. If the "Exact alarms" permission isn't granted, the toggle still switches on but a message appears explaining nothing was actually armed, with a button to grant the permission.

This is useful for temporarily suspending a schedule (e.g., during a vacation) without losing its configuration.

---

### 4.5 Deleting a Schedule

There are two ways to delete a schedule:

**Swipe to delete:**
On the **Schedules** list, swipe a schedule card to the left. The card reveals a red "Delete" label. Release to confirm deletion. All pending alarms for that schedule are cancelled immediately.

**Delete from editor:**
Open a schedule by tapping it, then tap the trash icon in the top-right corner of the editor. A confirmation dialog will appear — tap **Delete** to confirm. Tap **Cancel** to go back without deleting.

---

### 4.6 How Scheduled Mutes Fire

When a scheduled mute fires:

1. `AlarmManager` wakes the app at the precise scheduled time, even if your device was asleep.
2. The alarm is **immediately re-armed** for the next occurrence of that schedule, before anything else happens, so nothing that goes wrong later can stop the schedule from repeating.
3. The app mutes your media volume to zero silently (no popup — scheduled mutes are automatic and don't interrupt you).
4. A **foreground notification** appears: "Phone muted — Restoring volume in Xh Ym", counting down.
5. At the end of the duration, volume is restored to your default restore volume and the restore chime plays (if enabled).

> **Note:** Scheduled mutes survive device reboots, app updates, and clock or time-zone changes. A `BootReceiver` re-arms all enabled schedule alarms automatically in each of those cases, and again the moment you grant the exact-alarm permission.

---

## 5. Settings

The **Settings** tab is the home screen of the app. All preferences are saved automatically as you change them — there is no Save button.

A **Close** button (X) in the top-right corner sends the app to the background, the same as pressing your device's Home button. The app keeps running — volume monitoring and any active mute timer or schedule are unaffected — it just gets out of your way.

### 5.1 Restore Volume

A slider that sets the **default volume** your device returns to when any mute timer expires.

- Drag right to increase volume, drag left to decrease.
- The icon to the left of the label updates to reflect the current level (muted, low, or high).
- The percentage (e.g., **65%**) is shown to the right of the label.
- This value is scaled to your specific device's maximum volume, so 65% represents the same perceived loudness regardless of manufacturer.

This value can be temporarily overridden per-mute in the duration popup (see [Section 3.2](#32-using-the-mute-duration-popup)).

---

### 5.2 App Theme

Three radio options under **App Settings**:

- **Light** — forces the app to use the light theme regardless of system setting.
- **Dark** — forces the app to use the dark theme.
- **System** — follows your device's system-wide dark/light mode setting (default).

Tap any option to switch immediately.

---

### 5.3 Show Timer Popup on Mute

A toggle switch. When **on**, the mute duration dialog appears automatically over your screen whenever media volume is set to zero.

When **off**, the app still runs in the background and scheduled mutes still fire, but pressing volume-down to zero will not trigger any popup. Your phone mutes as it normally would without the app intervening.

Use this setting if you want to keep scheduled mutes active but prefer to control manual mutes yourself without the popup.

---

### 5.4 Chime on Mute

A toggle switch. When **on**, a sound plays the instant your device is muted (both for manual mutes and scheduled mutes).

**To change the chime sound:**
Tap anywhere on the "Chime on mute" row (not just the switch). The system ringtone picker opens, letting you choose from any notification sound on your device. The name of the currently selected sound is shown below the label.

Tap **Silent** in the ringtone picker to turn this chime off (the switch flips off for you), or tap **Default** to use the system default notification tone.

---

### 5.5 Chime on Restore

A toggle switch. When **on**, a sound plays when your volume is automatically restored at the end of a mute timer. This is the audio confirmation that you're "back" — useful if your phone was in your pocket and you couldn't see the toast notification.

**To change the chime sound:**
Tap the "Chime on restore" row. The system ringtone picker opens. The currently selected sound name is displayed below the label.

Chimes on mute and restore can be set to different sounds independently.

---

### 5.6 Permissions Status Panel

At the bottom of the Settings screen, the **Permissions** section shows the current status of each required permission.

| Permission | What it does | Status indicator |
|---|---|---|
| Display over other apps | Shows the mute popup over the lock screen | Green check (Granted) or red warning (Grant button) |
| Exact alarms (Android 12+) | Fires scheduled mutes at the precise time | Green check (Granted) or red warning (Grant button) |
| Ignore battery optimization | Keeps the volume monitor from being killed in the background | Green check (Granted) or red warning (Grant button) |

Tapping **Grant** for any row opens the relevant system settings page directly. After granting, return to the app — the status refreshes automatically when the app resumes.

---

## 6. Complete Feature List

### Manual Mute
- Detects when media or ring volume transitions to zero via physical button, software slider, or the volume-panel mute icon
- Restores the same stream that was muted
- Displays a floating overlay popup over any app (not above the lock screen)
- Preset-chips-and-stepper duration picker (15m/30m/1h/2h presets, plus hour/5-minute steppers)
- Per-mute restore volume override via in-dialog slider
- Optional mute chime — plays immediately when mute is detected
- "Skip" option to dismiss the popup and keep the phone muted indefinitely
- Persistent countdown notification showing remaining time (updates every ~10 seconds)
- Automatic volume restore when the timer expires
- Toast notification on restore: "System volume has been restored"
- Optional restore chime — plays after volume is raised

### Scheduled Mutes
- Create unlimited named recurring mute schedules
- Select any combination of days of the week (Mon–Sun)
- Quick-preset day selectors: Weekdays, Weekends, Every day
- Precise start time picker in 12-hour format
- Mute duration picker, independent of start time (same preset-chips-and-stepper picker as manual mute)
- Per-schedule enable/disable toggle — suspend a schedule without deleting it
- Swipe-to-delete on the schedule list
- Delete from the schedule editor with a confirmation dialog
- Countdown notification identical to manual mutes (showing remaining time)
- Automatic re-scheduling after each occurrence — no manual intervention needed
- Alarms survive device reboots (re-armed on boot automatically)

### Settings & Customization
- Global default restore volume (percentage slider, device-max aware)
- Toggle the manual mute popup on or off independently of scheduled mutes
- Independent chime sounds for mute and restore events
- Chime sound selection via system ringtone picker (supports any notification sound)
- Light / Dark / System theme selection
- Permissions status panel with one-tap deep links to system settings

### Reliability & Background Operation
- Always-on volume monitor foreground service (silent, persistent notification)
- Mute timer foreground service prevents Android from killing the countdown
- Backup restore alarm and reboot-safe persisted restore state mean volume still comes back even if the app's process is killed or the device restarts mid-timer (does not cover force-stopping the app — see [Section 3.5](#35-edge-cases--behavior-notes))
- Self-expiring suppression window prevents the mute popup from re-triggering when the app itself changes volume
- Schedules re-arm themselves the instant they fire, and again after reboot, update, clock change, or permission grant
- Exact-alarm scheduling ensures scheduled mutes fire within seconds of the configured time

---

## 7. Frequently Asked Questions

**The mute popup didn't appear when I pressed volume down.**
Check that "Show timer popup on mute" is enabled in Settings, and that the "Display over other apps" permission is granted. If the permission was recently revoked, the app will post a notification prompting you to re-grant it. Also check that the "Volume monitoring active" notification is present, and that "Ignore battery optimization" shows Granted in the Permissions section — if your phone's battery manager has killed the monitor service, granting that exemption is the fix, not just reopening the app.

**My scheduled mute didn't fire.**
Make sure the "Exact alarms" permission is granted (Settings → Permissions). Without it, Android may defer scheduled alarms significantly. Also confirm the schedule is enabled (the toggle on the card should be on).

**The popup appears but the Start button is disabled.**
Both hours and minutes on the duration picker are set to zero. Tap a preset (15m/30m/1h/2h) or use the steppers to set at least one minute before tapping Start.

**Will my schedules still fire after a reboot?**
Yes. The app registers a `BootReceiver` that re-arms all enabled schedule alarms automatically when the device powers on.

**Can I have the popup without scheduled mutes, or scheduled mutes without the popup?**
Yes. The popup is controlled by the "Show timer popup on mute" toggle in Settings and is completely independent from the Schedules tab. You can use either feature, both, or neither.

**Can I choose a different sound for muting versus restoring?**
Yes. Tap the "Chime on mute" row to pick the mute sound, and tap the "Chime on restore" row to pick the restore sound. They are set independently.

**What happens if a new scheduled mute fires while a manual mute timer is already running?**
The new timer replaces the old one. The countdown resets to the scheduled duration, and volume will be restored at the end of the new timer.
