package com.example.customviews.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.customviews.databinding.ActivityOpenGlsampleBinding
import com.example.customviews.ui.adapter.LutItemAdapter
import com.example.customviews.utils.decodeBitmapByGlide
import com.sqsong.opengllib.common.EglBuffer
import com.sqsong.opengllib.filters.GaussianBlurImageFilter
import com.sqsong.opengllib.filters.LUTImageFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class OpenGLSampleActivity : AppCompatActivity() {

    private var lutPath: String? = null
    private var imageBitmap: Bitmap? = null
    private val eglBuffer: EglBuffer by lazy { EglBuffer() }
    private lateinit var binding: ActivityOpenGlsampleBinding

    private val lutItemAdapter by lazy {
        LutItemAdapter { path, position ->
            lutPath = path
            Log.d("LUTFilterActivity", "LUT item clicked: $path")
            loadLutFilterImage(path, position)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOpenGlsampleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        intent?.getParcelableExtra<Uri>("imageUri")?.let {
            loadImage(it)
        }

        binding.recyclerView.adapter = lutItemAdapter
        loadLutFiles()

        binding.slider.addOnChangeListener { _, value, _ ->
            binding.glSurfaceView.setProgress(value)
        }
        binding.getBitmapBtn.setOnClickListener {
            loadRenderedBitmap()
        }
        binding.previewImage.setOnClickListener {
            binding.previewImage.visibility = View.GONE
        }
    }

    private fun loadRenderedBitmap() {
        binding.glSurfaceView.queueEvent {
            val start = System.currentTimeMillis()
            binding.glSurfaceView.getRenderedBitmap()?.let {
                Log.d("songmao", "Rendered bitmap cost: ${System.currentTimeMillis() - start}ms, thread: ${Thread.currentThread().name}")
                runOnUiThread {
                    binding.previewImage.setImageBitmap(it)
                    binding.previewImage.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun loadImage(imageUri: Uri) {
        flow {
            decodeBitmapByGlide(this@OpenGLSampleActivity, imageUri, 2048)?.let { emit(it) }
        }.flowOn(Dispatchers.IO)
            .onEach {
                imageBitmap = it
                binding.glSurfaceView.setImageBitmap(it)
            }
            .launchIn(lifecycleScope)
    }

    override fun onPause() {
        super.onPause()
        binding.glSurfaceView.onPause()
    }

    override fun onResume() {
        super.onResume()
        binding.glSurfaceView.onResume()
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
    private fun loadLutFilterImage(path: String, position: Int) {
        flow {
            val start = System.currentTimeMillis()
            BitmapFactory.decodeStream(assets.open("lut/$path")).let { lutBitmap ->
                emit(lutBitmap)
                Log.d("songmao", "LUT filter cost: ${System.currentTimeMillis() - start}ms, path: $path, lutBitmap size: ${lutBitmap.width}x${lutBitmap.height}")
            }
        }.debounce(100)
            .flowOn(Dispatchers.IO)
            .onEach {
                if (position == 0) {
                    binding.glSurfaceView.setFilter(GaussianBlurImageFilter(this@OpenGLSampleActivity), binding.slider.value)
                } else {
                    binding.glSurfaceView.setFilter(LUTImageFilter(this@OpenGLSampleActivity, it), binding.slider.value)
                }
            }
            .launchIn(lifecycleScope)
    }

//    @OptIn(FlowPreview::class)
//    private fun loadLutFilterImage(path: String) {
//        flow {
//            val start = System.currentTimeMillis()
//            Glide.with(this@OpenGLSampleActivity).asBitmap().load("file:///android_asset/lut/$path").submit().get()?.let { lutBitmap ->
//                val filter = LUTImageFilter(this@OpenGLSampleActivity, lutBitmap)
//                imageBitmap?.let {
//                    eglBuffer.getRenderedBitmap(it, filter)?.let { renderedBitmap ->
//                        emit(renderedBitmap)
//                    }
//                }
//            }
//            Log.d("songmao", "LUT filter cost: ${System.currentTimeMillis() - start}ms, path: $path")
//        }.debounce(100)
//            .flowOn(Dispatchers.IO)
//            .onEach {
//                binding.previewImage.setImageBitmap(it)
//                binding.previewImage.visibility = View.VISIBLE
//            }
//            .launchIn(lifecycleScope)
//    }

}