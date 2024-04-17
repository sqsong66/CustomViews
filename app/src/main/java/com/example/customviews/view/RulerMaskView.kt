package com.example.customviews.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View
import com.example.customviews.R
import com.example.customviews.utils.dp2Px
import com.example.customviews.utils.getThemeColor
import com.example.customviews.utils.getThemeColorWithAlpha
import com.example.customviews.utils.screenWidth

class RulerMaskView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var maskColor: Int = 0
    private var radius: Float = 0f
    private var borderWidth: Float = 0f
    private var borderColor: Int = 0
    private var borderGap: Float = 0f
    private var valueTextSize: Float = 0f
    private var textColor: Int = 0

    private var currentValue = 0

    private val paint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isDither = true
        }
    }

    private val textPaint by lazy {
        TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            isDither = true
            textSize = valueTextSize
            color = textColor
        }
    }

    init {
        handleAttributes(context, attrs)
    }

    private fun handleAttributes(context: Context, attrs: AttributeSet?) {
        context.obtainStyledAttributes(attrs, R.styleable.RulerMaskView).apply {
            maskColor = getColor(R.styleable.RulerMaskView_rmv_maskColor, getThemeColorWithAlpha(context, com.google.android.material.R.attr.colorPrimary, 100))
            val itemSize = (screenWidth - dp2Px<Int>(16) * 4) / 5 / 2f
            radius = getDimension(R.styleable.RulerMaskView_rmv_radius, itemSize)
            borderWidth = getDimension(R.styleable.RulerMaskView_rmv_borderWidth, dp2Px(2))
            borderColor = getColor(R.styleable.RulerMaskView_rmv_borderColor, getThemeColor(context, com.google.android.material.R.attr.colorPrimary))
            borderGap = getDimension(R.styleable.RulerMaskView_rmv_borderGap, dp2Px(2))
            valueTextSize = getDimension(R.styleable.RulerMaskView_rmv_textSize, dp2Px(16))
            textColor = getColor(R.styleable.RulerMaskView_rmv_textColor, Color.WHITE)
            recycle()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val size = (radius + borderWidth + borderGap).toInt() * 2
        setMeasuredDimension(size, size)
    }

    override fun onDraw(canvas: Canvas) {
        drawCircle(canvas)
        drawText(canvas)
    }

    private fun drawText(canvas: Canvas) {
        val text = currentValue.toString()
        val textWidth = textPaint.measureText(text)
        val baseline = height / 2f - (textPaint.descent() + textPaint.ascent()) / 2
        canvas.drawText(text, width / 2f - textWidth / 2, baseline, textPaint)
    }

    private fun drawCircle(canvas: Canvas) {
        paint.style = Paint.Style.FILL
        paint.color = maskColor
        canvas.drawCircle(width / 2f, height / 2f, radius, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = borderWidth
        paint.color = borderColor
        canvas.drawCircle(width / 2f, height / 2f, radius + borderGap, paint)
    }

    fun setCurrentValue(value: Int) {
        currentValue = value
        invalidate()
    }
}