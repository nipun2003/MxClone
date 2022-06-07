package com.nipunapps.mxclone.ui.viewmodels

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    private val videoRepository: VideoRepository
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

    fun getFolderList() {
        viewModelScope.launch {
            _folderList.postValue(videoRepository.getAllFolders())
        }
    }

    fun setBucketId(bucketId: String) {
        this.bucketId = bucketId
    }

    fun getAllVideoInsideFolder() {
        Log.e("Nipun", "Inside viewmodel method")
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

}