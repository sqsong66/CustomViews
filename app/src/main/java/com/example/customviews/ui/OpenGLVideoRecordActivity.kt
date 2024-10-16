package com.example.customviews.ui

import android.graphics.PixelFormat
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.Choreographer
import android.view.SurfaceHolder
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.IntentCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.customviews.R
import com.example.customviews.databinding.ActivityOpenGlvideoRecordBinding
import com.example.customviews.utils.copyInternalVideoToPublic
import com.example.customviews.utils.decodeImageUriBitmapToSize
import com.example.customviews.utils.getVideoOutputPath
import com.sqsong.opengllib.data.RecordConfig
import com.sqsong.opengllib.record.RenderThreadHelper
import com.sqsong.opengllib.utils.getDisplayRefreshNsec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OpenGLVideoRecordActivity : AppCompatActivity(), SurfaceHolder.Callback, Choreographer.FrameCallback {

    private var isRecording = false
    private var videoPath: String? = null
    private lateinit var binding: ActivityOpenGlvideoRecordBinding

    private val renderThreadHelper by lazy {
        RenderThreadHelper(
            recordConfig = RecordConfig(this, getVideoOutputPath(this), 720, 1280, audioAssetPath = "audio/music_sample.aac"),
            renderSurface = binding.surfaceView.holder.surface,
            refreshPeriodNs = getDisplayRefreshNsec(this),
            onVideoRecordDone = {
                Log.d("songmao", "Video record done.")
                lifecycleScope.launch {
                    delay(500)
                    videoPath?.let { internalPath ->
                        val videoUri = copyInternalVideoToPublic(this@OpenGLVideoRecordActivity, internalPath)
                        Log.d("songmao", "Video uri: $videoUri, videoPath: $internalPath")
                        Toast.makeText(this@OpenGLVideoRecordActivity, "Video saved to: $videoUri", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        window.setFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED, WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityOpenGlvideoRecordBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initLayout()

        IntentCompat.getParcelableArrayListExtra(intent, "imageUris", Uri::class.java)?.let {
            Log.d("songmao", "imageUris: $it, size: ${it.size}")
            loadImages(it)
        }
    }

    private fun loadImages(uris: List<Uri>) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                /*val resIds = intArrayOf(
                    R.drawable.img01, R.drawable.img02, R.drawable.img03, R.drawable.img04, R.drawable.img05, R.drawable.img06,
                    R.drawable.img07, R.drawable.img08, R.drawable.img09
                )*/
                val bitmaps = uris.mapNotNull { uri ->
                    val bitmap = decodeImageUriBitmapToSize(this@OpenGLVideoRecordActivity, uri, maxSize = 1024, size = Size(720, 1280))
                    // val bitmap = decodeResourceBitmapByGlide(this@OpenGLVideoRecordActivity, resId, maxSize = 1024)
                    Log.d("GLTransitionActivity", "bitmap: $bitmap, size: ${bitmap?.width}x${bitmap?.height}")
                    bitmap
                }
                bitmaps
            }.let { bitmaps ->
                renderThreadHelper.setImageBitmaps(bitmaps)
            }
        }
    }

    private fun initLayout() {
        binding.surfaceView.holder.setFormat(PixelFormat.TRANSLUCENT)
        binding.surfaceView.setZOrderOnTop(true)
        binding.surfaceView.holder.addCallback(this)

        binding.recordBtn.setOnClickListener {
            isRecording = !isRecording
            val path = if (isRecording) getVideoOutputPath(this) else null
            renderThreadHelper.sendRecordingEnabled(isRecording, path)
            if (path != null) videoPath = path
            binding.recordBtn.text = if (isRecording) "Stop Record" else "Start Record"
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        renderThreadHelper.prepare()
        renderThreadHelper.sendSurfaceCreated()
        Choreographer.getInstance().postFrameCallback(this)
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        renderThreadHelper.sendSurfaceChanged(width, height)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        renderThreadHelper.sendShutdown()
        Choreographer.getInstance().removeFrameCallback(this)
    }

    override fun doFrame(frameTimeNanos: Long) {
        Choreographer.getInstance().postFrameCallback(this)
        renderThreadHelper.sendDoFrame(frameTimeNanos)
    }

}