package com.example.customviews.ui

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.customviews.R
import com.example.customviews.databinding.ActivityOpenGlimageVideoFilterBinding
import com.example.customviews.utils.decodeBitmapByGlide
import com.example.customviews.view.layout.PhotoAdjustmentLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class OpenGLImageVideoFilterActivity : AppCompatActivity() {

    private var photoAdjustmentLayout: PhotoAdjustmentLayout? = null
    private lateinit var binding: ActivityOpenGlimageVideoFilterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityOpenGlimageVideoFilterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        initLayout()

    }

    private fun initLayout() {
        intent?.getParcelableExtra<Uri>("imageUri")?.let {
            loadImage(it)
        }
    }

    private fun loadImage(imageUri: Uri) {
        flow {
            decodeBitmapByGlide(this@OpenGLImageVideoFilterActivity, imageUri, 2048)?.let { emit(it) }
        }.flowOn(Dispatchers.IO)
            .onEach {
                addAdjustmentLayout(it)
            }
            .launchIn(lifecycleScope)
    }

    private fun addAdjustmentLayout(bitmap: Bitmap) {
        photoAdjustmentLayout = PhotoAdjustmentLayout(
            this, binding.main, bitmap,
            resultCallback = { it?.let(::updatePhotoViewBitmap) },
            removeCallback = {
                photoAdjustmentLayout = null
                finish()
            }
        )
    }

    private fun updatePhotoViewBitmap(bitmap: Bitmap) {

    }
}