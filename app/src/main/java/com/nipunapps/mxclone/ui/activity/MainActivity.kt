package com.nipunapps.mxclone.ui.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.nipunapps.mxclone.R
import com.nipunapps.mxclone.databinding.ActivityMainBinding
import com.nipunapps.mxclone.databinding.PermissionPermanentDenyBinding
import com.nipunapps.mxclone.databinding.RationalLayoutBinding
import com.nipunapps.mxclone.other.Constants
import com.nipunapps.mxclone.ui.repository.VideoRepository
import com.nipunapps.mxclone.ui.viewmodels.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val mainViewModel: MainViewModel by viewModels()

    private var _binding: ActivityMainBinding? = null
    private val binding: ActivityMainBinding get() = _binding!!

    private var _rationBinding: RationalLayoutBinding? = null
    private val rationalBinding: RationalLayoutBinding get() = _rationBinding!!

    private lateinit var mAppBarConfiguration: AppBarConfiguration

    private var _permissionPermanentBinding: PermissionPermanentDenyBinding? = null
    private val permissionPermanentDenyBinding: PermissionPermanentDenyBinding get() = _permissionPermanentBinding!!

    private var selectionMode = false
    override fun onBackPressed() {
        if (selectionMode) {
            mainViewModel.clearSelection()
        } else super.onBackPressed()
    }

    private val storagePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                handlePermissionGranted()
            } else {
                if (shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    handleRational()
                } else {
                    handlePermanentDeny()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        _binding = ActivityMainBinding.inflate(layoutInflater)
        _rationBinding = binding.rationalLayout
        _permissionPermanentBinding = binding.permissionPermanentDeny
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = getString(R.string.folder)
        checkPermission()
    }


    private fun checkPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED -> {
                handlePermissionGranted()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE) -> {
                handleRational()
            }
            else -> {
                handlePermanentDeny()
                storagePermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }

    private fun handlePermanentDeny() {
        binding.contentMain.mainContainer.isVisible = false
        rationalBinding.root.isVisible = false
        permissionPermanentDenyBinding.root.isVisible = true
        permissionPermanentDenyBinding.openSettings.setOnClickListener {
            with(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)) {
                data = Uri.parse("package:${this@MainActivity.packageName}")
                startActivity(this)
            }
        }
    }

    private fun handlePermissionGranted() {
        binding.contentMain.mainContainer.isVisible = true
        rationalBinding.root.isVisible = false
        permissionPermanentDenyBinding.root.isVisible = false
        if (!selectionMode)
            mainViewModel.refresh()
        subscribeToObserver()
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        mAppBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, mAppBarConfiguration)
        binding.fab.setOnClickListener {
            mainViewModel.getLastPlayMedia()?.let { last ->
                val intent = Intent(this, PlayerActivity::class.java).apply {
                    putExtra(Constants.BUCKET_ID, last.bucketId)
                    putExtra(Constants.FILE_ID, last.mediaId)
                }
                startActivity(intent)
            }
        }
    }

    private fun subscribeToObserver() {
        mainViewModel.fileList.observe(this) { f ->
            selectionMode = (f.count { it.isSelected }) > 0
        }
    }

    private fun handleRational() {
        rationalBinding.root.isVisible = true
        binding.contentMain.mainContainer.isVisible = false
        permissionPermanentDenyBinding.root.isVisible = false
        rationalBinding.enableStoragePermission.setOnClickListener {
            storagePermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    override fun onResume() {
        super.onResume()
        checkPermission()
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(mAppBarConfiguration)
                || super.onSupportNavigateUp()
    }
}