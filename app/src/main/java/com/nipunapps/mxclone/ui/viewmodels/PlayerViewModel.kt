package com.nipunapps.mxclone.ui.viewmodels

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.exoplayer2.MediaItem
import com.nipunapps.mxclone.database.dao.DurationDao
import com.nipunapps.mxclone.database.entity.LastPlayDurationEntity
import com.nipunapps.mxclone.database.helper.LastPlaybackHelper
import com.nipunapps.mxclone.other.Status
import com.nipunapps.mxclone.ui.models.FileModel
import com.nipunapps.mxclone.ui.repository.VideoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val videoRepository: VideoRepository,
    private val lastPlaybackHelper: LastPlaybackHelper,
    private val durationDao: DurationDao
) : ViewModel() {

    private val _isStart = MutableLiveData<Boolean>(true)
    val isStart: LiveData<Boolean> = _isStart

    private val _playWhenReady = MutableLiveData<Boolean>(true)
    val playWhenReady: LiveData<Boolean> = _playWhenReady


    private var bucketId: String = ""
    private val _position = MutableLiveData<Int>(-1)
    val position: LiveData<Int> = _position

    private val _scaleType = MutableLiveData<Int>(0)
    val scaleType: LiveData<Int> = _scaleType

    private val _playBackPosition = MutableLiveData<Long>(0L)
    val playbackPosition: LiveData<Long> = _playBackPosition

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _isError = MutableLiveData<Boolean>(false)
    private val isError: LiveData<Boolean> = _isError

    private val _fileList = MutableLiveData<List<FileModel>>(emptyList())
    val fileList: LiveData<List<FileModel>> = _fileList

    fun getCurrentFilePlaying(pos: Int): FileModel? {
        fileList.value?.let { files ->
            if (files.size <= pos) return null
            return files[pos]
        } ?: return null
    }

    fun setLastPlayback(mediaId: Long) {
        viewModelScope.launch {
            lastPlaybackHelper.insertLastPlayback(
                mediaId = mediaId,
                bucketId = bucketId
            )
        }
    }

    fun initialiseFiles(bucketId: String = "", isOffline: Boolean = false, uri: Uri? = null) {
        if (isOffline) {
            _fileList.postValue(uri?.let {
                videoRepository.getFileFromUri(it).filterIndexed { i, _ ->
                    i < 1
                }
            })
            return
        }
        this.bucketId = bucketId
        videoRepository.getAllVideoInsideFolder(bucketId = bucketId).onEach { result ->
            when (result.status) {
                Status.SUCCESS -> {
                    _isLoading.postValue(false)
                    _isError.postValue(false)
                    _fileList.postValue(result.data ?: emptyList())
                }
                Status.LOADING -> {
                    _isLoading.postValue(false)
                    _isError.postValue(false)
                    _fileList.postValue(emptyList())
                }
                Status.ERROR -> {
                    _isError.postValue(true)
                    _isLoading.postValue(false)
                }
            }
        }.launchIn(viewModelScope)
    }

    fun setPosition(pos: Int) {
        viewModelScope.launch {
            getCurrentFilePlaying(pos)?.let { file ->
                updatePlaybackPosition(file.id)
            }
        }
    }

    fun setPositionWithMediaId(id: Long) {
        viewModelScope.launch {
            lastPlaybackHelper.insertLastPlayback(
                mediaId = id,
                bucketId = bucketId
            )
        }
        fileList.value?.let { files ->
            files.forEachIndexed { i, f ->
                if (id == f.id) {
                    _position.postValue(i)
                }
            }
        }
    }

    fun insertLastDuration(
        file: FileModel,
        lastDuration: Long
    ) {
        viewModelScope.launch {
            durationDao.insertLastDuration(
                LastPlayDurationEntity(
                    file.id,
                    if(file.duration - lastDuration <= 10000) 0L else lastDuration
                )
            )
        }
    }

    fun updatePlaybackPosition(mediaId: Long) {
        viewModelScope.launch {
            durationDao.getLastPlayDuration(mediaId)?.let { last ->
                _playBackPosition.postValue(last.lastDuration)
            }
        }
    }

    fun setPosition(pos: Int, playback: Long) {
        _position.postValue(pos)
        _playBackPosition.postValue(playback)
    }

    fun setPlayWhenReady(playWhenReady: Boolean) {
        _playWhenReady.postValue(playWhenReady)
    }

    fun setScaleType(int: Int) {
        _scaleType.value = int
    }

    fun setIsStart() {
        _isStart.postValue(false)
    }

}