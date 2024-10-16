package com.example.customviews.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.util.AttributeSet
import android.util.Log
import android.view.View
import com.example.customviews.ui.OutlineInfo
import com.example.customviews.utils.dp2Px
import com.example.customviews.utils.resizeBitmap


class ImageOutlineView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : View(context, attrs, defStyle) {

    private var outlineColor = Color.YELLOW
    private var outlineWidth = dp2Px<Int>(12)

    private val borderPath = Path()
    private val imageMatrix = Matrix()
    private val alphaMatrix = Matrix()
    private var imageBitmap: Bitmap? = null
    private var alphaBitmap: Bitmap? = null

    private val blurPaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isFilterBitmap = true
            isDither = true
            color = outlineColor
            style = Paint.Style.STROKE
            strokeWidth = outlineWidth.toFloat()
            setLayerType(LAYER_TYPE_SOFTWARE, this)
        }
    }

    private val imagePaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isFilterBitmap = true
            isDither = true
        }
    }

    private val outlinePaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = outlineColor
            style = Paint.Style.STROKE
            strokeWidth = outlineWidth.toFloat()
        }
    }

    override fun onDraw(canvas: Canvas) {
        if (blurPaint.maskFilter != null) {
            alphaBitmap?.let {
                canvas.drawBitmap(it, alphaMatrix, blurPaint)
            }
        }



        imageBitmap?.let {
//            val newWidth = it.width + outlineWidth * 2f
//            val newHeight = it.height + outlineWidth * 2f
//            val newSx = newWidth / it.width
//            val newSy = newHeight / it.height
//            alphaMatrix.reset()
//            alphaMatrix.set(imageMatrix)
//            alphaMatrix.postScale(newSx, newSy, width / 2f, height / 2f)
//            canvas.drawBitmap(it, alphaMatrix, imagePaint)
//            canvas.drawColor(outlineColor, PorterDuff.Mode.SRC_ATOP)
            canvas.drawBitmap(it, imageMatrix, imagePaint)
        }
    }

    fun setImageBitmap(outlineInfo: OutlineInfo) {
        val bitmap = outlineInfo.bitmap
        val resultBitmap = bitmap.extractAlpha()
        imageBitmap = bitmap
        alphaBitmap = resizeBitmap(resultBitmap, 512)
        layoutImage(bitmap)
        Log.d("ImageOutlineView", "bitmap size: ${bitmap.width}x${bitmap.height}, alpha size: ${resultBitmap.width}x${resultBitmap.height}")
    }

    private fun layoutImage(bitmap: Bitmap) {
        if (width == 0 || height == 0) return
        // 居中摆放图片
        val scale: Float
        val dx: Float
        val dy: Float
        var destWidth = width * 0.8f
        var destHeight = destWidth * bitmap.height / bitmap.width
        if (destHeight > height * 0.8f) {
            destHeight = height * 0.8f
            destWidth = destHeight * bitmap.width / bitmap.height
        }
        scale = destWidth / bitmap.width
        dx = (width - destWidth) / 2
        dy = (height - destHeight) / 2
        imageMatrix.reset()
        imageMatrix.postScale(scale, scale)
        imageMatrix.postTranslate(dx, dy)

        alphaBitmap?.let { newBitmap ->
            val alphaScale = destWidth / newBitmap.width
            alphaMatrix.reset()
            alphaMatrix.postScale(alphaScale, alphaScale)
            alphaMatrix.postTranslate(dx, dy)
        }

        invalidate()
    }

    fun setOutlineWidth(width: Int) {
        outlineWidth = width
        blurPaint.strokeWidth = width.toFloat()
        invalidate()
    }

    fun setBlurRadius(blurRadius: Float) {
        blurPaint.maskFilter = if (blurRadius > 0f) {
            BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL)
        } else {
            null
        }
        invalidate()
    }

}