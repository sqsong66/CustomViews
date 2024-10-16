package com.example.customviews.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.VectorDrawable
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import com.example.customviews.R
import com.example.customviews.utils.dp2Px
import com.example.customviews.utils.getThemeColor
import kotlin.math.ceil

class FingerprintScanView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var lockSize: Float = 0f
    private var lockColor: Int = 0
    private var lockBgColor: Int = 0
    private var lockPadding: Float = 0f
    private var fingerprintColor: Int = 0
    private var fingerprintSize: Float = 0f
    private var outerStrokeWidth: Float = 0f
    private var outerStrokeColor: Int = 0
    private var outerStrokeRadius: Float = 0f
    private var iconOuterMargin: Float = 0f
    private var outerStrokeLength: Float = 0f

    private var outInsets = 0f
    private var outAlpha = 255
    private val tempPath = Path()
    private val tempRectF = RectF()
    private val outerRectF = RectF()
    private var scaleAnimator: ValueAnimator? = null
    private var lockDrawable: VectorDrawable? = null
    private var fingerprintDrawable: VectorDrawable? = null

    private val paint = Paint().apply {
        isAntiAlias = true
    }

    init {
        handleAttributes(context, attrs)
    }

    private fun handleAttributes(context: Context, attrs: AttributeSet?) {
        context.obtainStyledAttributes(attrs, R.styleable.FingerprintScanView).apply {
            val fingerprintRes = getResourceId(R.styleable.FingerprintScanView_fsv_fingerprintRes, R.drawable.ic_fingerprint_bold)
            val lockRes = getResourceId(R.styleable.FingerprintScanView_fsv_lockRes, R.drawable.ic_lock_fill)
            fingerprintSize = getDimension(R.styleable.FingerprintScanView_fsv_fingerprintSize, dp2Px(80))
            lockSize = getDimension(R.styleable.FingerprintScanView_fsv_lockSize, dp2Px(24))
            fingerprintColor = getColor(R.styleable.FingerprintScanView_fsv_fingerprintColor, getThemeColor(context, com.google.android.material.R.attr.colorPrimary))
            lockColor = getColor(R.styleable.FingerprintScanView_fsv_lockColor, getThemeColor(context, com.google.android.material.R.attr.colorOnSurface))
            lockBgColor = getColor(R.styleable.FingerprintScanView_fsv_lockBgColor, getThemeColor(context, com.google.android.material.R.attr.colorSurface))
            lockPadding = getDimension(R.styleable.FingerprintScanView_fsv_lockPadding, dp2Px(4))
            outerStrokeWidth = getDimension(R.styleable.FingerprintScanView_fsv_outerStrokeWidth, dp2Px(6))
            outerStrokeColor = getColor(R.styleable.FingerprintScanView_fsv_outerStrokeColor, getThemeColor(context, com.google.android.material.R.attr.colorSecondary))
            outerStrokeRadius = getDimension(R.styleable.FingerprintScanView_fsv_outerStrokeRadius, dp2Px(16))
            iconOuterMargin = getDimension(R.styleable.FingerprintScanView_fsv_iconOuterMargin, dp2Px(16))
            outerStrokeLength = getDimension(R.styleable.FingerprintScanView_fsv_outerStrokeLength, dp2Px(28))
            fingerprintDrawable = (AppCompatResources.getDrawable(context, fingerprintRes) as VectorDrawable).apply {
                setTint(fingerprintColor)
            }
            lockDrawable = (AppCompatResources.getDrawable(context, lockRes) as VectorDrawable).apply {
                setTint(lockColor)
            }
            recycle()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val size = ceil(fingerprintSize + iconOuterMargin * 2 + outerStrokeWidth * 2)
        setMeasuredDimension(size.toInt(), size.toInt())
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        outerRectF.set(outerStrokeWidth, outerStrokeWidth, w - outerStrokeWidth, h - outerStrokeWidth)
        tempRectF.set(w / 2f - fingerprintSize / 2, h / 2f - fingerprintSize / 2, w / 2f + fingerprintSize / 2, h / 2f + fingerprintSize / 2)
        fingerprintDrawable?.setBounds(tempRectF.left.toInt(), tempRectF.top.toInt(), tempRectF.right.toInt(), tempRectF.bottom.toInt())
        val lockCenterX = tempRectF.right - lockPadding - lockSize / 2
        val lockCenterY = tempRectF.bottom - lockPadding - lockSize / 2
        tempRectF.set(lockCenterX - lockSize / 2, lockCenterY - lockSize / 2, lockCenterX + lockSize / 2, lockCenterY + lockSize / 2)
        lockDrawable?.setBounds(tempRectF.left.toInt(), tempRectF.top.toInt(), tempRectF.right.toInt(), tempRectF.bottom.toInt())
        startScaleAlphaAnim()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        fingerprintDrawable?.draw(canvas)
        drawLock(canvas)
        drawOuter(canvas)
    }

    private fun drawLock(canvas: Canvas) {
        tempRectF.set(width / 2f - fingerprintSize / 2, height / 2f - fingerprintSize / 2, width / 2f + fingerprintSize / 2, height / 2f + fingerprintSize / 2)
        val lockCenterX = tempRectF.right - lockPadding - lockSize / 2
        val lockCenterY = tempRectF.bottom - lockPadding - lockSize / 2
        val radius = lockSize / 2 + lockPadding
        paint.color = lockBgColor
        paint.style = Paint.Style.FILL
        paint.alpha = 255
        canvas.drawCircle(lockCenterX, lockCenterY, radius, paint)
        lockDrawable?.draw(canvas)
    }

    private fun drawOuter(canvas: Canvas) {
        paint.color = outerStrokeColor
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = outerStrokeWidth
        paint.strokeCap = Paint.Cap.ROUND
        paint.alpha = outAlpha
        tempRectF.set(outerRectF)
        tempRectF.inset(outInsets, outInsets)
        // 在tempRectF的四个角绘制
        // 左上角
        tempPath.reset()
        tempPath.moveTo(tempRectF.left, tempRectF.top + outerStrokeLength)
        tempPath.lineTo(tempRectF.left, tempRectF.top + outerStrokeRadius)
        tempPath.quadTo(tempRectF.left, tempRectF.top, tempRectF.left + outerStrokeRadius, tempRectF.top)
        tempPath.lineTo(tempRectF.left + outerStrokeLength, tempRectF.top)
        canvas.drawPath(tempPath, paint)
        // 右上角
        tempPath.reset()
        tempPath.moveTo(tempRectF.right - outerStrokeLength, tempRectF.top)
        tempPath.lineTo(tempRectF.right - outerStrokeRadius, tempRectF.top)
        tempPath.quadTo(tempRectF.right, tempRectF.top, tempRectF.right, tempRectF.top + outerStrokeRadius)
        tempPath.lineTo(tempRectF.right, tempRectF.top + outerStrokeLength)
        canvas.drawPath(tempPath, paint)

        // 右下角
        tempPath.reset()
        tempPath.moveTo(tempRectF.right, tempRectF.bottom - outerStrokeLength)
        tempPath.lineTo(tempRectF.right, tempRectF.bottom - outerStrokeRadius)
        tempPath.quadTo(tempRectF.right, tempRectF.bottom, tempRectF.right - outerStrokeRadius, tempRectF.bottom)
        tempPath.lineTo(tempRectF.right - outerStrokeLength, tempRectF.bottom)
        canvas.drawPath(tempPath, paint)

        // 左下角
        tempPath.reset()
        tempPath.moveTo(tempRectF.left + outerStrokeLength, tempRectF.bottom)
        tempPath.lineTo(tempRectF.left + outerStrokeRadius, tempRectF.bottom)
        tempPath.quadTo(tempRectF.left, tempRectF.bottom, tempRectF.left, tempRectF.bottom - outerStrokeRadius)
        tempPath.lineTo(tempRectF.left, tempRectF.bottom - outerStrokeLength)
        canvas.drawPath(tempPath, paint)
    }

    private fun startScaleAlphaAnim() {
        scaleAnimator?.cancel()
        scaleAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 600
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            addUpdateListener {
                val value = it.animatedValue as Float
                outInsets = value * iconOuterMargin / 3f
                outAlpha = (255 - value * 180).toInt()
                invalidate()
            }
            start()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        scaleAnimator?.cancel()
    }
}