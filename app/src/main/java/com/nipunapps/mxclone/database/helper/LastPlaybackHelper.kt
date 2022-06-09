package com.nipunapps.mxclone.database.helper

import com.nipunapps.mxclone.database.dao.LastPlaybackDao
import com.nipunapps.mxclone.database.entity.LastPlaybackTimeEntity

class LastPlaybackHelper(
    private val lastPlaybackDao: LastPlaybackDao
) {

    suspend fun insertLastPlayback(mediaId : Long,bucketId : String){
        lastPlaybackDao.insert(
            LastPlaybackTimeEntity(
                mediaId = mediaId,
                lastPlaybackTime = System.currentTimeMillis(),
                bucketId = bucketId
            )
        )
    }

    suspend fun getAllLastPlayBack() : List<LastPlaybackTimeEntity> = lastPlaybackDao.getAllLastPlayback()

    suspend fun getLastPlaybackInsideBucket(bucketId: String) : List<LastPlaybackTimeEntity> = lastPlaybackDao.getLastPlayBackInsideBucket(bucketId)
}