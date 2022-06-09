package com.nipunapps.mxclone.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.nipunapps.mxclone.other.Constants

@Entity(tableName = Constants.LASTPLAYBACK_TABLE)
data class LastPlaybackTimeEntity(
    @PrimaryKey
    val mediaId : Long = 0L,
    val lastPlaybackTime: Long = 0L,
    val bucketId : String = ""
)
