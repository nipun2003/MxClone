package com.nipunapps.mxclone.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nipunapps.mxclone.database.entity.LastPlayDurationEntity
import com.nipunapps.mxclone.other.Constants

@Dao
interface DurationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLastDuration(playbackDuration: LastPlayDurationEntity)

    @Query("SELECT * FROM ${Constants.DURATION_TABLE} WHERE mediaId=:mediaId")
    suspend fun getLastPlayDuration(mediaId : Long) : LastPlayDurationEntity?
}