package com.example.customviews.ui

import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.customviews.R
import com.example.customviews.databinding.ActivityAdjustShadowBinding
import com.example.customviews.utils.decodeBitmapFromResource
import com.google.android.renderscript.Toolkit
import com.sqsong.opengllib.common.EglBuffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AdjustShadowActivity : AppCompatActivity() {

    private var shadowBitmap: Bitmap? = null
    private lateinit var binding: ActivityAdjustShadowBinding
    private val eglBuffer by lazy { EglBuffer() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAdjustShadowBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        loadShadowBitmap()

        binding.slider.addOnChangeListener { slider, value, fromUser ->
            shadowBitmap?.let { bitmap ->
                blurShadowBitmap(bitmap, value)
            }
        }
        binding.alphaSlider.addOnChangeListener { slider, value, fromUser ->
            binding.adjustShadowView.setShadowAlpha(value)
        }

        binding.moveModelSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            binding.adjustShadowView.setMoveMode(isChecked)
        }
    }

    // 函数根据progress值得到1-25的值
    private fun rangeProgress(progress: Float): Int {
        return (1 + (progress * 24f).toInt())
    }

    private fun blurShadowBitmap(bitmap: Bitmap, progress: Float) {
        flow {
            val radius = rangeProgress(progress)
            Log.d("songmao", "radius: $radius, progress: $progress")
            val blurBitmap = Toolkit.blur(bitmap, radius)
            emit(blurBitmap)
        }.flowOn(Dispatchers.IO)
            .onEach {
                binding.adjustShadowView.updateShadowBitmap(it)
            }
            .launchIn(lifecycleScope)
    }

    private fun loadShadowBitmap() {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                decodeBitmapFromResource(this@AdjustShadowActivity, R.drawable.shadow, maxSize = 256)
            }?.let {
                shadowBitmap = it.copy(Bitmap.Config.ARGB_8888, true)
                binding.adjustShadowView.setShadowBitmap(it)
            }
        }
    }
}