package com.example.customviews.view

import android.content.Context
import android.graphics.Paint
import android.text.method.MovementMethod
import android.util.AttributeSet
import android.util.Log
import androidx.appcompat.widget.AppCompatEditText
import com.example.customviews.utils.dp2Px
import com.example.customviews.utils.getThemeColor
import com.example.customviews.utils.getThemeColorWithAlpha
import kotlin.math.ceil

class PinPasswordEditText @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : AppCompatEditText(context, attrs, defStyleAttr) {

    private var maxPasswordLength = 4
    private var passwordCircleRadius = 0f
    private var passwordCircleStrokeWidth = 0f
    private var passwordCircleStrokeColor = 0
    private var passwordCircleColor = 0
    private var passwordCircleSolidColor = 0
    private var passwordCircleGap = 0f

    private val paint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isDither = true
            strokeWidth = passwordCircleStrokeWidth
        }
    }

    init {
        handleAttributes(context, attrs)
        initParams()
    }

    private fun handleAttributes(context: Context, attrs: AttributeSet?) {
        context.obtainStyledAttributes(attrs, com.example.customviews.R.styleable.PinPasswordEditText).apply {
            maxPasswordLength = getInt(com.example.customviews.R.styleable.PinPasswordEditText_ppet_maxPasswordLength, 6)
            passwordCircleRadius = getDimension(com.example.customviews.R.styleable.PinPasswordEditText_ppet_passwordCircleRadius, dp2Px(5))
            passwordCircleStrokeWidth = getDimension(com.example.customviews.R.styleable.PinPasswordEditText_ppet_passwordCircleStrokeWidth, dp2Px(1.5f))
            passwordCircleStrokeColor = getColor(com.example.customviews.R.styleable.PinPasswordEditText_ppet_passwordCircleStrokeColor, getThemeColorWithAlpha(context, com.google.android.material.R.attr.colorOnSurface, 180))
            passwordCircleColor = getColor(com.example.customviews.R.styleable.PinPasswordEditText_ppet_passwordCircleColor, getThemeColorWithAlpha(context, com.google.android.material.R.attr.colorOnSurface, 255))
            passwordCircleSolidColor = getColor(com.example.customviews.R.styleable.PinPasswordEditText_ppet_passwordCircleSoldColor, getThemeColor(context, com.google.android.material.R.attr.colorOnSurface))
            passwordCircleGap = getDimension(com.example.customviews.R.styleable.PinPasswordEditText_ppet_passwordCircleGap, dp2Px(12))
            recycle()
        }
    }

    private fun initParams() {
        background = null
        setWillNotDraw(false)
        isCursorVisible = false
        setTextIsSelectable(false)
        inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
        filters = arrayOf(android.text.InputFilter.LengthFilter(maxPasswordLength))
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val w = ceil(paddingStart + paddingEnd + (maxPasswordLength * passwordCircleRadius * 2) + ((maxPasswordLength - 1) * passwordCircleGap)).toInt()
        val h = ceil(paddingTop + paddingBottom + (passwordCircleRadius * 2)).toInt()
        setMeasuredDimension(w, h)
    }

    override fun onDraw(canvas: android.graphics.Canvas) {
        val startX = paddingStart + passwordCircleRadius
        val cy = paddingTop + passwordCircleRadius
        val textLength = text?.length ?: 0
        for (i in 0 until maxPasswordLength) {
            val radius: Float
            if (i < textLength) {
                paint.style = Paint.Style.FILL
                paint.color = passwordCircleColor
                radius = passwordCircleRadius
            } else {
                paint.style = Paint.Style.STROKE
                paint.color = passwordCircleStrokeColor
                radius = passwordCircleRadius - passwordCircleStrokeWidth / 2f
            }
            val cx = startX + (i * (passwordCircleRadius * 2 + passwordCircleGap))
            canvas.drawCircle(cx, cy, radius, paint)
        }
    }

    override fun getDefaultMovementMethod(): MovementMethod? {
        return null
    }
}