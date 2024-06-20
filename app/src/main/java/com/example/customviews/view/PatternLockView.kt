package com.example.customviews.view

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import androidx.annotation.IntDef
import androidx.core.animation.addListener
import com.example.customviews.R
import com.example.customviews.utils.VibratorHelper
import com.example.customviews.utils.dp2Px
import com.example.customviews.utils.getThemeColor
import com.example.customviews.utils.getThemeColorWithAlpha
import kotlin.math.min

@IntDef(value = [LockState.STATE_NORMAL, LockState.STATE_CORRECT, LockState.STATE_ERROR])
@Retention(AnnotationRetention.SOURCE)
annotation class LockState {
    companion object {
        const val STATE_NORMAL = 0
        const val STATE_CORRECT = 1
        const val STATE_ERROR = 2
    }
}

interface PatternLockActionListener {
    fun onDrawPatternSuccess(value: String)
}

class PatternLockView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var dotColor: Int = 0
    private var dotRadius: Float = 0f
    private var dotExpandColor: Int = 0
    private var dotExpandRadius: Float = 0f
    private var lineColor: Int = 0
    private var lineWidth: Float = 0f
    private var dotCorrectColor: Int = 0
    private var lineCorrectColor: Int = 0
    private var dotExpandCorrectColor: Int = 0
    private var dotErrorColor: Int = 0
    private var lineErrorColor: Int = 0
    private var dotExpandErrorColor: Int = 0
    private var minValidNumber = 4

    @LockState
    private var lockState: Int = LockState.STATE_NORMAL

    private var isTouchDown = false
    private val lockPath = Path()
    private val drawRect = RectF()
    private var expandFactor = 0f
    private var colorAlphaFactor = 0f
    private val dotRects = mutableListOf<RectF>()
    private val lockNumbers = mutableListOf<Int>()
    private var dotExpandAnimator: ValueAnimator? = null
    private var errorAlphaAnimator: ValueAnimator? = null
    private var actionListener: PatternLockActionListener? = null
    private val vibratorHelper by lazy {
        VibratorHelper(context)
    }

    private val paint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isDither = true
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
    }

    init {
        handleAttributes(context, attrs)
    }

    private fun handleAttributes(context: Context, attrs: AttributeSet?) {
        context.obtainStyledAttributes(attrs, R.styleable.PatternLockView).apply {
            dotColor = getColor(R.styleable.PatternLockView_plv_dotColor, getThemeColor(context, com.google.android.material.R.attr.colorOnSurface))
            dotRadius = getDimension(R.styleable.PatternLockView_plv_dotRadius, dp2Px(8))
            dotExpandColor = getColor(R.styleable.PatternLockView_plv_dotExpandColor, getThemeColorWithAlpha(context, com.google.android.material.R.attr.colorOnSurface, 80))
            dotExpandRadius = getDimension(R.styleable.PatternLockView_plv_dotExpandRadius, dp2Px(28))
            lineColor = getColor(R.styleable.PatternLockView_plv_lineColor, getThemeColorWithAlpha(context, com.google.android.material.R.attr.colorOnSurface, 180))
            lineWidth = getDimension(R.styleable.PatternLockView_plv_lineWidth, dp2Px(6))
            dotCorrectColor = getColor(R.styleable.PatternLockView_plv_dotCorrectColor, getThemeColorWithAlpha(context, com.google.android.material.R.attr.colorPrimary))
            lineCorrectColor = getColor(R.styleable.PatternLockView_plv_lineCorrectColor, getThemeColorWithAlpha(context, com.google.android.material.R.attr.colorPrimary, 180))
            dotExpandCorrectColor = getColor(R.styleable.PatternLockView_plv_dotExpandCorrectColor, getThemeColorWithAlpha(context, com.google.android.material.R.attr.colorPrimary, 150))
            dotErrorColor = getColor(R.styleable.PatternLockView_plv_dotErrorColor, getThemeColorWithAlpha(context, com.google.android.material.R.attr.colorError))
            lineErrorColor = getColor(R.styleable.PatternLockView_plv_lineErrorColor, getThemeColorWithAlpha(context, com.google.android.material.R.attr.colorError, 180))
            dotExpandErrorColor = getColor(R.styleable.PatternLockView_plv_dotExpandErrorColor, getThemeColorWithAlpha(context, com.google.android.material.R.attr.colorError, 150))
            lockState = getInt(R.styleable.PatternLockView_plv_lockState, LockState.STATE_NORMAL)
            minValidNumber = getInt(R.styleable.PatternLockView_plv_minValidNumber, 4)
            recycle()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val minSize = min(w, h)
        drawRect.set(width / 2f - minSize / 2f, height / 2f - minSize / 2f, width / 2f + minSize / 2f, height / 2f + minSize / 2f)
        val gap = (minSize - dotExpandRadius * 2) / 2
        dotRects.clear()
        for (y in 0 until 3) {
            for (x in 0 until 3) {
                val left = drawRect.left + x * gap
                val top = drawRect.top + y * gap
                val right = left + dotExpandRadius * 2
                val bottom = top + dotExpandRadius * 2
                dotRects.add(RectF(left, top, right, bottom))
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawExpandDots(canvas)
        drawDots(canvas)
        drawLines(canvas)
    }

    private fun drawLines(canvas: Canvas) {
        if (lockPath.isEmpty) return
        paint.color = when (lockState) {
            LockState.STATE_NORMAL -> lineColor
            LockState.STATE_CORRECT -> lineCorrectColor
            LockState.STATE_ERROR -> {
                val alpha = (Color.alpha(lineErrorColor) * colorAlphaFactor).toInt()
                Color.argb(alpha, Color.red(lineErrorColor), Color.green(lineErrorColor), Color.blue(lineErrorColor))
            }
            else -> lineColor
        }
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = lineWidth
        canvas.drawPath(lockPath, paint)
    }

    private fun drawDots(canvas: Canvas) {
        paint.style = Paint.Style.FILL
        dotRects.forEachIndexed { index, rect ->
            paint.color = when {
                lockNumbers.contains(index) -> when (lockState) {
                    LockState.STATE_NORMAL -> dotColor
                    LockState.STATE_CORRECT -> dotCorrectColor
                    LockState.STATE_ERROR -> {
                        val alpha = (Color.alpha(dotErrorColor) * colorAlphaFactor).toInt()
                        Color.argb(alpha, Color.red(dotErrorColor), Color.green(dotErrorColor), Color.blue(dotErrorColor))
                    }
                    else -> dotColor
                }
                else -> dotColor
            }
            canvas.drawCircle(rect.centerX(), rect.centerY(), dotRadius, paint)
        }
    }

    private fun drawExpandDots(canvas: Canvas) {
        paint.color = when (lockState) {
            LockState.STATE_NORMAL -> dotExpandColor
            LockState.STATE_CORRECT -> dotExpandCorrectColor
            LockState.STATE_ERROR -> {
                val alpha = (Color.alpha(dotExpandErrorColor) * colorAlphaFactor).toInt()
                Color.argb(alpha, Color.red(dotExpandErrorColor), Color.green(dotExpandErrorColor), Color.blue(dotExpandErrorColor))
            }
            else -> dotExpandColor
        }
        paint.style = Paint.Style.FILL
        lockNumbers.forEachIndexed { index, num ->
            val rect = dotRects[num]
            if (index == lockNumbers.size - 1) {
                canvas.drawCircle(rect.centerX(), rect.centerY(), dotExpandRadius * expandFactor, paint)
            } else {
                canvas.drawCircle(rect.centerX(), rect.centerY(), dotExpandRadius, paint)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                isTouchDown = true
                lockPath.reset()
                lockNumbers.clear()
                lockState = LockState.STATE_NORMAL
                detectTouchPath(event.x, event.y)
            }

            MotionEvent.ACTION_MOVE -> {
                detectTouchPath(event.x, event.y)
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isTouchDown = false
                detectTouchPath(event.x, event.y)
                validLockDots()
            }
        }
        invalidate()
        return true
    }

    private fun validLockDots() {
        if (lockPath.isEmpty) return
        lockState = if (lockNumbers.size < minValidNumber) {
            vibratorHelper.vibrateWaveform()
            startErrorAlphaAnimation()
            LockState.STATE_ERROR
        } else {
            actionListener?.onDrawPatternSuccess(lockNumbers.joinToString(separator = ""))
            LockState.STATE_CORRECT
        }
    }

    private fun detectTouchPath(x: Float, y: Float) {
        dotRects.forEachIndexed { index, rect ->
            if (rect.contains(x, y) && !lockNumbers.contains(index)) {
                if (lockNumbers.isNotEmpty()) {
                    val lastNumber = lockNumbers.last()
                    val lastRect = dotRects[lastNumber]
                    // 计算中点
                    val midX = (rect.centerX() + lastRect.centerX()) / 2
                    val midY = (rect.centerY() + lastRect.centerY()) / 2

                    // 检查中点是否在未包含的点集合中
                    dotRects.forEachIndexed { midIndex, midRect ->
                        if (midRect.contains(midX, midY) && !lockNumbers.contains(midIndex)) {
                            lockNumbers.add(midIndex)
                            startExpandAnimation()
                            vibratorHelper.vibrate()
                        }
                    }
                }
                lockNumbers.add(index)
                startExpandAnimation()
                vibratorHelper.vibrate()
            }
        }

        lockPath.reset()

        if (lockNumbers.isEmpty()) return
        lockNumbers.forEachIndexed { index, number ->
            val rect = dotRects[number]
            if (index == 0) {
                lockPath.moveTo(rect.centerX(), rect.centerY())
            } else {
                lockPath.lineTo(rect.centerX(), rect.centerY())
            }
        }
        // 手指抬起则只绘制到最后一个点
        if (isTouchDown) {
            lockPath.lineTo(x, y)
        }
    }

    private fun startExpandAnimation() {
        dotExpandAnimator?.cancel()
        dotExpandAnimator = ObjectAnimator.ofFloat(0f, 1f).apply {
            duration = 200L
            interpolator = OvershootInterpolator()
            addUpdateListener {
                expandFactor = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun startErrorAlphaAnimation() {
        errorAlphaAnimator?.cancel()
        errorAlphaAnimator = ObjectAnimator.ofFloat(0.3f, 1f).apply {
            duration = 200L
            interpolator = LinearInterpolator()
            repeatCount = 2
             repeatMode = ValueAnimator.REVERSE
            addUpdateListener {
                colorAlphaFactor = it.animatedValue as Float
                invalidate()
            }
            addListener(onEnd = {
                colorAlphaFactor = 1f
                invalidate()
            })
            start()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        dotExpandAnimator?.cancel()
        dotExpandAnimator = null
        errorAlphaAnimator?.cancel()
        errorAlphaAnimator = null
    }

    fun setPatternLockActionListener(listener: PatternLockActionListener) {
        this.actionListener = listener
    }
}