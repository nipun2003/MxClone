package com.nipunapps.mxclone.ui.fragments

import android.app.Activity.RESULT_OK
import android.app.AlertDialog
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.*
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.nipunapps.mxclone.R
import com.nipunapps.mxclone.databinding.BottomActionLayoutBinding
import com.nipunapps.mxclone.databinding.FragmentSecondBinding
import com.nipunapps.mxclone.databinding.PropertiesMultipleBinding
import com.nipunapps.mxclone.databinding.RenameLayoutBinding
import com.nipunapps.mxclone.other.getSizeInHigherByte
import com.nipunapps.mxclone.other.showLongToast
import com.nipunapps.mxclone.other.showShortToast
import com.nipunapps.mxclone.ui.adapters.FileItemAdapter
import com.nipunapps.mxclone.ui.adapters.GridSpacingDecoration
import com.nipunapps.mxclone.ui.models.FileModel
import com.nipunapps.mxclone.ui.viewmodels.MainViewModel

class SecondFragment : Fragment() {

    private var _binding: FragmentSecondBinding? = null
    private var _bottomActionBinding: BottomActionLayoutBinding? = null
    private val bottomActionBinding get() = _bottomActionBinding!!
    private lateinit var propertiesMultipleBinding: PropertiesMultipleBinding

    private var singleSelectedModel: FileModel? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var mainViewModel: MainViewModel
    private lateinit var fileItemAdapter: FileItemAdapter
    private var selectionMode: Boolean = false
    private var actionMode: ActionMode? = null
    private lateinit var renameLayoutBinding: RenameLayoutBinding
    private lateinit var alertDialogue: AlertDialog
    private var count = 0
    private var totalItem = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentSecondBinding.inflate(inflater, container, false)
        _bottomActionBinding = binding.bottomAction
        mainViewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]
        mainViewModel.getAllVideoInsideFolder()
        fileItemAdapter = FileItemAdapter(requireContext())
        binding.fileRv.adapter = fileItemAdapter
        binding.fileRv.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.fileRv.addItemDecoration(GridSpacingDecoration(2, 70, true))
        subscribeToObserver()
        bottomActionBinding.rename.setOnClickListener {
            if (count != 1) return@setOnClickListener
            showRenameDialogue()
        }
        bottomActionBinding.delete.setOnClickListener {
            deleteFile()
        }
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fileItemAdapter.setOnItemClickListener { pos, _ ->
            if (selectionMode) {
                mainViewModel.toggleSelection(pos)
            }
        }
        fileItemAdapter.setOnItemLongClickListener { pos ->
            selectionMode = true
            mainViewModel.toggleSelection(pos)
            true
        }
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
    }


//    ----------------Show action mode--------------------

    private fun startActionMode() {
        selectionMode = count > 0
        bottomActionBinding.root.isVisible = count > 0
        binding.bottomAction.selectAllIcon.setImageResource(
            if (count == totalItem) R.drawable.ic_close else R.drawable.ic_check
        )
        binding.bottomAction.selectedItemText.text =
            requireContext().getString(R.string.selected_item_text, count, totalItem)
        binding.bottomAction.selectAll.setOnClickListener {
            if (count != totalItem) mainViewModel.selectAllFile() else mainViewModel.clearSelection()
        }
        bottomActionBinding.renameIcon.setImageResource(
            if (count > 1) R.drawable.ic_rename_disable else R.drawable.ic_rename
        )
        bottomActionBinding.shareIcon.setImageResource(
            if (count > 25) R.drawable.ic_share_disable else R.drawable.ic_share
        )
        bottomActionBinding.share.setOnClickListener {
            if (count > 25) {
                requireContext().showLongToast(requireContext().getString(R.string.error_send_more_25))
                return@setOnClickListener
            }
            mainViewModel.sendFiles()
        }
        bottomActionBinding.properties.setOnClickListener {
            propertiesMultipleDialogue()
        }
    }

//    -------------------------Properties Dialogue-----------------------------------

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

    private fun deleteFile() {
        val intentSender = MediaStore.createDeleteRequest(
            requireContext().contentResolver,
            mainViewModel.getAllSelectedFileUri()
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