package com.nipunapps.mxclone.ui.adapters

import com.nipunapps.mxclone.ui.models.FileModel

interface FileItemClickListener {

    fun onItemClick(pos : Int,fileModel: FileModel)
    fun onItemLongClick(pos : Int) : Boolean
    fun onMoreIconClick(fileModel: FileModel)
}