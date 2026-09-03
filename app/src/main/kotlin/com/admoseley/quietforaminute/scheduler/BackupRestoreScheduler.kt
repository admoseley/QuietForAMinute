package com.admoseley.quietforaminute.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.admoseley.quietforaminute.receiver.BackupRestoreReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Extras carried by the backup-restore alarm's PendingIntent. */
const val EXTRA_BACKUP_STREAM_TYPE = "extra_backup_stream_type"
const val EXTRA_BACKUP_RESTORE_VOLUME = "extra_backup_restore_volume"

/**
 * Arms a single exact alarm that performs the mute-timer restore even if
 * [com.admoseley.quietforaminute.service.MuteTimerService]'s process is gone by the time the
 * timer would have ended — the whole reason issue #8 exists. AlarmManager alarms survive process
 * death (unlike a foreground service's own delay loop), though not a device reboot; that gap is
 * covered separately by [com.admoseley.quietforaminute.receiver.BootReceiver] reading the same
 * persisted record from [com.admoseley.quietforaminute.data.datastore.PreferencesRepository].
 *
 * A single fixed requestCode is correct here: only one mute countdown runs at a time (see
 * MuteTimerService), so there is only ever one backup alarm to hold.
 */
@Singleton
class BackupRestoreScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    /**
     * [triggerAtEpochMillis] should include a small buffer past the expected restore time, so
     * the primary in-service restore ordinarily wins and cancels this before it ever fires.
     *
     * Falls back to an inexact alarm ([AlarmManager.set]) when exact alarms aren't permitted
     * (denied by default on API 34+) instead of throwing — this alarm is already a fallback for
     * "the primary path failed"; a possibly-late backup restore beats a crash or no restore at
     * all. `set()` needs no special permission on any API level.
     */
    fun schedule(triggerAtEpochMillis: Long, streamType: Int, manualRestoreVolume: Int) {
        val pi = pendingIntent(streamType, manualRestoreVolume)
        val canBeExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        if (canBeExact) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtEpochMillis, pi)
        } else {
            Log.w(TAG, "Exact alarms not permitted — arming an inexact backup restore alarm instead")
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtEpochMillis, pi)
        }
    }

    fun cancel() {
        val pi = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, BackupRestoreReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) ?: return
        alarmManager.cancel(pi)
        pi.cancel()
    }

    private fun pendingIntent(streamType: Int, manualRestoreVolume: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, BackupRestoreReceiver::class.java).apply {
                putExtra(EXTRA_BACKUP_STREAM_TYPE, streamType)
                putExtra(EXTRA_BACKUP_RESTORE_VOLUME, manualRestoreVolume)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    companion object {
        private const val TAG = "BackupRestoreScheduler"
        private const val REQUEST_CODE = 90210

        /** Gives the primary in-service restore a head start to finish and cancel this alarm. */
        const val TRIGGER_BUFFER_MS = 30_000L
    }
}
