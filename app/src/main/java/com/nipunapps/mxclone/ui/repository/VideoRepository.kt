package com.nipunapps.mxclone.ui.repository

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import com.nipunapps.mxclone.other.Resource
import com.nipunapps.mxclone.ui.models.FileModel
import com.nipunapps.mxclone.ui.models.FolderModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File

class VideoRepository(
    private val context: Context
) {
    private val resolver = context.contentResolver

    fun renameFile(path: String, rename: String) {

        val file = File(path)
        val relativePath = path.dropLastWhile { it != '/' }
        val newFile = File(relativePath,rename+"."+path.takeLastWhile { it != '.' })
        file.renameTo(newFile)
    }
    fun renameFile(path : Uri,rename: String){
        val contentValues = ContentValues()
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, rename)
        resolver.update(path, contentValues, null)
    }

    fun getAllFolders(): List<FolderModel> {
        val folderUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Video.Media.BUCKET_ID,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.MediaColumns.VOLUME_NAME
        )
        val sortOrder = MediaStore.MediaColumns.BUCKET_DISPLAY_NAME + " ASC"
        val folderCursor = resolver.query(folderUri, projection, null, null, sortOrder)
        folderCursor?.let { cursor ->
            val folders = ArrayList<FolderModel>()
            while (cursor.moveToNext()) {
                val bucketId =
                    cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_ID))
                val bucketName =
                    cursor.getStringOrNull(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME))
                val volumeName = cursor.getStringOrNull(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.VOLUME_NAME))
                folders.withIndex()
                    .find { value ->
                        value.value.bucketId == bucketId
                    }?.let { folder ->
                        val copyValue = folder.value.copy(itemCount = folder.value.itemCount + 1)
                        folders.set(folder.index, copyValue)
                    } ?: folders.add(
                    FolderModel(
                        bucketId = bucketId,
                        bucketName = bucketName?:volumeName?:""
                    )
                )
            }
            return folders
        } ?: return emptyList()
    }

    fun getAllVideoInsideFolder(bucketId: String): Flow<Resource<List<FileModel>>> = flow {
        emit(Resource.loading(null))
        val videoUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.RESOLUTION,
            MediaStore.Video.Media.RELATIVE_PATH,
            MediaStore.Video.Media.DATE_MODIFIED
        )
        val selection = MediaStore.MediaColumns.BUCKET_ID + "=?"
        val selectionArg = arrayOf(bucketId)
        val sortOrder = MediaStore.Video.Media.DISPLAY_NAME + " ASC"
        val videoCursor = resolver.query(videoUri, projection, selection, selectionArg, sortOrder)
        videoCursor?.let { cursor ->
            val videos = ArrayList<FileModel>()
            while (cursor.moveToNext()) {
                val path =
                    cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA))
                videos.add(
                    FileModel(
                        path = path,
                        title = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)),
                        duration = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)),
                        id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)),
                        size = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)),
                        dateCreated = cursor.getLongOrNull(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED))?:0L,
                        dateModified = cursor.getLongOrNull(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED))?:0L,
                        relativePath = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.RELATIVE_PATH)),
                        resolution = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.RESOLUTION))
                    )
                )
            }
            emit(Resource.success(videos))
        } ?: emit(Resource.error("Something Went Wrong", emptyList()))
    }

    fun sendSingleFile(uri : Uri){
        val mimeType = "video/*"
        val intent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_SUBJECT, "here are some files")
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    fun sendMultipleFiles(uris : List<Uri>){
        val mimeType = "video/*"
        val intent : Intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            putExtra(Intent.EXTRA_SUBJECT,context.packageName)
            type = mimeType
            putParcelableArrayListExtra(Intent.EXTRA_STREAM,uris as ArrayList<Uri>)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}