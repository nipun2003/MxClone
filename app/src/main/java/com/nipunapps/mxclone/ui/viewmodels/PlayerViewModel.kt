package com.nipunapps.mxclone.ui.viewmodels

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.exoplayer2.MediaItem
import com.nipunapps.mxclone.other.Status
import com.nipunapps.mxclone.ui.models.FileModel
import com.nipunapps.mxclone.ui.repository.VideoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val videoRepository: VideoRepository
) : ViewModel() {

    private val _isStart = MutableLiveData<Boolean>(true)
    val isStart: LiveData<Boolean> = _isStart

    private val _playWhenReady = MutableLiveData<Boolean>(true)
    val playWhenReady: LiveData<Boolean> = _playWhenReady


    private var bucketId: String = ""
    private val _position = MutableLiveData<Int>(0)
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