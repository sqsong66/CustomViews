package com.example.customviews.view

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.animation.doOnEnd
import com.example.customviews.R
import com.example.customviews.utils.VibratorHelper
import com.example.customviews.utils.dp2Px
import com.example.customviews.utils.getThemeColor
import com.example.customviews.utils.getThemeColorWithAlpha
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

data class MosaicSize(val circleX: Float, val startX: Float, val endX: Float)

interface OnMosaicSizeProgressListener {
    fun onSizeProgressChanged(progress: Float, @TouchType touchType: Int)
}

class MosaicSizeView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : View(context, attrs, defStyle) {

    private var lineWidth = 0f
    private var lineColor = 0
    private var stepColor = 0
    private var stepCount = 0
    private var circleSize = 0f
    private var minCircleSize = 0f
    private var maxCircleSize = 0f

    private var maxRadius = 0f
    private var stepXCoord = 0f
    private var isInCircleMode = false
    private val mosaicSizeList = mutableListOf<MosaicSize>()
    private var onMosaicSizeProgressListener: OnMosaicSizeProgressListener? = null

    private val paint by lazy {
        Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            strokeWidth = lineWidth
        }
    }

    private val vibratorHelper by lazy {
        VibratorHelper(context)
    }

    init {
        handleAttributes(context, attrs)
    }

    private fun handleAttributes(context: Context, attrs: AttributeSet?) {
        context.obtainStyledAttributes(attrs, R.styleable.MosaicSizeView).apply {
            lineWidth = getDimension(R.styleable.MosaicSizeView_msv_lineWidth, dp2Px(2))
            lineColor = getColor(R.styleable.MosaicSizeView_msv_lineColor, getThemeColorWithAlpha(context, com.google.android.material.R.attr.colorOnSurface, 100))
            stepColor = getColor(R.styleable.MosaicSizeView_msv_stepColor, getThemeColor(context, com.google.android.material.R.attr.colorPrimary))
            stepCount = getInt(R.styleable.MosaicSizeView_msv_stepCount, 5)
            maxCircleSize = getDimension(R.styleable.MosaicSizeView_msv_maxCircleSize, dp2Px(20))
            minCircleSize = getDimension(R.styleable.MosaicSizeView_msv_minCircleSize, dp2Px(6))
            circleSize = getDimension(R.styleable.MosaicSizeView_msv_circleSize, dp2Px(16))
            recycle()
        }
        if (minCircleSize > maxCircleSize) {
            throw IllegalArgumentException("minCircleSize must be less than maxCircleSize")
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val measuredWidth = MeasureSpec.getSize(widthMeasureSpec)
        val measuredHeight = ceil(max(circleSize, maxCircleSize)).toInt()
        setMeasuredDimension(measuredWidth, measuredHeight)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        maxRadius = max(circleSize, maxCircleSize) / 2f
        stepXCoord = max(maxRadius, min(stepXCoord, width - maxRadius))

        mosaicSizeList.clear()
        repeat(stepCount) { index ->
            val radius = minCircleSize / 2f
            val circleX = maxRadius + (width - maxRadius * 2) / (stepCount - 1) * index
            mosaicSizeList.add(MosaicSize(circleX, circleX - radius, circleX + radius))
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawLineCircle(canvas)
    }

    private fun drawLineCircle(canvas: Canvas) {
        val maxSize = max(circleSize, maxCircleSize)
        paint.color = lineColor
        val start = maxSize / 2f
        val stepCircleSize = (maxCircleSize - minCircleSize) / stepCount
        val stepWidth = (width - maxSize) / (stepCount - 1)
        repeat(stepCount) { index ->
            val radius = (minCircleSize + stepCircleSize * index) / 2f
            val nextRadius = (minCircleSize + stepCircleSize * (index + 1)) / 2f
            val circleX = start + stepWidth * index
            val nextCircleX = start + stepWidth * (index + 1)

            // draw circle
            paint.color = if (circleX <= stepXCoord) stepColor else lineColor
            canvas.drawCircle(circleX, height / 2f, radius, paint)

            // draw line
            val lineStart = circleX
            val lineEnd = lineStart + stepWidth
            when {
                stepXCoord < lineStart && index < stepCount - 1 -> {
                    paint.color = lineColor
                    val startX = lineStart + radius
                    val endX = nextCircleX - nextRadius
                    canvas.drawLine(startX, height / 2f, endX, height / 2f, paint)
                }

                stepXCoord in lineStart..lineEnd && index < stepCount - 1 -> {
                    paint.color = stepColor
                    val startX = lineStart + radius
                    canvas.drawLine(startX, height / 2f, stepXCoord, height / 2f, paint)
                    paint.color = lineColor
                    val endX = nextCircleX - nextRadius
                    canvas.drawLine(stepXCoord, height / 2f, endX, height / 2f, paint)
                }

                stepXCoord > lineEnd && index < stepCount - 1 -> {
                    paint.color = stepColor
                    val startX = lineStart + radius
                    val endX = nextCircleX - nextRadius
                    canvas.drawLine(startX, height / 2f, endX, height / 2f, paint)
                }
            }

            // draw indicator circle
            paint.color = stepColor
            canvas.drawCircle(stepXCoord, height / 2f, circleSize / 2f, paint)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> onMove(event, TouchType.TOUCH_DOWN)
            MotionEvent.ACTION_MOVE -> onMove(event, TouchType.TOUCH_MOVE)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> onUp(event)
        }
        return true
    }

    private fun onMove(event: MotionEvent, @TouchType touchType: Int) {
        val touchX = max(maxRadius, min(event.x, width - maxRadius))
        stepXCoord = findPrefectXCoord(touchX)
        updateProgress()
        invalidate()
    }

    private fun onUp(event: MotionEvent) {
        val touchX = max(maxRadius, min(event.x, width - maxRadius))
        // 查找距离最近的圆心
        ValueAnimator.ofFloat(stepXCoord, findNearestXCoord(touchX)).apply {
            duration = 100
            addUpdateListener {
                stepXCoord = it.animatedValue as Float
                updateProgress()
                invalidate()
            }
            addListener(doOnEnd {
                updateProgress()
            })
            start()
        }
    }

    private fun updateProgress() {
        val progress = (stepXCoord - maxRadius) / (width - maxRadius * 2)
        onMosaicSizeProgressListener?.onSizeProgressChanged(progress, TouchType.TOUCH_UP)
    }

    private fun findNearestXCoord(x: Float): Float {
        var nearestX = maxRadius
        var minDistance = Float.MAX_VALUE
        mosaicSizeList.forEach { size ->
            val distance = abs(size.circleX - x)
            if (distance < minDistance) {
                minDistance = distance
                nearestX = size.circleX
            }
        }
        return nearestX
    }

    private fun findPrefectXCoord(x: Float): Float {
        mosaicSizeList.forEach { size ->
            if (x in size.startX..size.endX) {
                stepXCoord = size.circleX
                if (!isInCircleMode) {
                    isInCircleMode = true
                    vibratorHelper.vibrate()
                }
                return size.circleX
            }
        }
        isInCircleMode = false
        return x
    }

    fun setOnMosaicSizeProgressListener(listener: OnMosaicSizeProgressListener) {
        onMosaicSizeProgressListener = listener
    }
}