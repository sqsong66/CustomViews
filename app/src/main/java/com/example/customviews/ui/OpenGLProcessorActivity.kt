package com.example.customviews.ui

import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import android.util.Log
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.customviews.R
import com.example.customviews.databinding.ActivityOpenGlprocessorBinding
import com.example.customviews.utils.decodeBitmapFromResource
import com.example.customviews.utils.resizeBitmap
import com.sqsong.opengllib.common.EglBuffer
import com.sqsong.opengllib.filters.GaussianBlurImageFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OpenGLProcessorActivity : AppCompatActivity() {

    private var srcBitmap: Bitmap? = null
    private var blurBitmap: Bitmap? = null
    private lateinit var binding: ActivityOpenGlprocessorBinding

    private val eglBuffer by lazy { EglBuffer() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOpenGlprocessorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadImageBitmap()
        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                blurBitmap?.let { bitmap ->
                    blurImageBitmap(bitmap, progress)
                }
//                srcBitmap?.let { bitmap ->
//                    mosaicBitmap(bitmap, progress)
//                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })
    }

    private fun blurImageBitmap(bitmap: Bitmap, progress: Int) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val start = System.currentTimeMillis()
                val radius = (progress.toFloat() / 100f * 45f).toInt()
                val blur = resizeBitmap(bitmap, 512)
                val destBitmap = eglBuffer.getRenderedBitmap(blur, GaussianBlurImageFilter(this@OpenGLProcessorActivity, radius))
                // val destBitmap = eglBuffer.getBlurBitmap(blur, radius, Mode.MODE_GAUSSIAN)
                Log.w("songmao", "blurImageBitmap cost time: ${System.currentTimeMillis() - start} ms, bitmap size: ${destBitmap?.width}x${destBitmap?.height}")
                destBitmap
            }?.let {
                binding.dstImage.setImageBitmap(it)
            }
        }
    }

    @OptIn(FlowPreview::class)
//    private fun mosaicBitmap(bitmap: Bitmap, progress: Int) {
//        flow<Bitmap?> {
//            val start = System.currentTimeMillis()
//            val mosaicSize = 0.05f * progress / 100f  // 10f + progress.toFloat() / 100f * 10f
//            // val destBitmap = eglBuffer.getMosaicBitmap(bitmap.copy(bitmap.config, true), mosaicSize)
//            val destBitmap = eglBuffer.getTestBitmap(bitmap.copy(bitmap.config, true))
//            Log.w("songmao", "blurImageBitmap cost time: ${System.currentTimeMillis() - start} ms, bitmap size: ${destBitmap.width}x${destBitmap.height}")
//            emit(destBitmap)
//        }
//            .debounce(100)
//            .onEach { result ->
//                result?.let {
//                    binding.dstImage.setImageBitmap(it)
//                }
//            }
//            .launchIn(lifecycleScope)
//    }

    private fun loadImageBitmap() {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                decodeBitmapFromResource(this@OpenGLProcessorActivity, R.drawable.img_sample01, 2048)?.let {
                    val matrix = Matrix()
                    matrix.postScale(0.25f, 0.25f)
                    val scaledBitmap = Bitmap.createBitmap(it, 0, 0, it.width, it.height, matrix, true)
                    Pair(it, it)
                }
            }?.let { bitmapPair ->
                srcBitmap = bitmapPair.first
                blurBitmap = bitmapPair.second

                binding.srcImage.setImageBitmap(bitmapPair.first)
                binding.dstImage.setImageBitmap(bitmapPair.first)
            }
        }
    }
}