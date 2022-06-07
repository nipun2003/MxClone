package com.nipunapps.mxclone.ui.adapters

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.nipunapps.mxclone.databinding.FileItemBinding
import com.nipunapps.mxclone.other.getDurationInFormat
import com.nipunapps.mxclone.ui.models.FileModel
import java.io.File

class FileItemAdapter(
    private val context: Context
) : RecyclerView.Adapter<FileItemAdapter.ViewHolder>() {

    private var files = listOf<FileModel>()
    private var fileItemClickListener: FileItemClickListener? = null

    private var showMore = true

    fun hideShowMore(hideMore : Boolean){
        this.showMore = !hideMore
        notifyDataSetChanged()
    }

    fun setFileItemClickListener(fileItemClickListener: FileItemClickListener) {
        this.fileItemClickListener = fileItemClickListener
    }


    fun setFile(files: List<FileModel>) {
        this.files = files
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(FileItemBinding.inflate(LayoutInflater.from(context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val file = files[position]
        with(holder) {
            binding.duration.text = file.duration.getDurationInFormat()
            binding.title.text = file.title
            binding.more.isVisible = showMore
            Glide.with(context).asBitmap().load(Uri.fromFile(File(file.path)))
                .into(binding.videoThumbnail)
            binding.selected.isVisible = file.isSelected
            binding.root.setOnClickListener {
                fileItemClickListener?.onItemClick(position, file)
            }
            binding.root.setOnLongClickListener {
                fileItemClickListener?.onItemLongClick(position) ?: false
            }
            binding.more.setOnClickListener {
                fileItemClickListener?.onMoreIconClick(file)
            }
        }
    }

    override fun getItemCount(): Int {
        return files.size
    }

    class ViewHolder(val binding: FileItemBinding) : RecyclerView.ViewHolder(binding.root)
}