package com.example.customviews.ui

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.customviews.databinding.ActivityLutfilterBinding
import com.example.customviews.ui.adapter.LutItemAdapter
import com.example.customviews.utils.decodeBitmapByGlide
import com.sqsong.opengl.processor.EglBuffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class LUTFilterActivity : AppCompatActivity() {

    private var lutPath: String? = null
    private val eglBuffer = EglBuffer()
    private var sourceBitmap: Bitmap? = null
    private lateinit var binding: ActivityLutfilterBinding

    private val lutItemAdapter by lazy {
        LutItemAdapter { path, positon ->
            lutPath = path
            Log.d("LUTFilterActivity", "LUT item clicked: $path")
            sourceBitmap?.let { bitmap ->
                loadLutFilterImage(bitmap, path)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLutfilterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.recyclerView.adapter = lutItemAdapter

        loadLutFiles()

        intent?.getParcelableExtra<Uri>("imageUri")?.let {
            loadImage(it)
        }

        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val path = lutPath ?: return
                sourceBitmap?.let {
                    loadLutFilterImage(it, path, progress / 100f)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })
    }

    private fun loadImage(imageUri: Uri) {
        flow {
            decodeBitmapByGlide(this@LUTFilterActivity, imageUri, 2048)?.let { emit(it) }
        }.flowOn(Dispatchers.IO)
            .onEach {
                sourceBitmap = it
                binding.originalImage.setImageBitmap(it)
            }
            .launchIn(lifecycleScope)
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

    @OptIn(FlowPreview::class)
    private fun loadLutFilterImage(bitmap: Bitmap, path: String, intensity: Float = 1.0f) {
        flow {
            val start = System.currentTimeMillis()
            Glide.with(this@LUTFilterActivity).asBitmap().load("file:///android_asset/lut/$path").submit().get()?.let { lutBitmap ->
                eglBuffer.getLutFilterBitmap(bitmap.copy(bitmap.config, true), lutBitmap, intensity)?.let { emit(it) }
            }
            Log.d("songmao", "LUT filter cost: ${System.currentTimeMillis() - start}ms, path: $path, intensity: $intensity")
        }.debounce(100)
            .flowOn(Dispatchers.IO)
            .onEach {
                binding.ivLutImage.setImageBitmap(it)
            }
            .launchIn(lifecycleScope)
    }
}