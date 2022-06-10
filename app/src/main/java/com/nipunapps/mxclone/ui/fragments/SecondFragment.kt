package com.nipunapps.mxclone.ui.fragments

import android.app.Activity.RESULT_OK
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.transition.Slide
import android.transition.TransitionManager
import android.view.*
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.nipunapps.mxclone.R
import com.nipunapps.mxclone.database.entity.LastPlaybackTimeEntity
import com.nipunapps.mxclone.databinding.*
import com.nipunapps.mxclone.other.*
import com.nipunapps.mxclone.other.Constants.BUCKET_ID
import com.nipunapps.mxclone.other.Constants.FILE_ID
import com.nipunapps.mxclone.ui.activity.PlayerActivity
import com.nipunapps.mxclone.ui.adapters.FileItemAdapter
import com.nipunapps.mxclone.ui.adapters.FileItemClickListener
import com.nipunapps.mxclone.ui.adapters.GridSpacingDecoration
import com.nipunapps.mxclone.ui.models.FileModel
import com.nipunapps.mxclone.ui.viewmodels.MainViewModel

class SecondFragment : Fragment() {

    private var _binding: FragmentSecondBinding? = null
    private var _bottomActionBinding: BottomActionLayoutBinding? = null
    private val bottomActionBinding get() = _bottomActionBinding!!
    private lateinit var propertiesMultipleBinding: PropertiesMultipleBinding
    private var _moreBinding: MoreOptionBottomSheetBinding? = null
    private val moreBinding get() = _moreBinding!!
    private lateinit var bottomSheetDialog: BottomSheetDialog

    private var singleSelectedModel: FileModel? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var mainViewModel: MainViewModel
    private lateinit var fileItemAdapter: FileItemAdapter
    private var selectionMode: Boolean = false
    private var actionMode: ActionMode? = null
    private lateinit var renameLayoutBinding: RenameLayoutBinding
    private lateinit var singleFilePropertiesBinding: SingleFilePropertyBinding
    private lateinit var alertDialogue: AlertDialog
    private var count = 0
    private var totalItem = 0
    private var lastPlaybackItem: LastPlaybackTimeEntity? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentSecondBinding.inflate(inflater, container, false)
        _bottomActionBinding = binding.bottomAction
        _moreBinding = MoreOptionBottomSheetBinding.inflate(inflater, null, false)
        initBottomSheet()
        mainViewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]
        mainViewModel.getAllVideoInsideFolder()
        fileItemAdapter = FileItemAdapter(requireContext())
        binding.fileRv.adapter = fileItemAdapter
        binding.fileRv.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.fileRv.addItemDecoration(GridSpacingDecoration(2, 70, true))
        subscribeToObserver()
        binding.fileRv.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy < 0) {
                    binding.fab.isVisible = false
                }
                if (dy > 10) {
                    binding.fab.isVisible = true
                }
            }
        })
        binding.fab.setOnClickListener {
            lastPlaybackItem?.let { last ->
                val intent = Intent(requireContext(), PlayerActivity::class.java).apply {
                    putExtra(Constants.BUCKET_ID, last.bucketId)
                    putExtra(Constants.FILE_ID, last.mediaId)
                }
                startActivity(intent)
            }
        }
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fileItemAdapter.setFileItemClickListener(object : FileItemClickListener {
            override fun onItemClick(pos: Int, fileModel: FileModel) {
                if (selectionMode) {
                    mainViewModel.toggleSelection(pos)
                    return
                }
                val intent = Intent(requireContext(), PlayerActivity::class.java).apply {
                    putExtra(BUCKET_ID, mainViewModel.getBucketId())
                    putExtra(FILE_ID, fileModel.id)
                }
                requireContext().startActivity(intent)
            }

            override fun onItemLongClick(pos: Int): Boolean {
                selectionMode = true
                mainViewModel.toggleSelection(pos)
                return true
            }

            override fun onMoreIconClick(fileModel: FileModel) {
                moreBinding.selectedItemTitle.text = fileModel.title
                singleSelectedModel = fileModel
                bottomSheetDialog.show()
            }
        })
    }


