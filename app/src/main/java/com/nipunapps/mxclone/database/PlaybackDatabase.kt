package com.nipunapps.mxclone.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.nipunapps.mxclone.database.dao.DurationDao
import com.nipunapps.mxclone.database.dao.LastPlaybackDao
import com.nipunapps.mxclone.database.entity.LastPlayDurationEntity
import com.nipunapps.mxclone.database.entity.LastPlaybackTimeEntity

@Database(
    entities = [LastPlaybackTimeEntity::class,LastPlayDurationEntity::class],
    version = 1,
    exportSchema = false
)
abstract class PlaybackDatabase : RoomDatabase() {

    abstract fun getLastPlaybackDao() : LastPlaybackDao
    abstract fun getLastPlayDuration() : DurationDao
}