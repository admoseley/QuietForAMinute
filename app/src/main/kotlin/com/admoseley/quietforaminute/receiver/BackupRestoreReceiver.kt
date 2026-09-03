package com.admoseley.quietforaminute.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.admoseley.quietforaminute.data.datastore.PreferencesRepository
import com.admoseley.quietforaminute.scheduler.EXTRA_BACKUP_RESTORE_VOLUME
import com.admoseley.quietforaminute.scheduler.EXTRA_BACKUP_STREAM_TYPE
import com.admoseley.quietforaminute.service.MuteTimerService
import com.admoseley.quietforaminute.service.VolumeRestorer
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Fires only when [MuteTimerService] failed to restore volume itself — normally this alarm is
 * cancelled by the service before it ever reaches here (see [BackupRestoreScheduler]'s trigger
 * buffer). Reaching this receiver means the timer's foreground service was killed mid-countdown
 * (Doze, an OEM battery manager, a force-stop, low memory) and never got to run its own restore.
 */
@AndroidEntryPoint
class BackupRestoreReceiver : BroadcastReceiver() {

    @Inject lateinit var volumeRestorer: VolumeRestorer
    @Inject lateinit var prefsRepository: PreferencesRepository

    override fun onReceive(context: Context, intent: Intent) {
        val streamType = intent.getIntExtra(EXTRA_BACKUP_STREAM_TYPE, -1)
        val manualRestoreVolume = intent.getIntExtra(EXTRA_BACKUP_RESTORE_VOLUME, -1)
        if (streamType == -1) return // FLAG_NO_CREATE races aside, extras should always be present.

        Log.w(TAG, "Backup restore firing for stream $streamType — MuteTimerService did not finish in time")

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try {
                volumeRestorer.restore(streamType, manualRestoreVolume)
                prefsRepository.clearPendingRestore()
                // The service's own notification would otherwise be orphaned on the rare chance
                // the process is still alive but just stuck (vs. actually killed).
                NotificationManagerCompat.from(context).cancel(MuteTimerService.NOTIF_ID)
            } catch (t: Throwable) {
                Log.e(TAG, "Backup restore failed", t)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        const val TAG = "BackupRestoreReceiver"
    }
}
