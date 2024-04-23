package com.example.customviews.ui

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.customviews.databinding.ActivityOpenGlfilterBinding
import com.example.customviews.utils.decodeBitmapByGlide
import com.sqsong.opengllib.filters.BaseImageFilter
import com.sqsong.opengllib.filters.BrightnessImageFilter
import com.sqsong.opengllib.filters.ComposeAdjustImageFilter
import com.sqsong.opengllib.filters.ContrastImageFilter
import com.sqsong.opengllib.filters.GaussianBlurImageFilter
import com.sqsong.opengllib.filters.LUTImageFilter
import com.sqsong.opengllib.filters.VignetteImageFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class OpenGLFilterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOpenGlfilterBinding

    private val imageFilter by lazy {
        BrightnessImageFilter(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOpenGlfilterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        intent?.getParcelableExtra<Uri>("imageUri")?.let {
            loadImage(it)
        }

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

    private fun loadImage(imageUri: Uri) {
        flow {
            decodeBitmapByGlide(this@OpenGLFilterActivity, imageUri, 2048)?.let { emit(it) }
        }.flowOn(Dispatchers.IO)
            .onEach {
                binding.glSurfaceView.setImageBitmap(it)
                binding.glSurfaceView.setFilter(imageFilter)
            }
            .launchIn(lifecycleScope)
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

    private fun applyFilter() {
        flow<BaseImageFilter> {
            BitmapFactory.decodeStream(assets.open("lut/filter_03.png")).let { lutBitmap ->
                emit(GaussianBlurImageFilter(this@OpenGLFilterActivity, 0))
            }
        }.flowOn(Dispatchers.IO)
            .onEach {
                Log.d("LUTFilterActivity", "LUT list: $it")
                binding.glSurfaceView.setFilter(it)
            }.launchIn(lifecycleScope)
    }
}