//    --------------------------ViewModel Observer------------------------------

    private fun subscribeToObserver() {
        mainViewModel.fileList.observe(viewLifecycleOwner) { files ->
            totalItem = files.size
            count = files.count { f ->
                f.isSelected
            }
            if (count == 1) {
                singleSelectedModel = files.find { f ->
                    f.isSelected
                }
            }
            startActionMode()
            fileItemAdapter.setFile(files)
        }
        mainViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.fileRv.isVisible = !isLoading
            binding.progressCircular.isVisible = isLoading
        }
        mainViewModel.lastPlaybackInsideBucket.observe(viewLifecycleOwner) { lastPlaybacks ->
            if (lastPlaybacks.isNotEmpty()) {
                lastPlaybackItem = lastPlaybacks[0]
                fileItemAdapter.setLastPlaybacks(lastPlaybacks, lastPlaybacks[0].mediaId)
                binding.fab.isVisible = true
            }else{
                binding.fab.isVisible = false
            }
            if (lastPlaybacks.size > 1) {
                fileItemAdapter.setSecondLast(lastPlaybacks[1].mediaId)
            }
        }
    }


//    ----------------Show action mode--------------------

    private fun toggleBottomActionBar(show: Boolean, view: View) {
        val transition = Slide(Gravity.BOTTOM).apply {
            duration = 200
            addTarget(view)
        }
        TransitionManager.beginDelayedTransition(binding.root, transition)
        view.isVisible = show
    }

    private fun startActionMode() {
        selectionMode = count > 0
        toggleBottomActionBar(count > 0, bottomActionBinding.root)
        fileItemAdapter.hideShowMore(count > 0)
        binding.bottomAction.selectAllIcon.setImageResource(
            if (count == totalItem) R.drawable.ic_close else R.drawable.ic_check
        )
        binding.bottomAction.selectedItemText.text =
            requireContext().getString(R.string.selected_item_text, count, totalItem)
        bottomActionBinding.renameIcon.setImageResource(
            if (count > 1) R.drawable.ic_rename_disable else R.drawable.ic_rename
        )
        bottomActionBinding.shareIcon.setImageResource(
            if (count > 25) R.drawable.ic_share_disable else R.drawable.ic_share
        )


//        ---BottomAction click Listener------
        binding.bottomAction.selectAll.setOnClickListener {
            if (count != totalItem) mainViewModel.selectAllFile() else mainViewModel.clearSelection()
        }
        bottomActionBinding.rename.setOnClickListener {
            if (count != 1) return@setOnClickListener
            showRenameDialogue()
        }
        bottomActionBinding.delete.setOnClickListener {
            deleteFile()
        }

        bottomActionBinding.share.setOnClickListener {
            if (count > 25) {
                requireContext().showLongToast(requireContext().getString(R.string.error_send_more_25))
                return@setOnClickListener
            }
            mainViewModel.sendFiles()
        }
        bottomActionBinding.properties.setOnClickListener {
            if (count == 1) {
                singleSelectedModel?.let { file ->
                    singleItemProperties(file)
                }
                return@setOnClickListener
            }
            propertiesMultipleDialogue()
        }
    }

/*
    -------------------------Properties Dialogue-----------------------------------
    -------------------------------------------------------------------------------
 */

//    ---------------------------BottomSheet Dialog---------------------------------------

    private fun initBottomSheet() {
        bottomSheetDialog = BottomSheetDialog(requireContext())
        bottomSheetDialog.setContentView(moreBinding.root)
        moreBinding.rename.setOnClickListener {
            showRenameDialogue()
            bottomSheetDialog.dismiss()
        }
        moreBinding.delete.setOnClickListener {
            singleSelectedModel?.let { file ->
                deleteFile(listOf(file.getContentUri()))
            }
            bottomSheetDialog.dismiss()
        }
        moreBinding.share.setOnClickListener {
            singleSelectedModel?.let { file ->
                mainViewModel.sendSingleFile(file.getContentUri())
            }
            bottomSheetDialog.dismiss()
        }
        moreBinding.properties.setOnClickListener {
            singleSelectedModel?.let { file ->
                singleItemProperties(file)
            }
            bottomSheetDialog.dismiss()
        }
    }

