package com.nipunapps.mxclone.ui.activity

import android.app.PictureInPictureParams
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.media.AudioManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.util.Rational
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.widget.AppCompatSeekBar
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.nipunapps.mxclone.R
import com.nipunapps.mxclone.databinding.ActivityPlayerBinding
import com.nipunapps.mxclone.other.Constants
import com.nipunapps.mxclone.other.Constants.BUCKET_ID
import com.nipunapps.mxclone.other.Constants.FILE_ID
import com.nipunapps.mxclone.ui.models.FileModel
import com.nipunapps.mxclone.ui.viewmodels.PlayerViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PlayerActivity : AppCompatActivity() {

    private var _binding: ActivityPlayerBinding? = null
    private val binding get() = _binding!!

    // player controller
    private lateinit var back: ImageView
    private lateinit var scaleType: ImageView
    private lateinit var more: ImageView
    private lateinit var title: TextView
    private lateinit var lock: ImageView
    private lateinit var pipMode: ImageView
    private lateinit var volumeSeek: AppCompatSeekBar


    private var exoPlayer: ExoPlayer? = null
    private val dataSourceFactory: DataSource.Factory by lazy {
        DefaultDataSourceFactory(this, "exo_player")
    }

    private var fromOffline = false

    private val playerViewModel: PlayerViewModel by viewModels()
    private var position = 0
    private var bucketId: String? = null
    private var filePlaying: FileModel? = null
    private var scale = 0
    private var fileId: Long? = null
    private var mediaFiles = emptyList<FileModel>()
    private var lockMode = false
    private lateinit var audioManager: AudioManager
    private var currentVolume = 0

    override fun onBackPressed() {
        if (lockMode) {
            binding.lockSurface.performClick()
        } else super.onBackPressed()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityPlayerBinding.inflate(layoutInflater)
        initialiseControllerView()
        initialiseMediaSeek()
        setContentView(binding.root)
        val uri = intent.data
        uri?.let { u ->
            playerViewModel.initialiseFiles(isOffline = true, uri = u)
            fromOffline = true
            return
        }
        fileId = intent.extras?.getLong(FILE_ID)
        bucketId = intent.extras?.getString(BUCKET_ID)
        bucketId?.let { bId ->
            playerViewModel.initialiseFiles(bId)
            fileId?.let { id ->
                playerViewModel.setPositionWithMediaId(id)
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { newIntent ->
            finish()
            startActivity(newIntent)
        }
    }

    private fun initialiseControllerView() {
        back = binding.player.findViewById(R.id.back)
        title = binding.player.findViewById(R.id.title)
        more = binding.player.findViewById(R.id.more)
        scaleType = binding.player.findViewById(R.id.scaleType)
        lock = binding.player.findViewById(R.id.lock_controller)
        pipMode = binding.player.findViewById(R.id.pipMode)
        pipMode.setOnClickListener {
            goIntoPipMode()
        }
        back.setOnClickListener { finish() }
        lock.setOnClickListener {
            binding.player.hideController()
            lockMode = true
            binding.lockSurface.isVisible = true
            hideOpenLock()
        }
        binding.openLock.setOnClickListener {
            binding.lockSurface.isVisible = false
            lockMode = false
            binding.player.showController()
            job?.cancel()
        }
        scaleType.setOnClickListener {
            playerViewModel.setScaleType(
                if (scale == 4) 0 else scale + 1
            )
        }
        binding.lockSurface.setOnClickListener {
            binding.openLock.isVisible = true
            hideOpenLock()
        }
        AspectRatioFrameLayout.RESIZE_MODE_FILL
    }

    private fun initialiseMediaSeek() {
        volumeSeek = binding.player.findViewById(R.id.volume_seek)
        audioManager = getSystemService(AudioManager::class.java)
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        volumeSeek.max = maxVolume + 1
        currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        volumeSeek.progress = currentVolume
        volumeSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, p1, 0)
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {

            }

            override fun onStopTrackingTouch(p0: SeekBar?) {

            }
        })
    }

    private var job: Job? = null

    private fun hideOpenLock() {
        job?.cancel()
        job = lifecycleScope.launch {
            delay(3000)
            binding.openLock.isVisible = false
        }
    }

    override fun onStart() {
        super.onStart()
        initialisePlayer()
        subscribeToObserver()
    }

    private var currentPlaybackPosition = 0L

    private fun initialisePlayer() {
        exoPlayer = ExoPlayer.Builder(this)
            .build()
        binding.player.player = exoPlayer
    }

    private fun subscribeToObserver() {
        playerViewModel.fileList.observe(this) { files ->
            mediaFiles = files
            exoPlayer?.let { player ->
                files.forEachIndexed { index, file ->
                    fileId?.let { id ->
                        if (id == file.id) {
                            position = index
                        }
                    }
                    val metaData = MediaMetadata.Builder()
                        .setTitle(file.title)
                        .build()
                    val mediaItem = MediaItem.Builder()
                        .setUri(Uri.parse(file.path))
                        .setMediaId(file.id.toString())
                        .setMediaMetadata(metaData)
                        .build()
                    player.addMediaSource(
                        ProgressiveMediaSource.Factory(dataSourceFactory)
                            .createMediaSource(mediaItem)
                    )
                }
                player.addListener(object : Player.Listener {
                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        super.onMediaItemTransition(mediaItem, reason)
                        title.text = mediaItem?.mediaMetadata?.title ?: ""
                        mediaItem?.mediaId?.let { id ->
                            if (!fromOffline)
                                playerViewModel.setLastPlayback(id.toLong())
                        }
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        super.onPlaybackStateChanged(playbackState)
                        if (playbackState == ExoPlayer.STATE_ENDED) {
                            finish()
                        }
                    }
                })
            }
        }
        playerViewModel.isStart.observe(this) { isStart ->
            if (isStart) {
                playerViewModel.setPosition(position, currentPlaybackPosition)
                playerViewModel.setIsStart()
            }
        }
        playerViewModel.position.observe(this) { pos ->
            if (pos == -1) return@observe
            filePlaying = playerViewModel.getCurrentFilePlaying(pos)
            filePlaying?.let { file ->
                val width = file.resolution.takeWhile { it.isDigit() }.toInt()
                val height = file.resolution.takeLastWhile { it.isDigit() }.toInt()
                playerViewModel.updatePlaybackPosition(file.id)
                title.text = file.title
                requestedOrientation = if (width > height) {
                    ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                } else {
                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                }
            }
            playerViewModel.playbackPosition.observe(this) { playback ->
                exoPlayer?.prepare()
                exoPlayer?.seekTo(pos, playback)
            }
            playerViewModel.scaleType.observe(this) { scale ->
                this.scale = scale
                binding.player.resizeMode = scale
                Constants.scaleMap.get(scale)?.let { res ->
                    scaleType.setImageResource(
                        res
                    )
                }
            }
        }
        playerViewModel.playWhenReady.observe(this) {
            exoPlayer?.playWhenReady = it
        }
    }

    private fun releasePlayer() {
        exoPlayer?.let { player ->
            position = player.currentPeriodIndex
            currentPlaybackPosition = player.currentPosition
            playerViewModel.setPosition(position, currentPlaybackPosition)
            setLastPlayDuration(position, currentPlaybackPosition)
            player.release()
        }
    }

    private fun setLastPlayDuration(pos: Int, duration: Long) {
        playerViewModel.insertLastDuration(file = mediaFiles[pos], lastDuration = duration)
    }

//    --------------------------LifeCycle Observer--------------------

    override fun onPause() {
        super.onPause()
        if (!isInPictureInPictureMode)
            exoPlayer?.playWhenReady = false
    }

    override fun onUserLeaveHint() {
        goIntoPipMode()
    }

    private fun goIntoPipMode(){
        exoPlayer?.let {
            releasePlayer()
            binding.player.useController = false
            val aspectRation = Rational(binding.player.width, binding.player.height)
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(aspectRation)
                .build()
            enterPictureInPictureMode(params)
            binding.player.hideController()
        }
    }

    override fun onResume() {
        super.onResume()
        exoPlayer?.playWhenReady = true
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration?
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        if(isInPictureInPictureMode){
            binding.player.hideController()
        }else binding.player.showController()
        binding.player.useController = !isInPictureInPictureMode
    }

    override fun onStop() {
        super.onStop()
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, 0)
        releasePlayer()
    }

    override fun onDestroy() {
        super.onDestroy()
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, 0)
        releasePlayer()
    }
}