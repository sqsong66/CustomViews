package com.example.customviews.ui

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.customviews.databinding.ActivityMosaicBinding
import com.example.customviews.utils.decodeBitmapByGlide
import com.example.customviews.utils.dp2Px
import com.example.customviews.view.pixelate.Pixelate
import com.example.customviews.view.pixelate.PixelateLayer
import com.sqsong.opengl.processor.EglBuffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class MosaicActivity : AppCompatActivity() {

    private val eglBuffer by lazy { EglBuffer() }
    private lateinit var binding: ActivityMosaicBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMosaicBinding.inflate(layoutInflater)
        setContentView(binding.root)

        intent?.getParcelableExtra<Uri>("imageUri")?.let {
            initImageBitmap(it)
        }

        binding.previewImageView.setOnClickListener { binding.previewImageView.visibility = View.GONE }
        binding.showPreviewButton.setOnClickListener { showPreview() }
        binding.clearMosaicButton.setOnClickListener { binding.mosaicView.clearMosaic() }
    }

    private fun initImageBitmap(imageUri: Uri) {
        flow {
            decodeBitmapByGlide(this@MosaicActivity, imageUri, 4096)?.let { bitmap ->
//                val matrix = Matrix()
//                matrix.postScale(0.25f, 0.25f)
//                val scaledBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
//                val blurBitmap = eglBuffer.getBlurBitmap(scaledBitmap, 15, Mode.MODE_GAUSSIAN)

//                val matrix = Matrix()
//                matrix.postScale(1.0f, 1.0f)
//                val scaledBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
//                val blurBitmap = eglBuffer.getMosaicBitmap(scaledBitmap, 0.02f)

                val start = System.currentTimeMillis()
                val blurBitmap = Pixelate.fromBitmap(
                    bitmap,
//                    PixelateLayer.Builder(PixelateLayer.Shape.Diamond)
//                        .setResolution(dp2Px(24))
//                        .setSize(dp2Px(25))
//                        .build(),
                    PixelateLayer.Builder(PixelateLayer.Shape.Square)
                        .setResolution(dp2Px(24))
//                        .setAlpha(0.6f)
                        .build(),
                    PixelateLayer.Builder(PixelateLayer.Shape.Diamond)
                        .setResolution(dp2Px(24))
                        .setOffset(dp2Px(12))
                        .build(),
//                    PixelateLayer.Builder(PixelateLayer.Shape.Circle)
//                        .setResolution(dp2Px(28))
//                        .setOffset(dp2Px(14))
//                        .build(),
                )
                emit(Pair(bitmap, blurBitmap))
                Log.w("songmao", "initImageBitmap cost: ${System.currentTimeMillis() - start}ms.")
            }
        }.flowOn(Dispatchers.IO)
            .onEach { resultPair ->
                binding.mosaicView.setImageBitmaps(resultPair.first, resultPair.second)
            }.launchIn(lifecycleScope)
    }

    private fun showPreview() {
        flow {
            val startTime = System.currentTimeMillis()
            binding.mosaicView.getMosaicBitmap()?.let { emit(it) }
            Log.w("songmao", "showPreview cost: ${System.currentTimeMillis() - startTime}ms.")
        }.flowOn(Dispatchers.IO)
            .onEach { bitmap ->
                binding.previewImageView.setImageBitmap(bitmap)
                binding.previewImageView.visibility = View.VISIBLE
            }.launchIn(lifecycleScope)
    }
}