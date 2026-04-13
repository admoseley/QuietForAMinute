package com.admoseley.quietforaminute.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.app.NotificationManagerCompat
import com.admoseley.quietforaminute.MainActivity
import com.admoseley.quietforaminute.QuietApplication.Companion.CHANNEL_MONITOR
import com.admoseley.quietforaminute.QuietApplication.Companion.CHANNEL_PERMISSION
import com.admoseley.quietforaminute.R
import com.admoseley.quietforaminute.audio.ChimePlayer
import com.admoseley.quietforaminute.data.datastore.PreferencesRepository
import com.admoseley.quietforaminute.overlay.OverlayViewController
import com.admoseley.quietforaminute.receiver.VolumeReceiver
import com.admoseley.quietforaminute.scheduler.EXTRA_DURATION_MINUTES
import com.admoseley.quietforaminute.scheduler.EXTRA_RESTORE_VOLUME
import com.admoseley.quietforaminute.scheduler.EXTRA_SOURCE
import com.admoseley.quietforaminute.scheduler.SOURCE_MANUAL
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class OverlayService : Service() {

    @Inject lateinit var prefsRepository: PreferencesRepository
    @Inject lateinit var chimePlayer: ChimePlayer

    private lateinit var volumeReceiver: VolumeReceiver
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

        overlayViewController = OverlayViewController(this, prefsRepository, ::onDurationChosen)

        volumeReceiver = VolumeReceiver(onVolumeMuted = ::handleMuteDetected)
        val filter = IntentFilter(VolumeReceiver.ACTION_VOLUME_CHANGED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(volumeReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(volumeReceiver, filter)
        }
    }

    private fun handleMuteDetected() {
        // Check if this volume change was triggered programmatically by MuteTimerService
        if (OverlayServiceBridge.suppressCount.getAndUpdate { if (it > 0) it - 1 else 0 } > 0) {
            return
        }

        serviceScope.launch {
            val overlayEnabled = prefsRepository.overlayEnabled.first()
            if (!overlayEnabled) return@launch

            if (!Settings.canDrawOverlays(this@OverlayService)) {
                showOverlayPermissionNotification()
                return@launch
            }

            if (!overlayViewController.isShowing()) {
                if (prefsRepository.chimeOnMute.first()) {
                    val chimeUri = prefsRepository.muteChimeUri.first()
                    chimePlayer.playChime(chimeUri)
                }
                overlayViewController.show()
            }
        }
    }

    private fun onDurationChosen(hours: Int, minutes: Int, restoreVolume: Int) {
        val durationMinutes = hours * 60 + minutes
        if (durationMinutes <= 0) return

        val intent = Intent(this, MuteTimerService::class.java).apply {
            putExtra(EXTRA_DURATION_MINUTES, durationMinutes)
            putExtra(EXTRA_SOURCE, SOURCE_MANUAL)
            putExtra(EXTRA_RESTORE_VOLUME, restoreVolume)
        }
        startForegroundService(intent)
    }

    private fun buildMonitorNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_MONITOR)
            .setSmallIcon(R.drawable.ic_volume_monitor)
            .setContentTitle("Volume monitoring active")
            .setContentText("Listening for volume changes")
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
            data = android.net.Uri.parse("package:$packageName")
        }
        val pi = PendingIntent.getActivity(this, 99, intent, PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(this, CHANNEL_PERMISSION)
            .setSmallIcon(R.drawable.ic_volume_monitor)
            .setContentTitle("Permission needed")
            .setContentText("Tap to allow QuietForAMinute to show overlays")
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(this).notify(PERM_NOTIF_ID, notification)
        } catch (_: SecurityException) { /* POST_NOTIFICATIONS not granted */ }
    }

    override fun onDestroy() {
        unregisterReceiver(volumeReceiver)
        overlayViewController.dismiss()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val NOTIF_ID = 1001
        const val PERM_NOTIF_ID = 1003
    }
}
