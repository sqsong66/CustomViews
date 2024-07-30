package com.example.customviews.ui

import android.graphics.Bitmap
import android.graphics.ColorFilter
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.customviews.R
import com.example.customviews.databinding.ActivityGifMakerBinding
import com.example.customviews.utils.copyInternalGifToPublic
import com.example.customviews.utils.decodeBitmapByGlide
import com.example.customviews.utils.decodeResourceBitmapByGlide
import com.example.customviews.utils.getGifPath
import com.example.customviews.utils.getThemeColor
import com.example.customviews.utils.gif.GifEncoder
import com.example.customviews.view.anno.GifQuality
import com.sqsong.photoeditor.view.anno.CropMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import java.io.File
import java.io.FileOutputStream

class GifMakerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGifMakerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityGifMakerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        IntentCompat.getParcelableArrayListExtra(intent, "imageUris", Uri::class.java)?.let {
            Log.d("songmao", "imageUris: $it, size: ${it.size}")
            loadBitmaps(it)
        }
        val color = getThemeColor(this, com.google.android.material.R.attr.colorOnPrimary)
        binding.toolbar.overflowIcon?.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
        binding.makeGifBtn.setOnClickListener {
            makeGif()
        }
        binding.applyCropModeBtn.setOnClickListener {
            binding.gifIv.applyCropMode(binding.gifIv.getCropMode())
        }
        binding.toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.cropOriginalMenu -> {
                    binding.gifIv.setCropMode(CropMode.ORIGIN)
                }

                R.id.crop11Menu -> {
                    binding.gifIv.setCropMode(CropMode.RATIO_1_1)
                }

                R.id.crop34Menu -> {
                    binding.gifIv.setCropMode(CropMode.RATIO_3_4)
                }

                R.id.crop43Menu -> {
                    binding.gifIv.setCropMode(CropMode.RATIO_4_3)
                }

                R.id.crop169Menu -> {
                    binding.gifIv.setCropMode(CropMode.RATIO_16_9)
                }

                R.id.crop916Menu -> {
                    binding.gifIv.setCropMode(CropMode.RATIO_9_16)
                }
            }
            true
        }
    }

    private fun makeGif() {
        val bitmaps = binding.gifIv.getGifBitmaps()
        if (bitmaps.isEmpty()) return
        val gifSize = binding.gifIv.getGifSize(GifQuality.HIGH)
        val destRect = binding.gifIv.getOrigin()
        val delay = binding.gifIv.getGifDelay()
        flow<String> {
            val outputPath = getGifPath(this@GifMakerActivity)
            val outputFile = File(outputPath, "test.gif")
            if (outputFile.parentFile?.exists() == false) {
                outputFile.parentFile?.mkdirs()
            }
            if (outputFile.exists()) {
                outputFile.delete()
            }
            val gifEncoder = GifEncoder()
            gifEncoder.setSize(gifSize.width, gifSize.height)
            gifEncoder.start(FileOutputStream(outputFile))
            gifEncoder.setDelay(delay.toInt())
            gifEncoder.setQuality(1)
            gifEncoder.setTransparent(0)
            bitmaps.forEach {
                gifEncoder.addFrame(it, destRect)
            }
            gifEncoder.finish()
            copyInternalGifToPublic(this@GifMakerActivity, outputFile.absolutePath)
            emit(outputFile.absolutePath)
        }.flowOn(Dispatchers.IO)
            .catch { e ->
                Log.e("GifMakerActivity", "makeGif error: $e")
            }
            .onStart {
                binding.makeGifBtn.isEnabled = false
            }
            .onEach {
                binding.makeGifBtn.isEnabled = true
                Log.d("GifMakerActivity", "makeGif success: $it")
                // Glide.with(binding.gifIv).load(it).into(binding.gifIv)
            }
            .launchIn(lifecycleScope)
    }

    private fun getBitmaps(): List<Bitmap> {
        val resIds = intArrayOf(
            /*R.drawable.img01, R.drawable.img02, */R.drawable.img03, R.drawable.img04, R.drawable.img05, R.drawable.img06,
            R.drawable.img07, R.drawable.img08, R.drawable.img09
        )
        val bitmaps = resIds.map { resId ->
            val bitmap = decodeResourceBitmapByGlide(this@GifMakerActivity, resId, maxSize = 2048)
            Log.d("GLTransitionActivity", "bitmap: $bitmap, size: ${bitmap.width}x${bitmap.height}")
            bitmap
        }
        return bitmaps
    }

    private fun loadBitmaps(imageUris: List<Uri>) {
        flow {
            val bitmaps = imageUris.mapNotNull { uri ->
                val bitmap = decodeBitmapByGlide(this@GifMakerActivity, uri, maxSize = 2048)
                Log.d("GifMakerActivity", "bitmap: $bitmap, size: ${bitmap?.width}x${bitmap?.height}")
                bitmap
            }
            emit(bitmaps)
        }.flowOn(Dispatchers.IO)
            .catch { e ->
                Log.e("GifMakerActivity", "loadBitmaps error: $e")
            }
            .onEach {
                Log.d("GifMakerActivity", "loadBitmaps success: $it")
                binding.gifIv.setGifBitmaps(it)
            }
            .launchIn(lifecycleScope)
    }
}