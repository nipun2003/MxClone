package com.nipunapps.mxclone.ui

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.nipunapps.mxclone.database.PlaybackDatabase
import com.nipunapps.mxclone.database.dao.LastPlaybackDao
import com.nipunapps.mxclone.database.helper.LastPlaybackHelper
import com.nipunapps.mxclone.ui.repository.VideoRepository
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
    fun provideVideoRepository(@ApplicationContext context: Context): VideoRepository =
        VideoRepository(context = context)

    @Provides
    @Singleton
    fun providePlaybackDatabase(@ApplicationContext context: Context): PlaybackDatabase =
        Room.databaseBuilder(context, PlaybackDatabase::class.java, "playback_database")
            .build()

    @Provides
    @Singleton
    fun provideLastPlaybackHelper(playbackDatabase: PlaybackDatabase) : LastPlaybackHelper = LastPlaybackHelper(playbackDatabase.getLastPlaybackDao())
}