package com.example.customviews.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.text.TextPaint
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.widget.OverScroller
import com.example.customviews.R
import com.example.customviews.utils.VibratorHelper
import com.example.customviews.utils.dp2Px
import com.example.customviews.utils.getThemeColor
import com.example.customviews.utils.getThemeColorWithAlpha
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

class RulerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    interface OnValueChangeListener {
        fun onValueChange(value: Float)
    }

    private var calibrationWidth: Float = 0f
    private var calibrationLength: Float = 0f
    private var calibrationColor: Int = 0
    private var stepCalibrationColor: Int = 0
    private var indicatorCalibrationWidth: Float = 0f
    private var indicatorCalibrationLength: Float = 0f
    private var indicatorCalibrationColor: Int = 0
    private var calibrationTextSize: Float = 0f
    private var calibrationTextColor: Int = 0
    private var indicatorTextGap: Float = 0f
    private var minValue: Float = 0f
    private var maxValue: Float = 0f
    private var currentValue: Float = 0f
    private var calibrationUnit: Float = 0f
    private var numberPerStep: Int = 0
    private var calibrationGap: Float = 0f
    private var textMargin: Float = 0f

    private var minNumber = 0
    private var maxNumber = 0
    private var currentNumber = 0
    private var numberUnit = 0
    private var currentDistance = 0f
    private var totalDistance = 0f
    private var widthRangeNumber = 0

    private var lastX = 0f
    private var lastY = 0f
    private var downX = 0f
    private var isTouched = false
    private var minFlingVelocity = 0
    private var maxFlingVelocity = 0
    private val tempRect = Rect()
    private var currentProgress: Int = 0
    private var drawCalibrationNumberList = mutableListOf<Int>()
    private var onValueChangeListener: OnValueChangeListener? = null
    private val scroller by lazy {
        OverScroller(context)
    }
    private val velocityTracker by lazy {
        VelocityTracker.obtain()
    }
    private val vibratorHelper by lazy {
        VibratorHelper(context)
    }

    private val paint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isDither = true
            strokeWidth = calibrationWidth
        }
    }

    private val textPaint by lazy {
        TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            isDither = true
            textSize = calibrationTextSize
            color = calibrationTextColor
        }
    }

    init {
        handleAttributes(context, attrs)
        convertValueToNumber()

        val viewConfiguration = ViewConfiguration.get(context)
        minFlingVelocity = viewConfiguration.scaledMinimumFlingVelocity
        maxFlingVelocity = viewConfiguration.scaledMaximumFlingVelocity
    }

    private fun handleAttributes(context: Context, attrs: AttributeSet?) {
        context.obtainStyledAttributes(attrs, R.styleable.RulerView).apply {
            calibrationWidth = getDimension(R.styleable.RulerView_rv_calibrationWidth, dp2Px(1.5f))
            calibrationLength = getDimension(R.styleable.RulerView_rv_calibrationLength, dp2Px(14))
            calibrationColor = getColor(R.styleable.RulerView_rv_calibrationColor, getThemeColorWithAlpha(context, com.google.android.material.R.attr.colorOnSurface, 100))
            stepCalibrationColor = getColor(R.styleable.RulerView_rv_stepCalibrationColor, getThemeColorWithAlpha(context, com.google.android.material.R.attr.colorOnSurface, 200))
            indicatorCalibrationWidth = getDimension(R.styleable.RulerView_rv_indicatorCalibrationWidth, calibrationWidth * 1.5f)
            indicatorCalibrationLength = getDimension(R.styleable.RulerView_rv_indicatorCalibrationLength, calibrationLength * 2)
            indicatorCalibrationColor = getColor(R.styleable.RulerView_rv_indicatorCalibrationColor, getThemeColor(context, com.google.android.material.R.attr.colorPrimary))
            calibrationTextSize = getDimension(R.styleable.RulerView_rv_calibrationTextSize, dp2Px(12))
            calibrationTextColor = getColor(R.styleable.RulerView_rv_calibrationTextColor, getThemeColorWithAlpha(context, com.google.android.material.R.attr.colorOnSurface, 200))
            indicatorTextGap = getDimension(R.styleable.RulerView_rv_indicatorTextGap, dp2Px(8))
            minValue = getFloat(R.styleable.RulerView_rv_minValue, 0f)
            maxValue = getFloat(R.styleable.RulerView_rv_maxValue, 100f)
            currentValue = getFloat(R.styleable.RulerView_rv_defaultValue, 0f)
            calibrationUnit = getFloat(R.styleable.RulerView_rv_calibrationUnit, 2f)
            numberPerStep = getInt(R.styleable.RulerView_rv_numberPerStep, 10)
            calibrationGap = getDimension(R.styleable.RulerView_rv_calibrationGap, dp2Px(10))
            textMargin = getDimension(R.styleable.RulerView_rv_textMargin, dp2Px(16))
            recycle()
        }
    }

    private fun convertValueToNumber() {
        minNumber = minValue.toInt() * 10
        maxNumber = maxValue.toInt() * 10
        currentNumber = currentValue.toInt() * 10
        numberUnit = (calibrationUnit * 10).toInt()
        currentProgress = (currentNumber - minNumber) / numberUnit

        for (i in minNumber .. maxNumber step numberUnit) {
            drawCalibrationNumberList.add(i)
        }

        currentDistance = (currentNumber - minNumber).toFloat() / numberUnit * calibrationGap
        totalDistance = (maxNumber - minNumber).toFloat() / numberUnit * calibrationGap
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val measuredWidth = MeasureSpec.getSize(widthMeasureSpec)
        textPaint.getTextBounds("0", 0, 1, tempRect)
        val measuredHeight = ceil(indicatorCalibrationLength + indicatorTextGap).toInt() + tempRect.height() + paddingTop + paddingBottom
        setMeasuredDimension(measuredWidth, measuredHeight)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        widthRangeNumber = (w / calibrationGap).toInt() * numberUnit
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        velocityTracker.addMovement(event)
        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                isTouched = true
                downX = event.x
                scroller.forceFinished(true)
            }

            MotionEvent.ACTION_MOVE -> {
                var dx = event.x - lastX
                if (currentDistance < 0 || currentDistance > totalDistance) {
                    dx *= 0.3f
                }
                currentDistance -= dx
                calculateValues()
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                velocityTracker.computeCurrentVelocity(1000, maxFlingVelocity.toFloat())
                val velocityX = velocityTracker.xVelocity
                if (currentDistance < 0 || currentDistance > totalDistance) {
                    // 手动开始快速回弹
                    val startX = currentDistance.toInt()
                    val endX = if (currentDistance < 0) 0 else totalDistance.toInt()
                    scroller.startScroll(startX, 0, endX - startX, 0, 500)  // 回弹速度快，时间短
                } else
                    if (abs(velocityX) > minFlingVelocity) {
                        val maxOffset = width / 5
                        val speed = -(velocityX * 0.6f).toInt()
                        scroller.fling(
                            currentDistance.toInt(), 0,
                            speed, 0,
                            -maxOffset, totalDistance.toInt() + maxOffset,
                            0, 0
                        )
                    }

                velocityTracker.clear()
                postInvalidateOnAnimation()
                isTouched = false
            }
        }
        lastX = event.x
        lastY = event.y
        return true
    }

    override fun computeScroll() {
        if (scroller.computeScrollOffset()) {
            currentDistance = scroller.currX.toFloat()
            calculateValues()
        } else {
            if ((currentDistance < 0 || currentDistance > totalDistance) && !isTouched) {
                val startDistance = currentDistance
                val endDistance = if (currentDistance < 0) 0 else totalDistance.toInt()
                scroller.startScroll(
                    startDistance.toInt(), 0,
                    (endDistance - startDistance).toInt(), 0,
                    500 // 可调整的回弹时间，以毫秒为单位
                )
                postInvalidateOnAnimation()
            }
        }
    }

    private fun calculateValues() {
        currentNumber = minNumber + ((currentDistance / calibrationGap) * numberUnit).toInt()
        currentValue = min(maxValue, max(minValue, currentNumber.toFloat() / 10))
        val value = round(currentValue).toInt()
        if (value != currentProgress) {
            currentProgress = value
            if (currentProgress % calibrationUnit.toInt() == 0) {
                vibratorHelper.vibrate()
            }
            onValueChangeListener?.onValueChange(currentProgress.toFloat())
            Log.d("RulerView", "currentProgress: $currentProgress")
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawCalibration(canvas)
        drawIndicator(canvas)
    }

    private fun drawCalibration(canvas: Canvas) {
        val halfWidth = width / 2f
        var startNum: Int = (currentDistance - halfWidth).toInt() / calibrationGap.toInt() * numberUnit + minNumber
        val expandUnit = numberUnit shl 1
        startNum -= expandUnit
        if (startNum < minNumber) {
            startNum = minNumber
        }

        var endNum: Int = startNum + expandUnit + widthRangeNumber + expandUnit
        if (endNum > maxNumber) {
            endNum = maxNumber
        }
        var distance = halfWidth - (currentDistance - (startNum - minNumber).toFloat() / numberUnit * calibrationGap)
        val perUnitCount: Int = numberPerStep * numberUnit
        paint.strokeWidth = calibrationWidth
        val startY = (indicatorCalibrationLength - calibrationLength) / 2f
        val stopY = startY + calibrationLength
        var isMinTextDraw = false
        var isMaxTextDraw = false

        while (startNum <= endNum) {
            if ((startNum - minNumber) % perUnitCount == 0) {
                paint.color = stepCalibrationColor
                canvas.drawLine(distance, startY, distance, stopY, paint)
                val number = startNum / 10
                if (number == minNumber / 10 || number == maxNumber / 10) {
                    val text = number.toString()
                    textPaint.getTextBounds(text, 0, text.length, tempRect)
                    val x = distance - tempRect.width() / 2f
                    val baseline = height - tempRect.height() / 2f - (textPaint.descent() + textPaint.ascent()) / 2

                    if (x > textMargin && number == minNumber / 10) {
                        canvas.drawText(text, x, baseline, textPaint)
                        isMinTextDraw = true
                    }
                    if (x < width - textMargin - tempRect.width() && number == maxNumber / 10) {
                        canvas.drawText(text, x, baseline, textPaint)
                        isMaxTextDraw = true
                    }
                }
            } else {
                paint.color = calibrationColor
                canvas.drawLine(distance, startY, distance, stopY, paint)
            }
            startNum += numberUnit
            distance += calibrationGap
        }

        if (!isMinTextDraw) {
            val text = (minNumber / 10).toString()
            textPaint.getTextBounds(text, 0, text.length, tempRect)
            val x = textMargin
            val baseline = height - tempRect.height() / 2f - (textPaint.descent() + textPaint.ascent()) / 2
            canvas.drawText(text, x, baseline, textPaint)
        }
        if (!isMaxTextDraw) {
            val text = (maxNumber / 10).toString()
            textPaint.getTextBounds(text, 0, text.length, tempRect)
            val x = width - textMargin - tempRect.width()
            val baseline = height - tempRect.height() / 2f - (textPaint.descent() + textPaint.ascent()) / 2
            canvas.drawText(text, x, baseline, textPaint)
        }
    }

    private fun drawIndicator(canvas: Canvas) {
        paint.color = indicatorCalibrationColor
        paint.strokeWidth = indicatorCalibrationWidth
        val halfWidth = width / 2f
        canvas.drawLine(halfWidth, 0f, halfWidth, indicatorCalibrationLength, paint)
    }

    fun setOnValueChangeListener(listener: OnValueChangeListener) {
        this.onValueChangeListener = listener
    }

    fun setCurrentValue(value: Float) {
        currentValue = value
        convertValueToNumber()
        postInvalidate()
    }

}