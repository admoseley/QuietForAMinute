package com.admoseley.quietforaminute.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.admoseley.quietforaminute.scheduler.EXTRA_DURATION_MINUTES
import com.admoseley.quietforaminute.scheduler.EXTRA_SCHEDULE_ID
import com.admoseley.quietforaminute.scheduler.EXTRA_SOURCE
import com.admoseley.quietforaminute.scheduler.SOURCE_ALARM
import com.admoseley.quietforaminute.service.MuteTimerService

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val scheduleId = intent.getLongExtra(EXTRA_SCHEDULE_ID, -1L)
        val durationMinutes = intent.getIntExtra(EXTRA_DURATION_MINUTES, 0)

        val serviceIntent = Intent(context, MuteTimerService::class.java).apply {
            putExtra(EXTRA_SCHEDULE_ID, scheduleId)
            putExtra(EXTRA_DURATION_MINUTES, durationMinutes)
            putExtra(EXTRA_SOURCE, SOURCE_ALARM)
        }
        ContextCompat.startForegroundService(context, serviceIntent)
    }
}
