package com.example.customviews.ui

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.IntentCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.customviews.R
import com.example.customviews.databinding.ActivityImageLightOnBinding
import com.example.customviews.utils.decodeBitmapFromUri
import com.sqsong.opengllib.common.EglBuffer
import com.sqsong.opengllib.filters.LightOnImageFilter
import com.wangxutech.picwish.libnative.NativeLib
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class ImageLightOnActivity : AppCompatActivity() {

    private var imageUri: Uri? = null
    private val eglBuffer by lazy { EglBuffer() }
    private lateinit var binding: ActivityImageLightOnBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityImageLightOnBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        IntentCompat.getParcelableExtra(intent, "imageUri", Uri::class.java)?.let {
            imageUri = it
            binding.srcImage.setImageURI(it)
        }
        binding.openglLightonBtn.setOnClickListener {
            imageUri?.let { lightOnImage(it, true) }
        }
        binding.cppLightonBtn.setOnClickListener {
            imageUri?.let { lightOnImage(it, false) }
        }
    }

    private fun lightOnImage(imageUri: Uri, userOpengl: Boolean) {
        flow {
            decodeBitmapFromUri(this@ImageLightOnActivity, imageUri, 4096)?.let {
                val start = System.currentTimeMillis()
                val bitmap = if (userOpengl) {
                    eglBuffer.getRenderedBitmap(it, LightOnImageFilter(this@ImageLightOnActivity))
                } else {
                    NativeLib.lightOn(it)
                }
                Log.d("songmao", "${if (userOpengl) "OpenGL" else "C++"} lightOnImage cost: ${System.currentTimeMillis() - start}ms, bitmap size: ${bitmap?.width}x${bitmap?.height}")
                emit(bitmap)
            }
        }.flowOn(Dispatchers.IO)
            .onEach {
                binding.lightOnImage.setImageBitmap(it)
            }
            .launchIn(lifecycleScope)
    }

}