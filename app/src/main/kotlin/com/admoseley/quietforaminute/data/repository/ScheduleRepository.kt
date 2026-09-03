package com.admoseley.quietforaminute.data.repository

import com.admoseley.quietforaminute.data.db.ScheduleDao
import com.admoseley.quietforaminute.data.db.toDomain
import com.admoseley.quietforaminute.data.db.toEntity
import com.admoseley.quietforaminute.domain.model.Schedule
import com.admoseley.quietforaminute.scheduler.AlarmScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single entry point for schedule persistence. Every write here also keeps AlarmManager in sync,
 * so callers never touch [AlarmScheduler] directly.
 *
 * Note: [AlarmScheduler.schedule] returns false when exact alarms are not permitted. That result
 * is currently swallowed, so a schedule can be saved as "enabled" with nothing armed. Surfacing it
 * to the edit screen (snackbar + link to the permission page) would be a worthwhile follow-up.
 */
@Singleton
class ScheduleRepository @Inject constructor(
    private val dao: ScheduleDao,
    private val alarmScheduler: AlarmScheduler
) {

    val schedules: Flow<List<Schedule>> = dao.getAllSchedules()
        .map { list -> list.map { it.toDomain() } }

    suspend fun getById(id: Long): Schedule? = dao.getById(id)?.toDomain()

    /** Inserts or updates, then re-arms every weekday slot from scratch. */
    suspend fun save(schedule: Schedule): Schedule {
        if (schedule.id != 0L) {
            // Cancel all seven slots, not just the currently selected days, so days removed in
            // this edit do not leave a stale alarm behind.
            alarmScheduler.cancelAllDaysForSchedule(schedule.id)
        }
        val id = dao.upsert(schedule.toEntity())
        val saved = schedule.copy(id = id)
        if (saved.isEnabled) alarmScheduler.schedule(saved)
        return saved
    }

    suspend fun delete(schedule: Schedule) {
        alarmScheduler.cancelAllDaysForSchedule(schedule.id)
        dao.delete(schedule.toEntity())
    }

    suspend fun setEnabled(schedule: Schedule, enabled: Boolean) {
        dao.setEnabled(schedule.id, enabled)
        if (enabled) {
            // The caller passes the schedule as it was *before* the toggle, i.e. isEnabled=false.
            // AlarmScheduler.schedule() early-returns for disabled schedules, so the old code
            // never armed anything when switching a schedule back on. Pass an updated copy.
            alarmScheduler.schedule(schedule.copy(isEnabled = true))
        } else {
            alarmScheduler.cancelAllDaysForSchedule(schedule.id)
        }
    }
}
