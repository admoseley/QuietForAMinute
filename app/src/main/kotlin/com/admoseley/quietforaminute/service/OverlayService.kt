package com.admoseley.quietforaminute.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import com.admoseley.quietforaminute.MainActivity
import com.admoseley.quietforaminute.QuietApplication.Companion.CHANNEL_MONITOR
import com.admoseley.quietforaminute.QuietApplication.Companion.CHANNEL_PERMISSION
import com.admoseley.quietforaminute.R
import com.admoseley.quietforaminute.audio.ChimePlayer
import com.admoseley.quietforaminute.data.datastore.PreferencesRepository
import com.admoseley.quietforaminute.overlay.OverlayViewController
import com.admoseley.quietforaminute.receiver.VolumeReceiver
import com.admoseley.quietforaminute.receiver.VolumeTransition
import com.admoseley.quietforaminute.scheduler.EXTRA_DND_ENABLED
import com.admoseley.quietforaminute.scheduler.EXTRA_DURATION_MINUTES
import com.admoseley.quietforaminute.scheduler.EXTRA_RESTORE_VOLUME
import com.admoseley.quietforaminute.scheduler.EXTRA_SOURCE
import com.admoseley.quietforaminute.scheduler.EXTRA_STREAM_TYPE
import com.admoseley.quietforaminute.scheduler.SOURCE_MANUAL
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Always-on foreground service: listens for the user muting the phone and shows the
 * "Mute for how long?" popup.
 *
 * Trigger pipeline (see [handleStreamMuted]):
 *  1. [VolumeReceiver] sees a music/ring stream transition to zero (or get mute-flagged).
 *  2. Ignore it if the app itself just changed the volume ([OverlayServiceBridge]).
 *  3. Ignore it if the popup is disabled in Settings.
 *  4. If the overlay permission is missing, post a notification instead.
 *  5. Ignore it if the popup is already on screen.
 *  6. Play the mute chime (optional) and show the popup.
 *
 * Reliability notes:
 *  - This service is what keeps the receiver alive. If an OEM battery optimiser kills it, no popup
 *    can appear. The manifest already declares REQUEST_IGNORE_BATTERY_OPTIMIZATIONS; consider a
 *    Settings row that deep-links to the exemption dialog on devices known for aggressive killing.
 *  - [onStartCommand] returns START_STICKY so the system re-creates the service after a kill.
 */
@AndroidEntryPoint
class OverlayService : Service() {

    @Inject lateinit var prefsRepository: PreferencesRepository
    @Inject lateinit var chimePlayer: ChimePlayer
    @Inject lateinit var dndController: DndController

    private var volumeReceiver: VolumeReceiver? = null
    private lateinit var overlayViewController: OverlayViewController
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        ServiceCompat.startForeground(
            this,
            NOTIF_ID,
            buildMonitorNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        )

        overlayViewController =
            OverlayViewController(this, prefsRepository, dndController, ::onDurationChosen)

