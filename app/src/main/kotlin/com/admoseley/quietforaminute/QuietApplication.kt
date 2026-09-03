package com.admoseley.quietforaminute

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class QuietApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val nm = getSystemService(NotificationManager::class.java)

        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_MONITOR,
                getString(R.string.notif_channel_monitor_name),
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = getString(R.string.notif_channel_monitor_desc)
                setShowBadge(false)
            }
        )

        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_TIMER,
                getString(R.string.notif_channel_timer_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notif_channel_timer_desc)
                setShowBadge(true)
            }
        )

        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_PERMISSION,
                getString(R.string.notif_channel_permission_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.notif_channel_permission_desc)
            }
        )
    }

    companion object {
        const val CHANNEL_MONITOR = "channel_monitor"
        const val CHANNEL_TIMER = "channel_timer"
        const val CHANNEL_PERMISSION = "channel_permission"
    }
}
