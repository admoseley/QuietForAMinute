package com.admoseley.quietforaminute.receiver

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.admoseley.quietforaminute.data.datastore.PreferencesRepository
import com.admoseley.quietforaminute.data.repository.ScheduleRepository
import com.admoseley.quietforaminute.scheduler.AlarmScheduler
import com.admoseley.quietforaminute.scheduler.BackupRestoreScheduler
import com.admoseley.quietforaminute.service.OverlayService
import com.admoseley.quietforaminute.service.VolumeRestorer
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Re-arms every enabled schedule whenever the system state that alarms depend on changes:
 *
 *  - BOOT_COMPLETED / MY_PACKAGE_REPLACED — AlarmManager drops all alarms on reboot; after an
 *    update they survive but re-arming is harmless and guards against changed extras.
 *  - TIMEZONE_CHANGED / TIME_SET — alarms are stored as RTC epoch millis, so a zone change or a
 *    manual clock change moves "8:00 AM" to a different instant.
 *  - SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED — the user just granted (or revoked) exact
 *    alarms in system settings; schedules saved while it was denied were never armed.
 *
 * On an actual reboot specifically (not the other triggers above — see below), also finishes any
 * mute timer that was running when the device went down (issue #8). AlarmManager alarms — the
 * backup restore MuteTimerService armed — do not survive a reboot the way the persisted DataStore
 * record does, so this is the only path that can resume that record: if the timer's end time
 * already passed, restore right away; otherwise re-arm the backup alarm for what's left.
 *
 * The always-on [OverlayService] is (re)started only for boot/update: starting a foreground
 * service from the other broadcasts is not exempt from background-start restrictions.
 *
 * Security: this receiver is exported (required for system broadcasts) but only reacts to the
 * protected system actions above, so a third-party app cannot use it to trigger work.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var scheduleRepository: ScheduleRepository
    @Inject lateinit var alarmScheduler: AlarmScheduler
    @Inject lateinit var prefsRepository: PreferencesRepository
    @Inject lateinit var volumeRestorer: VolumeRestorer
    @Inject lateinit var backupRestoreScheduler: BackupRestoreScheduler

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val startsOverlayService = action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED
        val reArms = startsOverlayService ||
            action == Intent.ACTION_TIMEZONE_CHANGED ||
            action == Intent.ACTION_TIME_CHANGED ||
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                action == AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED)
        if (!reArms) return

        Log.d(TAG, "Re-arming schedules for $action")

        // goAsync() extends the receiver's lifespan so the coroutine can finish re-registering
        // alarms before the process is killed.
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try {
                scheduleRepository.schedules.first()
                    .filter { it.isEnabled }
                    .forEach { alarmScheduler.schedule(it) }

                // Only a real reboot clears AlarmManager's alarms (an app replace/update does
                // not), so only ACTION_BOOT_COMPLETED needs to resume a pending mute restore.
                if (action == Intent.ACTION_BOOT_COMPLETED) {
                    resumePendingRestoreIfAny()
                }
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to re-arm schedules", t)
            } finally {
                pendingResult.finish()
            }
        }

        if (startsOverlayService) {
            try {
                ContextCompat.startForegroundService(context, Intent(context, OverlayService::class.java))
            } catch (e: Exception) {
                Log.e(TAG, "Unable to start OverlayService", e)
            }
        }
    }

    private suspend fun resumePendingRestoreIfAny() {
        val pending = prefsRepository.pendingRestore.first() ?: return
        if (pending.endEpochMillis <= System.currentTimeMillis()) {
            Log.w(TAG, "Device rebooted after a mute timer's end time already passed — restoring now")
            volumeRestorer.restore(pending.streamType, pending.manualRestoreVolume)
            prefsRepository.clearPendingRestore()
        } else {
            Log.d(TAG, "Device rebooted mid-timer — re-arming the backup restore alarm")
            backupRestoreScheduler.schedule(
                pending.endEpochMillis + BackupRestoreScheduler.TRIGGER_BUFFER_MS,
                pending.streamType,
                pending.manualRestoreVolume
            )
        }
    }

    private companion object {
        const val TAG = "BootReceiver"
    }
}
