package com.example.customviews.ui

import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.WindowManager
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.customviews.R
import com.example.customviews.databinding.ActivityGltransitionBinding
import com.example.customviews.utils.decodeResourceBitmapToSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GLTransitionActivity : AppCompatActivity() {

    private var isRecording = false
    private lateinit var binding: ActivityGltransitionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        // 保持屏幕常亮
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityGltransitionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        loadImages()

        binding.recordBtn.setOnClickListener {
            if (isRecording) {
                binding.glSurfaceView.stopRecord()
                binding.recordBtn.text = "Start Record"
            } else {
                binding.glSurfaceView.startRecord()
                binding.recordBtn.text = "Stop Record"
            }
            isRecording = !isRecording
        }
    }

    private fun loadImages() {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val resIds = intArrayOf(
                    R.drawable.img01, R.drawable.img02, R.drawable.img03, R.drawable.img04, R.drawable.img05, R.drawable.img06,
                    R.drawable.img07, R.drawable.img08, R.drawable.img09
                )
                val bitmaps = resIds.map { resId ->
                    val bitmap = decodeResourceBitmapToSize(this@GLTransitionActivity, resId, maxSize = 1024, size = Size(720, 1280))
                    Log.d("GLTransitionActivity", "bitmap: $bitmap, size: ${bitmap.width}x${bitmap.height}")
                    bitmap
                }
                bitmaps
            }.let { bitmaps ->
                binding.glSurfaceView.setImageBitmaps(bitmaps)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.glSurfaceView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.glSurfaceView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.glSurfaceView.onDestroy()
    }
}