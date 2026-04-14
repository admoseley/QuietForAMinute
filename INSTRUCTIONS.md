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

Quiet For A Minute needs two special permissions to function. Both are explained inside the app on the **Settings** screen, under the **Permissions** section.

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

### 3.1 How It Gets Triggered

The manual mute flow starts the moment your media volume reaches zero — whether you pressed the physical volume-down button repeatedly or dragged the volume slider all the way down. The app runs a lightweight background service (`OverlayService`) that listens for this event at all times.

When zero volume is detected:

1. If the overlay is enabled (see [Section 5.3](#53-show-timer-popup-on-mute)), the **Mute Duration Dialog** appears on screen.
2. If a chime-on-mute sound is configured (see [Section 5.4](#54-chime-on-mute)), it plays at that moment.

> **Note:** The popup only appears if "Show timer popup on mute" is turned on AND the overlay permission has been granted. If either condition is not met, the phone mutes normally with no popup.

---

### 3.2 Using the Mute Duration Popup

The **Mute Duration Dialog** appears as a floating card over whatever is currently on your screen. It contains three areas:

#### Choosing a Duration

The dialog shows a **Material 3 time picker** in the center of the card. This picker works like a clock, letting you select hours and minutes.

- Tap the **hour** field and enter or spin to your desired number of hours (0–23).
- Tap the **minute** field and enter or spin to your desired number of minutes (0–59).
- The picker defaults to **0 hours, 30 minutes** — a sensible starting point for most short mutes.

#### Adjusting the Restore Volume (Per-Mute Override)

Below the time picker is a **Restore Volume** slider. This lets you set the exact volume level your device will return to when this specific timer ends — independently of the global default in Settings.

- Drag the slider to the desired level. The percentage is shown live to the right.
- This override applies only to the current mute. Future mutes will still use the global default unless you adjust it here again.

#### Starting or Skipping

- Tap **Start** to begin the countdown. The button is disabled if both hours and minutes are set to zero.
- Tap **Skip** to dismiss the popup without starting any timer. Your phone remains muted, but volume will not be restored automatically.

---

### 3.3 The Timer Notification

Once you tap **Start**, the popup dismisses and a persistent foreground notification appears in your notification shade. It shows:

- **Title:** "Phone muted"
- **Body:** "Restoring volume in Xh Ym" — updated roughly every 10 seconds as the countdown progresses.

Tapping the notification opens the app. The notification cannot be dismissed manually while the timer is running — this is intentional, as it ensures the background service stays alive.

---

### 3.4 Volume Restore & Chime

When the countdown reaches zero:

1. The app restores your media volume to the level set in the mute dialog (or the global default if no override was set).
2. A short toast message appears: **"System volume has been restored"**.
3. If **Chime on restore** is enabled (see [Section 5.5](#55-chime-on-restore)), your selected restore chime sound plays immediately after the volume is raised.
4. The timer notification disappears from the notification shade.

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

The current duration is displayed in large text (e.g., **1h 30m**). Tap **Change** to open a duration picker. This picker uses a 24-hour format where the "hour" and "minute" fields represent the length of the mute, not a time of day.

- Example: to mute for 90 minutes, set 1 hour 30 minutes.
- Example: to mute for 45 minutes, set 0 hours 45 minutes.

A duration greater than zero is required — saving will show an error if both hours and minutes are zero.

3. Tap **Create Schedule** at the bottom of the screen. The schedule is saved and alarms are set immediately for all selected days.

---

### 4.3 Editing a Schedule

1. On the **Schedules** screen, tap any schedule card to open the editor.
2. All fields (name, days, start time, duration) are editable.
3. Tap **Save Changes** when finished. The existing alarms for that schedule are cancelled and new ones are set based on your updated configuration.

---

### 4.4 Enabling and Disabling Schedules

Each schedule card has a toggle switch. Flipping it:

- **Off** — cancels all pending alarms for that schedule. The schedule remains saved and can be re-enabled at any time.
- **On** — immediately re-arms all pending alarms for the schedule based on its days, start time, and duration.

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
2. The app mutes your media volume to zero silently (no popup — scheduled mutes are automatic and don't interrupt you).
3. A **foreground notification** appears: "Phone muted — Restoring volume in Xh Ym", counting down.
4. At the end of the duration, volume is restored to your default restore volume and the restore chime plays (if enabled).
5. The alarm is **automatically re-scheduled** for the next occurrence of that schedule. You never need to manually re-arm it.

> **Note:** Scheduled mutes survive device reboots. A `BootReceiver` re-arms all enabled schedule alarms automatically when the device starts up.

---

## 5. Settings

The **Settings** tab is the home screen of the app. All preferences are saved automatically as you change them — there is no Save button.

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

Tap **Silent** in the ringtone picker if you want no sound, or tap **Default** to use the system default notification tone.

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

Tapping **Grant** for any row opens the relevant system settings page directly. After granting, return to the app — the status refreshes automatically when the app resumes.

---

## 6. Complete Feature List

### Manual Mute
- Detects when media volume is set to zero via physical button or software slider
- Displays a floating overlay popup over the lock screen and any app
- Material 3 time picker for selecting mute duration (hours + minutes)
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
- Mute duration picker (hours and minutes, independent of start time)
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
- Smart suppression prevents the mute popup from re-triggering when the app itself restores volume
- `BootReceiver` re-arms all active schedules on device restart
- Exact-alarm scheduling ensures scheduled mutes fire within seconds of the configured time

---

## 7. Frequently Asked Questions

**The mute popup didn't appear when I pressed volume down.**
Check that "Show timer popup on mute" is enabled in Settings, and that the "Display over other apps" permission is granted. If the permission was recently revoked, the app will post a notification prompting you to re-grant it.

**My scheduled mute didn't fire.**
Make sure the "Exact alarms" permission is granted (Settings → Permissions). Without it, Android may defer scheduled alarms significantly. Also confirm the schedule is enabled (the toggle on the card should be on).

**The popup appears but the Start button is disabled.**
Both hours and minutes on the time picker are set to zero. Set at least one minute of duration before tapping Start.

**Will my schedules still fire after a reboot?**
Yes. The app registers a `BootReceiver` that re-arms all enabled schedule alarms automatically when the device powers on.

**Can I have the popup without scheduled mutes, or scheduled mutes without the popup?**
Yes. The popup is controlled by the "Show timer popup on mute" toggle in Settings and is completely independent from the Schedules tab. You can use either feature, both, or neither.

**Can I choose a different sound for muting versus restoring?**
Yes. Tap the "Chime on mute" row to pick the mute sound, and tap the "Chime on restore" row to pick the restore sound. They are set independently.

**What happens if a new scheduled mute fires while a manual mute timer is already running?**
The new timer replaces the old one. The countdown resets to the scheduled duration, and volume will be restored at the end of the new timer.
