package com.nipunapps.mxclone.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.ActionMode
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.nipunapps.mxclone.R
import com.nipunapps.mxclone.databinding.FragmentHomeBinding
import com.nipunapps.mxclone.other.Constants
import com.nipunapps.mxclone.ui.activity.PlayerActivity
import com.nipunapps.mxclone.ui.adapters.FolderItemAdapter
import com.nipunapps.mxclone.ui.viewmodels.MainViewModel

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private lateinit var mainViewModel: MainViewModel
    private lateinit var folderItemAdapter: FolderItemAdapter
    private var actionMode : ActionMode? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        mainViewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]
        folderItemAdapter = FolderItemAdapter(requireContext())
        subscribeToObserver()
        binding.folderRv.adapter = folderItemAdapter
        binding.folderRv.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.fab.setOnClickListener {
            mainViewModel.getLastPlayMedia()?.let { last ->
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
        folderItemAdapter.setOnItemClickListener { folder ->
            mainViewModel.setBucketId(folder.bucketId)
            val bundle = Bundle().apply { putString("title", folder.bucketName) }
            view.animation = AnimationUtils.loadAnimation(requireContext(), R.anim.from_right_exit)
            findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment, bundle)
        }
    }

    private fun subscribeToObserver() {
        mainViewModel.folderList.observe(viewLifecycleOwner) { folders ->
            folderItemAdapter.setFolder(folders)
        }

        mainViewModel.lastPlayback.observe(viewLifecycleOwner){lastPlayback->
            if(lastPlayback.isNotEmpty()){
                folderItemAdapter.setLastPlayFolder(lastPlayback[0].bucketId)
                binding.fab.isVisible = true
            }else{
                binding.fab.isVisible = false
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}