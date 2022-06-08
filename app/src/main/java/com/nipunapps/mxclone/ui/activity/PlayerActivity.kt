package com.nipunapps.mxclone.ui.activity

import android.content.pm.ActivityInfo
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.nipunapps.mxclone.R
import com.nipunapps.mxclone.databinding.ActivityPlayerBinding
import com.nipunapps.mxclone.databinding.CustomPlayerControlBinding
import com.nipunapps.mxclone.other.Constants
import com.nipunapps.mxclone.other.Constants.BUCKET_ID
import com.nipunapps.mxclone.other.Constants.POSITION
import com.nipunapps.mxclone.ui.models.FileModel
import com.nipunapps.mxclone.ui.viewmodels.PlayerViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PlayerActivity : AppCompatActivity() {

    private var _binding: ActivityPlayerBinding? = null
    private val binding get() = _binding!!

    // Controller View
    private lateinit var back: ImageView
    private lateinit var more: ImageView
    private lateinit var scaleType: ImageView
    private lateinit var title: TextView


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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityPlayerBinding.inflate(layoutInflater)
        initialiseControllerView()
        setContentView(binding.root)
        val uri = intent.data
        uri?.let { u ->
            playerViewModel.initialiseFiles(isOffline = true, uri = u)
            fromOffline = true
            return
        }
        position = intent.extras?.getInt(POSITION) ?: 0
        bucketId = intent.extras?.getString(BUCKET_ID)
        bucketId?.let { bId ->
            playerViewModel.initialiseFiles(bId)
        }
    }

    private fun initialiseControllerView() {
        back = binding.player.findViewById(R.id.back)
        title = binding.player.findViewById(R.id.title)
        more = binding.player.findViewById(R.id.more)
        scaleType = binding.player.findViewById(R.id.scaleType)
        back.setOnClickListener { finish() }
        scaleType.setOnClickListener {
            playerViewModel.setScaleType(
                if (scale == 4) 0 else scale + 1
            )
        }
        AspectRatioFrameLayout.RESIZE_MODE_FILL
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
            exoPlayer?.let { player ->
                files.forEach { file ->
                    val metaData = MediaMetadata.Builder()
                        .setTitle(file.title)
                        .build()
                    val mediaItem = MediaItem.Builder()
                        .setUri(Uri.parse(file.path))
                        .setMediaMetadata(metaData)
                        .build()
                    player.addMediaSource(
                        ProgressiveMediaSource.Factory(dataSourceFactory)
                            .createMediaSource(mediaItem)
                    )
                }
                player.prepare()
                player.addListener(object : Player.Listener {
                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        super.onMediaItemTransition(mediaItem, reason)
                        title.text = mediaItem?.mediaMetadata?.title ?: ""
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        super.onPlaybackStateChanged(playbackState)
                        if(playbackState == ExoPlayer.STATE_ENDED){
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
            filePlaying = playerViewModel.getCurrentFilePlaying(pos)
            filePlaying?.let { file ->
                val width = file.resolution.takeWhile { it.isDigit() }.toInt()
                val height = file.resolution.takeLastWhile { it.isDigit() }.toInt()
                title.text = file.title
                requestedOrientation = if (width > height) {
                    ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                } else {
                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                }
            }
            playerViewModel.playbackPosition.observe(this) { playback ->
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
            player.release()
        }
    }

//    --------------------------LifeCycle Observer--------------------

    override fun onPause() {
        super.onPause()
        exoPlayer?.pause()
    }

    override fun onResume() {
        super.onResume()
        exoPlayer?.play()
    }

    override fun onStop() {
        super.onStop()
        releasePlayer()
    }

    override fun onDestroy() {
        super.onDestroy()
        releasePlayer()
    }
}