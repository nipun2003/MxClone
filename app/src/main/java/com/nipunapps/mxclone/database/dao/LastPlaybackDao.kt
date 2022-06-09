package com.nipunapps.mxclone.database.dao

import androidx.room.*
import com.nipunapps.mxclone.database.entity.LastPlaybackTimeEntity
import com.nipunapps.mxclone.other.Constants

@Dao
interface LastPlaybackDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(lastPlaybackTime: LastPlaybackTimeEntity)

    @Delete
    suspend fun delete(lastPlaybackTime: LastPlaybackTimeEntity)

    @Query("SELECT * FROM ${Constants.LASTPLAYBACK_TABLE} ORDER BY lastPlaybackTime DESC")
    suspend fun getAllLastPlayback() : List<LastPlaybackTimeEntity>

    @Query("SELECT * FROM ${Constants.LASTPLAYBACK_TABLE} WHERE mediaId=:mediaId")
    suspend fun getSpecificPlaybackTime(mediaId : Long) : LastPlaybackTimeEntity?

    @Query("SELECT * FROM ${Constants.LASTPLAYBACK_TABLE} WHERE bucketId=:bucketId ORDER BY lastPlaybackTime DESC")
    suspend fun getLastPlayBackInsideBucket(bucketId : String) : List<LastPlaybackTimeEntity>
}