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
                "Volume Monitor",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Persistent notification while volume monitoring is active"
                setShowBadge(false)
            }
        )

        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_TIMER,
                "Mute Timer",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows remaining time while your phone is muted"
                setShowBadge(true)
            }
        )

        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_PERMISSION,
                "Permission Requests",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when a required permission is missing"
            }
        )
    }

    companion object {
        const val CHANNEL_MONITOR = "channel_monitor"
        const val CHANNEL_TIMER = "channel_timer"
        const val CHANNEL_PERMISSION = "channel_permission"
    }
}
