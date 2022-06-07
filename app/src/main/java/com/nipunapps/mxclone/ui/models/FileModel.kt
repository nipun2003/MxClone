package com.nipunapps.mxclone.ui.models

import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore

data class FileModel(
    val path : String,
    val title : String,
    val duration : Long = 0L,
    val isSelected : Boolean = false,
    val id : Long = 0L,
    val size : Long = 0L,
    val dateCreated : Long,
    val dateModified : Long,
    val resolution : String,
    val relativePath : String
){
    fun getContentUri() : Uri{
        return ContentUris.withAppendedId(
            MediaStore.Video.Media.getContentUri("external"),
            id
        )
    }
}
