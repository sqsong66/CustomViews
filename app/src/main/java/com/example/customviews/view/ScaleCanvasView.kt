package com.example.customviews.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import com.example.customviews.utils.dp2Px

class ScaleCanvasView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val tempRect = Rect()
    private val tempRectF = RectF()
    private var bitmap: Bitmap? = null
    private var scaleValue = 1f
    private var spacing = dp2Px<Float>(10)
    private var interleaving = 0f

    private val text = "Hello, World!"

    private var offsetX = 0f
    private var offsetY = 0f

    private var lastX = 0f
    private var lastY = 0f

    private val textPaint = Paint().apply {
        color = Color.BLACK
        textSize = dp2Px(10)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                Log.w("sqsong", "ACTION_DOWN")
                lastX = event.x
                lastY = event.y
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - lastX
                val dy = event.y - lastY
                offsetX += (dx / scaleValue)
                offsetY += (dy / scaleValue)
                invalidate()
                Log.w("sqsong", "ACTION_MOVE")
                lastX = event.x
                lastY = event.y
            }

            MotionEvent.ACTION_UP -> {
                Log.w("sqsong", "ACTION_UP")
            }
        }
        return true
    }

    override fun onDraw(canvas: Canvas) {
        bitmap?.let { b ->
            canvas.save()
            canvas.scale(scaleValue, scaleValue, width / 2f, height / 2f)
            // drawText(canvas)
            drawImage(canvas, b)
            canvas.restore()
        }
    }

    private fun drawImage(canvas: Canvas, bitmap: Bitmap) {
        val bWidth = width / 10f
        val bHeight = bWidth * bitmap.height / bitmap.width

        // 根据交错计算更新
        interleaving = (bWidth + spacing) / 2f
        val lineHeight = bHeight + spacing

        // 注意这里在计算起始x, y时已经考虑了滑动偏移
        var x = offsetX % (bWidth + spacing)
        if (x > 0) x -= (bWidth + spacing)
        var y = offsetY % lineHeight
        if (y > 0) y -= lineHeight

        var line = 0
        while (y < height) {
            var tempX = x + if (line % 2 == 0) 0f else interleaving
            while (tempX < width) {
                tempRectF.set(tempX, y, tempX + bWidth, y + bHeight)
                canvas.drawBitmap(bitmap, null, tempRectF, null)
                tempX += bWidth + spacing
            }
            y += bHeight + spacing
            line++
        }
    }

    private fun drawText(canvas: Canvas) {
        textPaint.getTextBounds(text, 0, text.length, tempRect)
        val textWidth = tempRect.width()
        val textHeight = tempRect.height()

        val lineHeight = textHeight + spacing
        var x = offsetX % (textWidth + spacing)
        if (x > 0) x -= (textWidth + spacing)
        var y = offsetY % lineHeight
        if (y > 0) y -= lineHeight
        while (y < height) {
            var tempX = x
            while (tempX < width) {
                val baseline = y + textHeight / 2f - (textPaint.ascent() + textPaint.descent()) / 2
                canvas.drawText(text, tempX, baseline, textPaint)
                tempX += textWidth + spacing
            }
            y += textHeight + spacing
        }
    }

    fun setBitmap(bitmap: Bitmap) {
        this.bitmap = bitmap
        invalidate()
    }

    fun updateScaleValue(percent: Float) {
        scaleValue = 1f + 9f * percent
        Log.w("sqsong", "scaleValue: $scaleValue")
        invalidate()
    }

}