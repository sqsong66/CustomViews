package com.example.customviews.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import com.example.customviews.R
import com.example.customviews.utils.dp2Px
import com.example.customviews.utils.getThemeColor

class AiTemplateView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : AppCompatImageView(context, attrs, defStyle) {

    private var borderWidth = 0f
    private var cornerRadius = 0f
    private var isChecked = false
    private var borderColor = 0

    private val tempRect = RectF()
    private val clipPath = Path()

    private val paint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = borderWidth
            color = borderColor
        }
    }

    init {
        handleAttributes(context, attrs)
    }

    private fun handleAttributes(context: Context, attrs: AttributeSet?) {
        context.obtainStyledAttributes(attrs, R.styleable.AiTemplateView).apply {
            borderWidth = getDimension(R.styleable.AiTemplateView_atv_borderWidth, dp2Px(2))
            cornerRadius = getDimension(R.styleable.AiTemplateView_atv_cornerRadius, dp2Px(16))
            borderColor = getColor(R.styleable.AiTemplateView_atv_borderColor, getThemeColor(context, com.google.android.material.R.attr.colorPrimary))
            isChecked = getBoolean(R.styleable.AiTemplateView_atv_checked, false)
            recycle()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        clipPath.reset()
        tempRect.set(0f, 0f, w.toFloat(), h.toFloat())
        clipPath.addRoundRect(tempRect, cornerRadius, cornerRadius, Path.Direction.CW)
    }

    override fun onDraw(canvas: Canvas) {
        canvas.clipPath(clipPath)
        super.onDraw(canvas)
        if (isChecked) {
            tempRect.set(0f, 0f, width.toFloat(), height.toFloat())
            tempRect.inset(borderWidth / 2, borderWidth / 2)
            canvas.drawRoundRect(tempRect, cornerRadius, cornerRadius, paint)
        }
    }

    fun setChecked(checked: Boolean) {
        isChecked = checked
        invalidate()
    }

}