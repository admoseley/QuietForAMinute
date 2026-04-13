package com.admoseley.quietforaminute.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.admoseley.quietforaminute.data.repository.ScheduleRepository
import com.admoseley.quietforaminute.scheduler.AlarmScheduler
import com.admoseley.quietforaminute.service.OverlayService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var scheduleRepository: ScheduleRepository
    @Inject lateinit var alarmScheduler: AlarmScheduler

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) return

        // goAsync() extends the BroadcastReceiver's lifespan so the coroutine
        // can finish re-registering alarms before the process is killed.
        val pendingResult = goAsync()
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        scope.launch {
            try {
                scheduleRepository.schedules.first()
                    .filter { it.isEnabled }
                    .forEach { alarmScheduler.schedule(it) }
            } finally {
                pendingResult.finish()
            }
        }

        ContextCompat.startForegroundService(
            context,
            Intent(context, OverlayService::class.java)
        )
    }
}
