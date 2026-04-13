package com.admoseley.quietforaminute.di

import android.content.Context
import com.admoseley.quietforaminute.audio.ChimePlayer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideChimePlayer(@ApplicationContext context: Context): ChimePlayer =
        ChimePlayer(context)
}
