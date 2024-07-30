package com.example.customviews.ui

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.customviews.databinding.ActivityBlurImageBinding
import com.example.customviews.utils.decodeBitmapByGlide
import com.example.customviews.utils.saveBitmapToGallery
import com.example.customviews.view.RulerView
import com.wangxutech.picwish.libnative.NativeLib
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class BlurImageActivity : AppCompatActivity() {

    private lateinit var imageUri: Uri
    private lateinit var binding: ActivityBlurImageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityBlurImageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        imageUri = intent.getParcelableExtra("imageUri") ?: run {
            finish()
            return
        }
        loadImage(imageUri)
        initLayout()
    }

    private fun initLayout() {
        binding.rulerView.setOnValueChangeListener(object : RulerView.OnValueChangeListener {
            override fun onValueChange(value: Float, isStop: Boolean) {
                binding.valueText.text = value.toInt().toString()
                binding.blurImageView.setBlurRadius(value / 100f, isStop)
            }
        })
        binding.getBitmapBtn.setOnClickListener { getBitmap() }
        binding.resultImage.setOnClickListener { binding.resultImage.visibility = View.GONE }
    }

    private fun getBitmap() {
        flow {
            val bitmap = decodeBitmapByGlide(this@BlurImageActivity, imageUri)
            val start = System.currentTimeMillis()
            val b = NativeLib.lightOn(bitmap!!)
            Log.d("songmao", "lightOn bitmap cost time: ${System.currentTimeMillis() - start}ms")
            saveBitmapToGallery(applicationContext, b)
            emit(b)
        }.flowOn(Dispatchers.Default)
            .onEach {
                binding.resultImage.visibility = View.VISIBLE
                binding.resultImage.setImageBitmap(it)
            }
            .launchIn(lifecycleScope)
    }

    private fun loadImage(imageUri: Uri) {
        flow {
            decodeBitmapByGlide(this@BlurImageActivity, imageUri)?.let { emit(it) }
        }.flowOn(Dispatchers.IO)
            .onEach {
                binding.blurImageView.setImageBitmap(it)
            }
            .launchIn(lifecycleScope)
    }
}