//    --------------------------------Single Item properties dialog------------------

    private fun singleItemProperties(fileModel: FileModel) {
        singleFilePropertiesBinding = SingleFilePropertyBinding.inflate(layoutInflater)
        val alert = AlertDialog.Builder(requireContext())
        alert.setView(singleFilePropertiesBinding.root)
        alertDialogue = alert.create()
        singleFilePropertiesBinding.title.text = fileModel.title
        singleFilePropertiesBinding.fileName.text = fileModel.title
        singleFilePropertiesBinding.relativePath.text = fileModel.relativePath
        singleFilePropertiesBinding.dateTaken.text = fileModel.dateModified.getTimeInDate()
        singleFilePropertiesBinding.lastModified.text = fileModel.dateCreated.getTimeInDate()
        singleFilePropertiesBinding.size.text = requireContext().getString(
            R.string.selected_item_size,
            fileModel.size.getSizeInHigherByte(),
            "${fileModel.size} Bytes"
        )
        singleFilePropertiesBinding.resolution.text = fileModel.resolution
        singleFilePropertiesBinding.length.text = fileModel.duration.getDurationInFormat()
        singleFilePropertiesBinding.location.text =
            fileModel.path.dropLastWhile { it != '/' }.dropLast(1)
        singleFilePropertiesBinding.ok.setOnClickListener { alertDialogue.dismiss() }
        alertDialogue.show()
    }

    //    ----------------------------Multiple Item selected dialogue-----------------------
    private fun propertiesMultipleDialogue() {
        propertiesMultipleBinding = PropertiesMultipleBinding.inflate(layoutInflater)
        val alert = AlertDialog.Builder(requireContext())
        alert.setView(propertiesMultipleBinding.root)
        alertDialogue = alert.create()
        propertiesMultipleBinding.videoCount.text =
            requireContext().getString(R.string._100_video, count)
        propertiesMultipleBinding.size.text = requireContext().getString(
            R.string.selected_item_size,
            mainViewModel.getSelectedVideoSize().getSizeInHigherByte(),
            "${mainViewModel.getSelectedVideoSize()} Bytes"
        )
        propertiesMultipleBinding.ok.setOnClickListener {
            alertDialogue.dismiss()
        }
        alertDialogue.show()

    }

/*    ----------------------------Rename File ----------------------------------------
      --------------------------------------------------------------------------------
 */


    //    ---------- Rename Dialogue------------------
    private fun showRenameDialogue() {
        renameLayoutBinding = RenameLayoutBinding.inflate(layoutInflater)
        val alert = AlertDialog.Builder(requireContext())
        alert.setView(renameLayoutBinding.root)
        alertDialogue = alert.create()
        singleSelectedModel?.let { single ->
            renameLayoutBinding.newName.setText(single.title)
        }
        renameLayoutBinding.cancel.setOnClickListener { alertDialogue.dismiss() }
        renameLayoutBinding.yes.setOnClickListener {
            if (renameLayoutBinding.newName.text.toString().isBlank()) {
                Toast.makeText(
                    requireContext(),
                    requireContext().getString(R.string.name_empty),
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }
            mainViewModel.clearSelection()
            renameFile()
            alertDialogue.dismiss()
        }
        alertDialogue.show()
    }

    //    ------------------Rename Action----------------------
    private val renamePermission =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                singleSelectedModel?.let { single ->
                    val rename = renameLayoutBinding.newName.text.toString()
                    if (single.path.takeLastWhile { it != '.' } == "mkv") {
                        mainViewModel.renameVideo(
                            single.path, rename
                        )
                    } else {
                        mainViewModel.renameVideo(
                            single.getContentUri(),
                            rename
                        )
                    }
                }
            } else {
                Toast.makeText(
                    requireContext(),
                    requireContext().getString(R.string.error_rename),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }


    private fun renameFile() {
        singleSelectedModel?.let { single ->
            val newName = renameLayoutBinding.newName.text.toString()
            if (newName == single.title) return
            val intentSender =
                MediaStore.createWriteRequest(
                    requireContext().contentResolver,
                    listOf(single.getContentUri())
                ).intentSender
            renamePermission.launch(
                IntentSenderRequest.Builder(intentSender).build()
            )
        }
    }


    //    --------------------------------------Delete Action Start------------------------------------------

    private var deleteCount = 0
    private val deletePermission =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                requireContext().showShortToast(
                    requireContext().getString(
                        R.string.file_deleted,
                        deleteCount
                    )
                )
                mainViewModel.refresh()
            } else {
                requireContext().showLongToast(R.string.file_deleted_failed)
            }
        }

    private fun deleteFile(uris: List<Uri>? = null) {
        val intentSender = MediaStore.createDeleteRequest(
            requireContext().contentResolver,
            uris ?: mainViewModel.getAllSelectedFileUri()
        ).intentSender
        deleteCount = count
        deletePermission.launch(
            IntentSenderRequest.Builder(intentSender).build()
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}