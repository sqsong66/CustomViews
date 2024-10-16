package com.example.customviews.ui

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.IntentCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.customviews.R
import com.example.customviews.data.ImageFilterData
import com.example.customviews.databinding.ActivityImageLutBinding
import com.example.customviews.ui.adapter.LutPreviewAdapter
import com.example.customviews.utils.decodeBitmapByGlide
import com.example.customviews.utils.getLutFilterData
import com.sqsong.opengllib.common.EglBuffer
import com.sqsong.opengllib.filters.BaseImageFilter
import com.sqsong.opengllib.filters.TestImageFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class ImageLutActivity : AppCompatActivity() {

    private val eglBuffer by lazy { EglBuffer() }
    private lateinit var binding: ActivityImageLutBinding

    private val lutPreviewAdapter by lazy {
        LutPreviewAdapter {
            // Apply filter
            applyFilterToImage(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityImageLutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        IntentCompat.getParcelableExtra(intent, "imageUri", Uri::class.java)?.let {
            loadFilterBitmap(it)
            loadLutPreview(it)
        }
        initLayout()
    }

    private fun loadFilterBitmap(imageUri: Uri) {
        flow {
            decodeBitmapByGlide(this@ImageLutActivity, imageUri)?.let { srcBitmap ->
                emit(srcBitmap)
            }
        }.flowOn(Dispatchers.IO)
            .onEach {
                binding.glSurfaceView.setImageBitmap(it)
            }
            .launchIn(lifecycleScope)
    }

    private fun initLayout() {
        binding.recyclerView.adapter = lutPreviewAdapter
        binding.previewImageView.setOnClickListener { binding.previewImageView.visibility = View.GONE }

        binding.getBitmapBtn.setOnClickListener { loadPreviewBitmap() }

        binding.slider.addOnChangeListener { _, value, fromUser ->
            if (!fromUser) return@addOnChangeListener
            lutPreviewAdapter.updateCurrentFilterIntensity(value)
            binding.glSurfaceView.setProgress(value)
        }
    }

    private fun loadPreviewBitmap() {
        binding.glSurfaceView.queueEvent {
            binding.glSurfaceView.getRenderedBitmap()?.let {
                runOnUiThread {
                    binding.previewImageView.setImageBitmap(it)
                    binding.previewImageView.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun loadLutPreview(srcUri: Uri) {
        flow {
            decodeBitmapByGlide(this@ImageLutActivity, srcUri, 180)?.let { srcBitmap ->
                getLutFilterData().forEach { imageData ->
                    if (imageData.filterLutAssets.isNullOrEmpty()) {
                        emit(imageData.copy(previewBitmap = srcBitmap, clear = true))
                    } else {
                        /*assets.open(imageData.filterLutAssets).use { inputStream ->
                            val lutBitmap = BitmapFactory.decodeStream(inputStream)
                            eglBuffer.getRenderedBitmap(srcBitmap, LUTImageFilter(this@ImageLutActivity, lutBitmap, initOutputBuffer = false))?.let {
                                emit(imageData.copy(previewBitmap = it))
                            }
                        }*/
                        val bitmaps = imageData.filterLutAssets.map {
                            assets.open(it).use { stream -> BitmapFactory.decodeStream(stream) }
                        }
                        val filter = TestImageFilter(this@ImageLutActivity, bitmaps, fragmentAsset = imageData.filterFragShaderPath, initOutputBuffer = false)
                        eglBuffer.getRenderedBitmap(srcBitmap, filter)?.let {
                            emit(imageData.copy(previewBitmap = it))
                        }
                    }
                }
            }
        }.flowOn(Dispatchers.IO)
            .catch { it.printStackTrace() }
            .onEach {
                lutPreviewAdapter.addNewData(it, it.clear)
            }
            .launchIn(lifecycleScope)
    }

    @OptIn(FlowPreview::class)
    private fun applyFilterToImage(filterData: ImageFilterData) {
        flow {
            if (filterData.filterLutAssets.isNullOrEmpty()) {
                emit(BaseImageFilter(this@ImageLutActivity))
            } else {
//                val bitmap = BitmapFactory.decodeStream(assets.open(filterData.filterLutAssets))
//                emit(LUTImageFilter(this@ImageLutActivity, bitmap))
                val bitmaps = filterData.filterLutAssets.map {
                    assets.open(it).use { stream -> BitmapFactory.decodeStream(stream) }
                }
                emit(TestImageFilter(this@ImageLutActivity, bitmaps, fragmentAsset = filterData.filterFragShaderPath))
            }
        }.debounce(200)
            .flowOn(Dispatchers.IO)
            .onEach {
                binding.slider.visibility = if (filterData.filterLutAssets.isNullOrEmpty()) View.INVISIBLE else View.VISIBLE
                binding.glSurfaceView.setFilter(it, filterData.intensity)
                binding.slider.value = filterData.intensity
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

}