package com.nipunapps.mxclone.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.nipunapps.mxclone.other.Constants

@Entity(tableName = Constants.DURATION_TABLE)
data class LastPlayDurationEntity(
    @PrimaryKey
    val mediaId : Long = 0L,
    val lastDuration : Long = 0L
)
