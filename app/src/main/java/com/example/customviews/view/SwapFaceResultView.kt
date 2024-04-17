package com.example.customviews.view

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.animation.addListener
import androidx.core.graphics.drawable.toBitmap
import com.example.customviews.R
import com.example.customviews.utils.dp2Px
import kotlin.math.abs
import kotlin.math.max

class SwapFaceResultView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var circleX = 0f
    private var circleY = 0f
    private val clipRect: RectF = RectF()
    private val circleRect: RectF = RectF()
    private val tempRect: RectF = RectF()
    private var imageBitmap: Bitmap? = null
    private val imageMatrix: Matrix = Matrix()
    private val cacheMatrix: Matrix = Matrix()
    private var animatorSet: AnimatorSet? = null
    private var starAnimator: ValueAnimator? = null
    private val starRectList = mutableListOf<RectF>()

    private val bigStarBitmap by lazy {
        AppCompatResources.getDrawable(context, R.drawable.ic_star_big)?.toBitmap()
    }

    private val smallStarBitmap by lazy {
        AppCompatResources.getDrawable(context, R.drawable.ic_star_small)?.toBitmap()
    }

    private val paint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isDither = true
            isFilterBitmap = true
        }
    }

    private val bigStarPaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isDither = true
            isFilterBitmap = true
        }
    }

    private val smallStarPaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isDither = true
            isFilterBitmap = true
        }
    }

    private val circlePaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isDither = true
            color = Color.WHITE
            alpha = 0
        }
    }

    private val blurBitmap by lazy {
        BitmapFactory.decodeResource(resources, R.drawable.img_white_blur)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        resetImageMatrix()
    }

    private fun resetImageMatrix() {
        val bitmap = imageBitmap ?: return
        if (width == 0 || height == 0) return

        val bRatio = bitmap.width.toFloat() / bitmap.height
        val vRatio = width.toFloat() / height
        if (bRatio > vRatio) {
            val scale = width.toFloat() / bitmap.width
            val dy = (height - bitmap.height * scale) / 2
            imageMatrix.setScale(scale, scale)
            imageMatrix.postTranslate(0f, dy)
            clipRect.set(0f, dy, width.toFloat(), height - dy)
        } else {
            val scale = height.toFloat() / bitmap.height
            val dx = (width - bitmap.width * scale) / 2
            imageMatrix.setScale(scale, scale)
            imageMatrix.postTranslate(dx, 0f)
            clipRect.set(dx, 0f, width - dx, height.toFloat())
        }

        val circleSize = max(clipRect.width(), clipRect.height())
        circleRect.set(clipRect.centerX() - circleSize, clipRect.centerY() - circleSize, clipRect.centerX() + circleSize, clipRect.centerY() + circleSize)

        bigStarPaint.alpha = 0
        smallStarPaint.alpha = 0

        val bigStar = bigStarBitmap ?: return
        val smallStar = smallStarBitmap ?: return

        starRectList.clear()
        val left = clipRect.left + 59f / 343f * clipRect.width()
        val top = clipRect.top + 193f / 447f * clipRect.height()
        val rect = RectF(left, top, left + bigStar.width, top + bigStar.height)
        starRectList.add(rect)

        val left1 = rect.right + dp2Px<Float>(8)
        val top1 = rect.bottom + dp2Px<Float>(6)
        val rect1 = RectF(left1, top1, left1 + smallStar.width, top1 + smallStar.height)
        starRectList.add(rect1)

        val left2 = clipRect.left + 273f / 343f * clipRect.width()
        val top2 = clipRect.top + 40f / 447f * clipRect.height()
        val rect2 = RectF(left2, top2, left2 + bigStar.width, top2 + bigStar.height)
        starRectList.add(rect2)

        val left3 = left2 - dp2Px<Float>(8) - smallStar.width
        val top3 = top2 + dp2Px<Float>(6) + bigStar.height
        val rect3 = RectF(left3, top3, left3 + smallStar.width, top3 + smallStar.height)
        starRectList.add(rect3)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.clipRect(clipRect)
        imageBitmap?.let { canvas.drawBitmap(it, imageMatrix, paint) }
        drawStars(canvas)
        drawBlingCircle(canvas)
    }

    private fun drawBlingCircle(canvas: Canvas) {
        if (circlePaint.alpha == 0) return
        tempRect.set(circleRect.left, circleY, circleRect.right, circleY + circleRect.height())
        canvas.drawBitmap(blurBitmap, null, tempRect, circlePaint)
    }

    private fun drawStars(canvas: Canvas) {
        val bigStar = bigStarBitmap ?: return
        val smallStar = smallStarBitmap ?: return
        starRectList.forEachIndexed { index, rect ->
            if (index % 2 == 0) {
                canvas.drawBitmap(bigStar, rect.left, rect.top, bigStarPaint)
            } else {
                canvas.drawBitmap(smallStar, rect.left, rect.top, smallStarPaint)
            }
        }
    }

    fun setImageBitmap(bitmap: Bitmap?) {
        imageBitmap = bitmap
        resetImageMatrix()
        if (bitmap != null) {
            startImageAnimation()
        } else {
            starRectList.clear()
            starAnimator?.cancel()
            smallStarPaint.alpha = 0
            invalidate()
        }
    }

    private fun startImageAnimation() {
        cacheMatrix.set(imageMatrix)
        animatorSet = AnimatorSet().apply {
            duration = 1200
            playTogether(scaleAnimator(), alphaAnimator())
            addListener(onEnd = {
                postDelayed({ startStarBlingAnimation() }, 200)
            })
            start()
        }
    }

    private fun scaleAnimator(): ValueAnimator {
        return ObjectAnimator.ofFloat(1.4f, 1.1f).apply {
            interpolator = OvershootInterpolator()
            addUpdateListener {
                val scale = it.animatedValue as Float
                imageMatrix.set(cacheMatrix)
                imageMatrix.postScale(scale, scale, clipRect.centerX(), clipRect.centerY())
                invalidate()
            }
        }
    }

    private fun alphaAnimator(): ValueAnimator {
        circleX = clipRect.centerX()
        val height = clipRect.height() + circleRect.height()
        return ObjectAnimator.ofFloat(0f, 1f).apply {
            duration = 1200
            interpolator = LinearInterpolator()
            addUpdateListener {
                val value = it.animatedValue as Float
                // alpha 0 -> 255 -> 0
                val alpha = 255 - (abs(value - 0.5f) * 2 * 255).toInt()
                Log.d("songmao", "alpha: $alpha")
                circlePaint.alpha = alpha
                circleY = clipRect.top - circleRect.height() / 2f + height * value
                invalidate()
            }
        }
    }

    private fun startStarBlingAnimation() {
        starAnimator = ObjectAnimator.ofInt(0, 255).apply {
            duration = 1000
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.REVERSE
            addUpdateListener {
                val alpha = it.animatedValue as Int
                bigStarPaint.alpha = 255 - alpha
                smallStarPaint.alpha = alpha
                invalidate()
            }
            start()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        starAnimator?.cancel()
        animatorSet?.cancel()
    }
}