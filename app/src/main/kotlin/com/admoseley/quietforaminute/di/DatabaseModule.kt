package com.admoseley.quietforaminute.di

import android.content.Context
import androidx.room.Room
import com.admoseley.quietforaminute.data.db.AppDatabase
import com.admoseley.quietforaminute.data.db.ScheduleDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "quiet_db")
            // Registered migrations always win over the destructive fallback, so a real upgrade
            // path keeps the user's schedules. The fallback stays only as a last resort for a
            // version gap no migration covers (e.g. downgrade, or a sideloaded older build).
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .fallbackToDestructiveMigration(true)
            .build()

    @Provides
    fun provideScheduleDao(db: AppDatabase): ScheduleDao = db.scheduleDao()
}
