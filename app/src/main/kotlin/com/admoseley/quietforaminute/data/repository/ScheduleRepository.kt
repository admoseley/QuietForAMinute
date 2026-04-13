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

@Singleton
class ScheduleRepository @Inject constructor(
    private val dao: ScheduleDao,
    private val alarmScheduler: AlarmScheduler
) {

    val schedules: Flow<List<Schedule>> = dao.getAllSchedules()
        .map { list -> list.map { it.toDomain() } }

    suspend fun getById(id: Long): Schedule? = dao.getById(id)?.toDomain()

    suspend fun save(schedule: Schedule): Schedule {
        if (schedule.id != 0L) {
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
        if (enabled) alarmScheduler.schedule(schedule)
        else alarmScheduler.cancelAllDaysForSchedule(schedule.id)
    }
}
