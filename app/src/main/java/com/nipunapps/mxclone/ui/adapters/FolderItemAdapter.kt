package com.nipunapps.mxclone.ui.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nipunapps.mxclone.R
import com.nipunapps.mxclone.databinding.FolderItemBinding
import com.nipunapps.mxclone.ui.models.FolderModel

class FolderItemAdapter(
    private val context: Context
) : RecyclerView.Adapter<FolderItemAdapter.ViewHolder>() {

    private var folders = listOf<FolderModel>()
    private var onItemClickListener: ((FolderModel) -> Unit)? = null

    fun setOnItemClickListener(onItemClickListener: (FolderModel) -> Unit) {
        this.onItemClickListener = onItemClickListener
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(FolderItemBinding.inflate(LayoutInflater.from(context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val folder = folders[position]
        with(holder) {
            folderItemBinding.folderName.text = folder.bucketName
            folderItemBinding.folderCount.text =
                context.getString(R.string._100_video, folder.itemCount)
            folderItemBinding.root.setOnClickListener { v ->
                onItemClickListener?.let { click ->
                    click(folder)
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return folders.size
    }

    fun setFolder(folders: List<FolderModel>) {
        this.folders = folders
        notifyDataSetChanged()
    }

    class ViewHolder(val folderItemBinding: FolderItemBinding) :
        RecyclerView.ViewHolder(folderItemBinding.root)
}