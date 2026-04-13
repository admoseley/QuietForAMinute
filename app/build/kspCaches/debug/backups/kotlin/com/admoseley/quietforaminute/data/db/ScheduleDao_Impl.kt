package com.admoseley.quietforaminute.`data`.db

import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import javax.`annotation`.processing.Generated
import kotlin.Boolean
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class ScheduleDao_Impl(
  __db: RoomDatabase,
) : ScheduleDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfScheduleEntity: EntityInsertAdapter<ScheduleEntity>

  private val __deleteAdapterOfScheduleEntity: EntityDeleteOrUpdateAdapter<ScheduleEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfScheduleEntity = object : EntityInsertAdapter<ScheduleEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `schedules` (`id`,`label`,`daysBitmask`,`triggerHour`,`triggerMinute`,`durationMinutes`,`isEnabled`) VALUES (nullif(?, 0),?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: ScheduleEntity) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.label)
        statement.bindLong(3, entity.daysBitmask.toLong())
        statement.bindLong(4, entity.triggerHour.toLong())
        statement.bindLong(5, entity.triggerMinute.toLong())
        statement.bindLong(6, entity.durationMinutes.toLong())
        val _tmp: Int = if (entity.isEnabled) 1 else 0
        statement.bindLong(7, _tmp.toLong())
      }
    }
    this.__deleteAdapterOfScheduleEntity = object : EntityDeleteOrUpdateAdapter<ScheduleEntity>() {
      protected override fun createQuery(): String = "DELETE FROM `schedules` WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: ScheduleEntity) {
        statement.bindLong(1, entity.id)
      }
    }
  }

  public override suspend fun upsert(schedule: ScheduleEntity): Long = performSuspending(__db,
      false, true) { _connection ->
    val _result: Long = __insertAdapterOfScheduleEntity.insertAndReturnId(_connection, schedule)
    _result
  }

  public override suspend fun delete(schedule: ScheduleEntity): Unit = performSuspending(__db,
      false, true) { _connection ->
    __deleteAdapterOfScheduleEntity.handle(_connection, schedule)
  }

  public override fun getAllSchedules(): Flow<List<ScheduleEntity>> {
    val _sql: String = "SELECT * FROM schedules ORDER BY triggerHour, triggerMinute"
    return createFlow(__db, false, arrayOf("schedules")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _cursorIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _cursorIndexOfLabel: Int = getColumnIndexOrThrow(_stmt, "label")
        val _cursorIndexOfDaysBitmask: Int = getColumnIndexOrThrow(_stmt, "daysBitmask")
        val _cursorIndexOfTriggerHour: Int = getColumnIndexOrThrow(_stmt, "triggerHour")
        val _cursorIndexOfTriggerMinute: Int = getColumnIndexOrThrow(_stmt, "triggerMinute")
        val _cursorIndexOfDurationMinutes: Int = getColumnIndexOrThrow(_stmt, "durationMinutes")
        val _cursorIndexOfIsEnabled: Int = getColumnIndexOrThrow(_stmt, "isEnabled")
        val _result: MutableList<ScheduleEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: ScheduleEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_cursorIndexOfId)
          val _tmpLabel: String
          _tmpLabel = _stmt.getText(_cursorIndexOfLabel)
          val _tmpDaysBitmask: Int
          _tmpDaysBitmask = _stmt.getLong(_cursorIndexOfDaysBitmask).toInt()
          val _tmpTriggerHour: Int
          _tmpTriggerHour = _stmt.getLong(_cursorIndexOfTriggerHour).toInt()
          val _tmpTriggerMinute: Int
          _tmpTriggerMinute = _stmt.getLong(_cursorIndexOfTriggerMinute).toInt()
          val _tmpDurationMinutes: Int
          _tmpDurationMinutes = _stmt.getLong(_cursorIndexOfDurationMinutes).toInt()
          val _tmpIsEnabled: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_cursorIndexOfIsEnabled).toInt()
          _tmpIsEnabled = _tmp != 0
          _item =
              ScheduleEntity(_tmpId,_tmpLabel,_tmpDaysBitmask,_tmpTriggerHour,_tmpTriggerMinute,_tmpDurationMinutes,_tmpIsEnabled)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getById(id: Long): ScheduleEntity? {
    val _sql: String = "SELECT * FROM schedules WHERE id = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, id)
        val _cursorIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _cursorIndexOfLabel: Int = getColumnIndexOrThrow(_stmt, "label")
        val _cursorIndexOfDaysBitmask: Int = getColumnIndexOrThrow(_stmt, "daysBitmask")
        val _cursorIndexOfTriggerHour: Int = getColumnIndexOrThrow(_stmt, "triggerHour")
        val _cursorIndexOfTriggerMinute: Int = getColumnIndexOrThrow(_stmt, "triggerMinute")
        val _cursorIndexOfDurationMinutes: Int = getColumnIndexOrThrow(_stmt, "durationMinutes")
        val _cursorIndexOfIsEnabled: Int = getColumnIndexOrThrow(_stmt, "isEnabled")
        val _result: ScheduleEntity?
        if (_stmt.step()) {
          val _tmpId: Long
          _tmpId = _stmt.getLong(_cursorIndexOfId)
          val _tmpLabel: String
          _tmpLabel = _stmt.getText(_cursorIndexOfLabel)
          val _tmpDaysBitmask: Int
          _tmpDaysBitmask = _stmt.getLong(_cursorIndexOfDaysBitmask).toInt()
          val _tmpTriggerHour: Int
          _tmpTriggerHour = _stmt.getLong(_cursorIndexOfTriggerHour).toInt()
          val _tmpTriggerMinute: Int
          _tmpTriggerMinute = _stmt.getLong(_cursorIndexOfTriggerMinute).toInt()
          val _tmpDurationMinutes: Int
          _tmpDurationMinutes = _stmt.getLong(_cursorIndexOfDurationMinutes).toInt()
          val _tmpIsEnabled: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_cursorIndexOfIsEnabled).toInt()
          _tmpIsEnabled = _tmp != 0
          _result =
              ScheduleEntity(_tmpId,_tmpLabel,_tmpDaysBitmask,_tmpTriggerHour,_tmpTriggerMinute,_tmpDurationMinutes,_tmpIsEnabled)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun setEnabled(id: Long, enabled: Boolean) {
    val _sql: String = "UPDATE schedules SET isEnabled = ? WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        val _tmp: Int = if (enabled) 1 else 0
        _stmt.bindLong(_argIndex, _tmp.toLong())
        _argIndex = 2
        _stmt.bindLong(_argIndex, id)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
