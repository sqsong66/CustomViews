package com.example.customviews.ui

import android.graphics.Bitmap
import android.graphics.PointF
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.customviews.R
import com.example.customviews.databinding.ActivityImageFilterBinding
import com.example.customviews.utils.decodeResourceBitmapByGlide
import com.sqsong.imagefilter.processor.Filter
import com.sqsong.imagefilter.processor.filters.SaturationSubFilter
import com.sqsong.imagefilter.processor.filters.ToneCurveSubFilter
import com.sqsong.imagefilter.processor.filters.VignetteSubFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ImageFilterActivity : AppCompatActivity() {

    private var imageBitmap: Bitmap? = null
    private lateinit var binding: ActivityImageFilterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImageFilterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        lifecycleScope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                decodeResourceBitmapByGlide(applicationContext, R.drawable.img_sample01, 2048)
            }
            imageBitmap = bitmap
            binding.originalImage.setImageBitmap(bitmap)
        }

        binding.addFilterBtn.setOnClickListener {
            imageBitmap?.let { applyFilterToImage(it) }
        }
    }

    private fun applyFilterToImage(bitmap: Bitmap) {
        lifecycleScope.launch {
            val start = System.currentTimeMillis()
            val newBitmap = withContext(Dispatchers.IO) {
                val rgbKnots = arrayOf(
                    PointF(0f, 0f),
                    PointF(210f, 170f),
                    PointF(255f, 255f)
                )
                val redKnots = arrayOf(
                    PointF(0f, 0f),
                    PointF(212f, 151f),
                    PointF(255f, 255f)
                )
                val filters = Filter()
                val filter = ToneCurveSubFilter(rgbKnots = rgbKnots, redKnots = redKnots)
                filters.addSubFilter(filter)
//                val filter1 = SaturationSubFilter(level = 0f)
//                filters.addSubFilter(filter1)
                val filter2 = VignetteSubFilter(context = applicationContext, alphaValue = 120)
                filters.addSubFilter(filter2)
                filters.process(bitmap.copy(Bitmap.Config.ARGB_8888, true))
            }
            Log.d("sqsong", "applyFilterToImage cost: ${System.currentTimeMillis() - start} ms, bitmap size: ${bitmap.width}x${bitmap.height}")
            binding.filterImage.setImageBitmap(newBitmap)
        }
    }
}