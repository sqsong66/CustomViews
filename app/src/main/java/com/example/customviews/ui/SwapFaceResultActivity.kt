package com.example.customviews.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.customviews.R
import com.example.customviews.databinding.ActivitySwapFaceResultBinding
import com.example.customviews.utils.decodeBitmapFromAssets
import com.example.customviews.utils.decodeResourceBitmapByGlide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SwapFaceResultActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySwapFaceResultBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySwapFaceResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.loadImageBtn.setOnClickListener { loadImage() }
        binding.clearBtn.setOnClickListener { binding.swapFaceResultView.setImageBitmap(null) }
    }

    private fun loadImage() {
        binding.swapFaceResultView.setImageBitmap(null)
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                decodeBitmapFromAssets(this@SwapFaceResultActivity, "image/image01.png", 2048)
            }.let { bitmap ->
                binding.swapFaceResultView.setImageBitmap(bitmap)
            }
        }
    }
}