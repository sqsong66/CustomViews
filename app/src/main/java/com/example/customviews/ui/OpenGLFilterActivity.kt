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
import com.example.customviews.utils.getLutFilterData
import com.sqsong.opengllib.filters.BaseImageFilter
import com.sqsong.opengllib.filters.BlurImageFilter
import com.sqsong.opengllib.filters.TestImageFilter
import com.wangxutech.picwish.libnative.NativeLib
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class OpenGLFilterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOpenGlfilterBinding

    private val blurImageFilter by lazy {
        BlurImageFilter(this)
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

        // applyFilter()
        binding.glSurfaceView.setFilter(blurImageFilter)
    }

    private fun loadImage(imageUri: Uri) {
        flow {
            decodeBitmapByGlide(this@OpenGLFilterActivity, imageUri, 2048)?.let {
                emit(it)
            }
        }.flowOn(Dispatchers.IO)
            .onEach {
                binding.glSurfaceView.setImageBitmap(it)
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
            val filterData = getLutFilterData()[8]
            val lutBitmaps = filterData.filterLutAssets!!.map { BitmapFactory.decodeStream(assets.open(it)) }
            emit(TestImageFilter(this@OpenGLFilterActivity, lutBitmaps, fragmentAsset = filterData.filterFragShaderPath))
        }.flowOn(Dispatchers.IO)
            .onEach {
                Log.d("LUTFilterActivity", "LUT list: $it")
                binding.glSurfaceView.setFilter(it)
            }.launchIn(lifecycleScope)
    }

    override fun onPause() {
        super.onPause()
        binding.glSurfaceView.onPause()
    }

    override fun onResume() {
        super.onResume()
        binding.glSurfaceView.onResume()
    }
}