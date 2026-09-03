package com.admoseley.quietforaminute.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ScheduleEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scheduleDao(): ScheduleDao

    companion object {
        /**
         * Adds `dndEnabled` for the Do Not Disturb option (issue #45).
         *
         * This migration is not optional housekeeping: the database is built with
         * `fallbackToDestructiveMigration`, so without it a version bump would drop the table and
         * silently delete every schedule the user had saved. Existing rows default to 0 (off),
         * which preserves exactly the behaviour those schedules had before the column existed.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE schedules ADD COLUMN dndEnabled INTEGER NOT NULL DEFAULT 0"
                )
            }
        }
    }
}
