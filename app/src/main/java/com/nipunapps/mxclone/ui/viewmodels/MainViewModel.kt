package com.nipunapps.mxclone.ui.viewmodels

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nipunapps.mxclone.database.entity.LastPlaybackTimeEntity
import com.nipunapps.mxclone.database.helper.LastPlaybackHelper
import com.nipunapps.mxclone.other.Status
import com.nipunapps.mxclone.ui.models.FileModel
import com.nipunapps.mxclone.ui.models.FolderModel
import com.nipunapps.mxclone.ui.repository.VideoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val videoRepository: VideoRepository,
    private val lastPlaybackHelper: LastPlaybackHelper
) : ViewModel() {

    private var bucketId: String = ""

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _isError = MutableLiveData<Boolean>(false)
    private val isError: LiveData<Boolean> = _isError


    private val _folderList = MutableLiveData<List<FolderModel>>(emptyList())
    val folderList: LiveData<List<FolderModel>> = _folderList

    private val _fileList = MutableLiveData<List<FileModel>>(emptyList())
    val fileList: LiveData<List<FileModel>> = _fileList

    private val _lastPlaybackInsideBucket = MutableLiveData<List<LastPlaybackTimeEntity>>(emptyList())
    val lastPlaybackInsideBucket: LiveData<List<LastPlaybackTimeEntity>> = _lastPlaybackInsideBucket

    private val _lastPlayback = MutableLiveData<List<LastPlaybackTimeEntity>>(emptyList())
    val lastPlayback: LiveData<List<LastPlaybackTimeEntity>> = _lastPlayback

    fun refresh() {
        getFolderList()
        getAllVideoInsideFolder()
    }

    fun renameVideo(path: String, newName: String) {
        videoRepository.renameFile(path, newName)
    }

    fun renameVideo(path: Uri, newName: String) {
        videoRepository.renameFile(path, newName)
    }

    fun getSelectedVideoSize(): Long {
        var size = 0L
        fileList.value?.let { files ->
            files.filter { it.isSelected }.forEach { size += it.size }
        }
        return size
    }

    fun clearSelection() {
        _fileList.postValue(
            fileList.value?.map { f ->
                f.copy(isSelected = false)
            }
        )
    }

    fun selectAllFile() {
        _fileList.postValue(
            fileList.value?.map { f ->
                f.copy(isSelected = true)
            }
        )
    }

    fun sendFiles() {
        fileList.value?.let { files ->
            videoRepository.sendMultipleFiles(files.filter { it.isSelected }
                .map { it.getContentUri() })
        }
    }

    fun sendSingleFile(uri: Uri) {
        videoRepository.sendSingleFile(uri)
    }

    fun getAllSelectedFileUri(): List<Uri> =
        fileList.value?.let { files ->
            files.filter { it.isSelected }.map { it.getContentUri() }
        } ?: emptyList()


    fun toggleSelection(pos: Int) {
        _fileList.postValue(
            fileList.value?.mapIndexed { i, f ->
                if (i == pos) {
                    f.copy(isSelected = !f.isSelected)
                } else f
            }
        )
    }

    private fun getFolderList() {
        viewModelScope.launch {
            _lastPlayback.postValue(lastPlaybackHelper.getAllLastPlayBack())
            _folderList.postValue(videoRepository.getAllFolders())
        }
    }

    fun getLastPlayMedia() : LastPlaybackTimeEntity?{
        lastPlayback.value?.let { f->
            if(f.isNotEmpty()) return f[0]
            else return null
        }
        return null
    }

    fun setBucketId(bucketId: String) {
        this.bucketId = bucketId
    }

    fun getBucketId(): String = bucketId

    fun getAllVideoInsideFolder() {
        viewModelScope.launch {
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
            }.launchIn(this)
            _lastPlaybackInsideBucket.postValue(lastPlaybackHelper.getLastPlaybackInsideBucket(bucketId = bucketId))
        }
    }

}