package com.example.customviews.ui

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Path
import android.graphics.RectF
import android.net.Uri
import android.os.Bundle
import androidx.core.content.IntentCompat
import androidx.core.graphics.get
import androidx.lifecycle.lifecycleScope
import com.example.customviews.databinding.ActivityImageOutlineBinding
import com.example.customviews.ui.base.BaseActivity
import com.example.customviews.utils.decodeBitmapFromUri
import com.example.customviews.utils.dp2Px
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

data class OutlineInfo(
    val bitmap: Bitmap,
    val outlinePath: Path,
)

class ImageOutlineActivity : BaseActivity<ActivityImageOutlineBinding>(ActivityImageOutlineBinding::inflate) {

    private var imageUri: Uri? = null

    override fun initActivity(savedInstanceState: Bundle?) {
        IntentCompat.getParcelableExtra(intent, "imageUri", Uri::class.java)?.let {
            imageUri = it
            loadImageBitmap(it)
        } ?: run { finish() }
        initLayout()
    }

    private fun initLayout() {
        binding.strokeSlider.addOnChangeListener { _, value, _ ->
            binding.imageOutlineView.setShortnessAlpha(value)
        }
        binding.blurSlider.addOnChangeListener { _, value, _ ->
            val maxSize = dp2Px<Int>(16)
            val blurRadius = value * maxSize
            binding.imageOutlineView.setBlurRadius(blurRadius)
        }
        binding.alphaSlider.addOnChangeListener { _, value, _ ->
            val alpha = (255 * value).toInt()
            binding.imageOutlineView.setShadowAlpha(alpha)
        }
        binding.modeSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            binding.imageOutlineView.setMoveMode(isChecked)
        }
    }

    private fun loadImageBitmap(imageUri: Uri) {
        flow {
            decodeBitmapFromUri(this@ImageOutlineActivity, imageUri, 2048)?.let { bitmap ->
                val path = Path()
                emit(OutlineInfo(bitmap, path))
            }
        }
            .flowOn(Dispatchers.IO)
            .onEach { outlineInfo ->
                binding.imageOutlineView.setImageBitmap(outlineInfo)
            }.launchIn(lifecycleScope)
    }

    private fun isEdgePixel(bitmap: Bitmap, x: Int, y: Int): Boolean {
        val currentPixel = bitmap.getPixel(x, y)
        if (Color.alpha(currentPixel) != 255) return false  // 跳过透明像素
        val offsets = listOf(
            Pair(-1, 0), // 左
            Pair(1, 0),  // 右
            Pair(0, -1), // 上
            Pair(0, 1)   // 下
        )
        // 检查是否有相邻的透明像素
        for ((dx, dy) in offsets) {
            val nx = x + dx
            val ny = y + dy
            if (nx in 0 until bitmap.width && ny in 0 until bitmap.height) {
                if (Color.alpha(bitmap.getPixel(nx, ny)) != 255) {
                    return true
                }
            }
        }
        return false
    }

}