        // VOLUME_CHANGED / STREAM_MUTE_CHANGED are system broadcasts: RECEIVER_NOT_EXPORTED still
        // receives them (they originate from the system UID) while refusing any other sender.
        val receiver = VolumeReceiver(onTransition = ::handleVolumeTransition)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, VolumeReceiver.intentFilter(), Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(receiver, VolumeReceiver.intentFilter())
        }
        volumeReceiver = receiver
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    private fun handleVolumeTransition(transition: VolumeTransition, streamType: Int) {
        // Our own setStreamVolume / adjustStreamVolume calls arrive here as well. Anything inside
        // the window MuteTimerService opened right before changing volume is not a user action —
        // this is also what stops the timer's *own* end-of-countdown restore from looking like
        // the user manually restoring volume and cancelling the timer that just finished.
        if (OverlayServiceBridge.isWithinProgrammaticWindow()) {
            Log.d(TAG, "Ignoring programmatic volume change on stream $streamType")
            return
        }

        when (transition) {
            VolumeTransition.MUTED -> handleStreamMuted(streamType)
            VolumeTransition.UNMUTED -> handleStreamUnmuted(streamType)
        }
    }

    /**
     * The user brought volume back themselves while a timer was counting down, so the timer has
     * nothing left to do — cancel it rather than leaving it to re-set the volume later (issue
     * #42). Checking [PreferencesRepository.pendingRestore] first means an ordinary volume nudge
     * with no timer running costs nothing but a DataStore read.
     */
    private fun handleStreamUnmuted(streamType: Int) {
        serviceScope.launch {
            if (prefsRepository.pendingRestore.first() == null) return@launch

            Log.d(TAG, "Manual restore on stream $streamType — cancelling the running timer")
            try {
                startService(
                    Intent(this@OverlayService, MuteTimerService::class.java)
                        .setAction(MuteTimerService.ACTION_CANCEL_TIMER)
                )
            } catch (e: Exception) {
                Log.e(TAG, "Unable to deliver timer cancel", e)
            }
        }
    }

    private fun handleStreamMuted(streamType: Int) {
        serviceScope.launch {
            if (!prefsRepository.overlayEnabled.first()) return@launch

            if (!Settings.canDrawOverlays(this@OverlayService)) {
                showOverlayPermissionNotification()
                return@launch
            }

            // isShowing() flips synchronously inside show(), so a second broadcast for the same
            // gesture (e.g. VOLUME_CHANGED and STREAM_MUTE_CHANGED both fire when the ringer goes
            // to vibrate) collapses into a single popup.
            if (overlayViewController.isShowing()) return@launch

            if (prefsRepository.chimeOnMute.first()) {
                chimePlayer.playChime(prefsRepository.muteChimeUri.first())
            }
            overlayViewController.show(streamType)
        }
    }

    private fun onDurationChosen(
        hours: Int,
        minutes: Int,
        restoreVolume: Int,
        streamType: Int,
        dndEnabled: Boolean
    ) {
        val durationMinutes = hours * 60 + minutes
        if (durationMinutes <= 0) return

        // Remember the DND choice so the toggle comes back the way they left it next time.
        serviceScope.launch { prefsRepository.setDndWithMute(dndEnabled) }

        val intent = Intent(this, MuteTimerService::class.java).apply {
            putExtra(EXTRA_DURATION_MINUTES, durationMinutes)
            putExtra(EXTRA_SOURCE, SOURCE_MANUAL)
            putExtra(EXTRA_RESTORE_VOLUME, restoreVolume)
            putExtra(EXTRA_STREAM_TYPE, streamType)
            putExtra(EXTRA_DND_ENABLED, dndEnabled)
        }
        try {
            startForegroundService(intent)
        } catch (e: Exception) {
            // ForegroundServiceStartNotAllowedException on 12+ if background restrictions apply.
            Log.e(TAG, "Unable to start MuteTimerService", e)
        }
    }

    private fun buildMonitorNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_MONITOR)
            .setSmallIcon(R.drawable.ic_volume_monitor)
            .setContentTitle(getString(R.string.notif_monitor_title))
            .setContentText(getString(R.string.notif_monitor_text))
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    this, 0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()

    private fun showOverlayPermissionNotification() {
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
            data = Uri.parse("package:$packageName")
        }
        val pi = PendingIntent.getActivity(this, 99, intent, PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(this, CHANNEL_PERMISSION)
            .setSmallIcon(R.drawable.ic_volume_monitor)
            .setContentTitle(getString(R.string.notif_permission_title))
            .setContentText(getString(R.string.notif_permission_text, getString(R.string.app_name)))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(this).notify(PERM_NOTIF_ID, notification)
        } catch (_: SecurityException) { /* POST_NOTIFICATIONS not granted */ }
    }

    override fun onDestroy() {
        volumeReceiver?.let { runCatching { unregisterReceiver(it) } }
        volumeReceiver = null
        if (::overlayViewController.isInitialized) overlayViewController.release()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "OverlayService"
        const val NOTIF_ID = 1001
        const val PERM_NOTIF_ID = 1003
    }
}
