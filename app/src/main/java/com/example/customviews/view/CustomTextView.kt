package com.example.customviews.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import com.example.customviews.utils.dp2Px

class CustomTextView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : View(context, attrs, defStyle) {

    private val text = "abcdefghijk"
    private val textSize = dp2Px<Float>(14)
    private val bounds = Rect()

    private val paint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isDither = true
            this.textSize = this@CustomTextView.textSize
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        paint.getTextBounds(text, 0, text.length, bounds)
        val fontMetrics = paint.fontMetrics
        // 计算需要的高度，考虑ascender和descender
        val desiredHeight = (fontMetrics.bottom - fontMetrics.top).toInt()

        val width = bounds.width()
        val height = bounds.height()
        setMeasuredDimension(width, desiredHeight)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        paint.color = Color.RED
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

        paint.color = Color.WHITE
        val baseline = height / 2f - (paint.ascent() + paint.descent()) / 2f
        canvas.drawText(text, 0f, baseline, paint)
    }
}