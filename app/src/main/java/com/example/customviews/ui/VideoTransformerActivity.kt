 package com.example.customviews.ui

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.util.GlUtil
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.DrawableOverlay
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.OverlaySettings
import androidx.media3.effect.TextureOverlay
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import com.bumptech.glide.Glide
import com.example.customviews.R
import com.example.customviews.databinding.ActivityVideoTransformerBinding
import com.example.customviews.ui.adapter.LutItemAdapter
import com.example.customviews.utils.getThemeColor
import com.google.common.collect.ImmutableList
import com.sqsong.opengllib.effect.BitmapColorLut
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

 @UnstableApi
class VideoTransformerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVideoTransformerBinding

    private var videoUri: Uri? = null
    private var exoPlayer: ExoPlayer? = null
    private var bitmapColorLut: BitmapColorLut? = null

    private val lutItemAdapter by lazy {
        LutItemAdapter { path, _ ->
            Log.d("LUTFilterActivity", "LUT item clicked: $path")
            videoUri?.let { loadLutBitmap(it, path) }
        }
    }

    private fun loadLutFiles() {
        flow<List<String>> {
            assets.list("lut")?.let {
                emit(it.toList())
            }
        }.flowOn(Dispatchers.IO)
            .onEach {
                Log.d("LUTFilterActivity", "LUT list: $it")
                lutItemAdapter.submitList(it)
            }.launchIn(lifecycleScope)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityVideoTransformerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initLayout()
        loadLutFiles()
    }

    @OptIn(UnstableApi::class)
    private fun initLayout() {
        val videoUri = intent?.getParcelableExtra<Uri>("videoUri") ?: run {
            finish()
            return
        }
        this.videoUri = videoUri
        val colorSurface = getThemeColor(this, com.google.android.material.R.attr.colorSurface)
        val red: Float = (colorSurface shr 16 and 0xFF) / 255.0f
        val green: Float = (colorSurface shr 8 and 0xFF) / 255.0f
        val blue: Float = (colorSurface and 0xFF) / 255.0f
        GlUtil.setClearColor(red, green, blue, 1.0f)

        binding.recyclerView.adapter = lutItemAdapter
        binding.transformBtn.setOnClickListener { startTransform(videoUri) }

        binding.seekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                bitmapColorLut?.setIntensity(progress / 100f)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {

            }
        })
        setupPlayerView(videoUri)
    }

    @OptIn(UnstableApi::class) private fun loadLutBitmap(videoUri: Uri, path: String) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                Glide.with(this@VideoTransformerActivity).asBitmap().load("file:///android_asset/lut/$path").submit().get()
            }?.let { bitmap ->
                setupPlayerView(videoUri, BitmapColorLut(bitmap).apply { bitmapColorLut = this })
            }
        }
    }

    @OptIn(UnstableApi::class)
    private fun setupPlayerView(videoUri: Uri, effect: Effect? = null) {
        exoPlayer?.release()
        exoPlayer = null

        val mediaItem = MediaItem.fromUri(videoUri)
        val videoPlayer = ExoPlayer.Builder(this)
            .build()
        videoPlayer.addMediaItem(mediaItem)
        videoPlayer.playWhenReady = true
        effect?.let {
            videoPlayer.setVideoEffects(listOf(createOverlayEffect(), effect))
        }
        videoPlayer.prepare()
        binding.playerView.player = videoPlayer
        exoPlayer = videoPlayer
    }

    @OptIn(UnstableApi::class)
    private fun startTransform(videoUri: Uri) {
        val mediaItem = MediaItem.Builder()
            .setUri(videoUri)
            .setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(366603L)
                    .setEndPositionMs(1019532L)
                    .build()
            )
            .build()
        val editMediaItem = EditedMediaItem.Builder(mediaItem).build()
        val transformer = Transformer.Builder(this)
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    super.onCompleted(composition, exportResult)
                    binding.progressIndicator.setProgress(100, true)
                    Toast.makeText(this@VideoTransformerActivity, "Transform completed", Toast.LENGTH_SHORT).show()
                }

                override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) {
                    super.onError(composition, exportResult, exportException)
                    Toast.makeText(this@VideoTransformerActivity, "Transform failed", Toast.LENGTH_SHORT).show()
                    exportException.printStackTrace()
                }
            })
            .setDebugViewProvider { width, height ->
                return@setDebugViewProvider binding.surfaceView
            }
            .build()
        val outputDir = File(getExternalFilesDir(null)?.absolutePath + "/output")
        if (!outputDir.exists()) outputDir.mkdirs()
        transformer.start(editMediaItem, File(outputDir, "transformed.mp4").absolutePath)

        val progressHolder = ProgressHolder()
        binding.root.post(object : Runnable {
            override fun run() {
                val progressState = transformer.getProgress(progressHolder)
                val progress = progressHolder.progress
                Log.w("VideoTransformer", "Progress: $progress")
                binding.progressIndicator.setProgress(progress, true)
                if (progressState != Transformer.PROGRESS_STATE_NOT_STARTED) {
                    binding.root.postDelayed(this, 500)
                }
            }
        })
    }

    @UnstableApi
    fun createOverlayEffect(): Effect {
        val logoSettings = OverlaySettings.Builder()
            .setOverlayFrameAnchor(1f, 1f)
            .setBackgroundFrameAnchor(0.80f, -0.90f)
            .setAlphaScale(0.8f)
            .setScale(0.5f, 0.5f)
            .build()
        val logoDrawable = packageManager.getApplicationIcon(packageName)
        logoDrawable.setBounds(0, 0, logoDrawable.intrinsicWidth, logoDrawable.intrinsicHeight)
        val logoOverlay: TextureOverlay = DrawableOverlay.createStaticDrawableOverlay(logoDrawable, logoSettings)
        return OverlayEffect(ImmutableList.of(logoOverlay))
    }

    override fun onDestroy() {
        super.onDestroy()
        exoPlayer?.release()
        exoPlayer = null
        bitmapColorLut?.release()
        bitmapColorLut = null
    }
}