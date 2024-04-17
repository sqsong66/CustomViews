package com.example.customviews.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.customviews.databinding.ActivityScaleCanvasBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ScaleCanvasActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScaleCanvasBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScaleCanvasBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.button.setOnClickListener {
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    binding.imageSampleView.getBitmap()
                }.let { bitmap ->
                    binding.showIv.setImageBitmap(bitmap)
                }
            }
        }


    }
}