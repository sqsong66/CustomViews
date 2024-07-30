package com.example.customviews.ui

import android.graphics.PixelFormat
import android.graphics.SurfaceTexture
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.Choreographer
import android.view.SurfaceHolder
import android.view.TextureView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.IntentCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.customviews.R
import com.example.customviews.databinding.ActivityOpenGlvideoRecordBinding
import com.example.customviews.utils.decodeResourceBitmapToSize
import com.example.customviews.utils.getVideoPath
import com.sqsong.opengllib.data.RecordConfig
import com.sqsong.opengllib.record.RenderThreadHelper
import com.sqsong.opengllib.utils.getDisplayRefreshNsec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OpenGLVideoRecordActivity : AppCompatActivity(), SurfaceHolder.Callback, Choreographer.FrameCallback {

    private lateinit var binding: ActivityOpenGlvideoRecordBinding

    private val renderThreadHelper by lazy {
        RenderThreadHelper(
            recordConfig = RecordConfig(this, getVideoPath(this), 720, 1280),
            renderSurface = binding.surfaceView.holder.surface,
            refreshPeriodNs = getDisplayRefreshNsec(this)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
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
            // loadBitmaps(it)
        }
        loadImages()
    }

    private fun loadImages() {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val resIds = intArrayOf(
                    R.drawable.img01, R.drawable.img02, R.drawable.img03, R.drawable.img04, R.drawable.img05, R.drawable.img06,
                    R.drawable.img07, R.drawable.img08, R.drawable.img09
                )
                val bitmaps = resIds.map { resId ->
                    val bitmap = decodeResourceBitmapToSize(this@OpenGLVideoRecordActivity, resId, maxSize = 1024, size = Size(720, 1280))
                    Log.d("GLTransitionActivity", "bitmap: $bitmap, size: ${bitmap.width}x${bitmap.height}")
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
    }

    override fun doFrame(frameTimeNanos: Long) {
        Choreographer.getInstance().postFrameCallback(this)
        renderThreadHelper.sendDoFrame(frameTimeNanos)
    }

    override fun onResume() {
        super.onResume()
        // start the draw events
        Choreographer.getInstance().postFrameCallback(this)
    }

    override fun onPause() {
        super.onPause()
        // stop the draw events
        Choreographer.getInstance().removeFrameCallback(this)
    }

}