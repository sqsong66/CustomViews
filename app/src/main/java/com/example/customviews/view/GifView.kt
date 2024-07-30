package com.example.customviews.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Size
import android.view.View
import com.example.customviews.R
import com.example.customviews.utils.dp2Px
import com.example.customviews.utils.getThemeColor
import com.example.customviews.view.anno.GifQuality
import com.sqsong.photoeditor.view.anno.CropMode
import kotlin.math.min

class GifView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var isRatioMode = false
    private var borderWidth = 0f
    private var borderColor = 0
    private var gridCount = 0
    private var gridStrokeWidth = 0f

    private val showRect = RectF()
    private val originRect = RectF()
    private val tempRect = RectF()
    private var showMilSeconds = 1000L
    private var currentBitmapIndex = 0

    @CropMode
    private var cropMode: Int = CropMode.ORIGIN
    private val bitmapList = mutableListOf<Bitmap>()
    private val bitmapMatrix = mutableListOf<Matrix>()
    private var resizeAnimator: ValueAnimator? = null

    private val paint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = borderWidth
            color = borderColor
        }
    }

    private val bitmapPaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isDither = true
            isFilterBitmap = true
        }
    }

    private val playRunnable = object : Runnable {
        override fun run() {
            val index = currentBitmapIndex + 1
            currentBitmapIndex = index % bitmapList.size
            invalidate()
            postDelayed(this, showMilSeconds)
        }
    }

    init {
        handleAttributes(context, attrs)
    }

    private fun handleAttributes(context: Context, attrs: AttributeSet?) {
        context.obtainStyledAttributes(attrs, R.styleable.GifView).apply {
            isRatioMode = getBoolean(R.styleable.GifView_gv_isRatioMode, true)
            borderWidth = getDimension(R.styleable.GifView_gv_borderWidth, dp2Px(1))
            borderColor = getColor(R.styleable.GifView_gv_borderColor, getThemeColor(context, com.google.android.material.R.attr.colorPrimary))
            gridStrokeWidth = getDimension(R.styleable.GifView_gv_gridWidth, dp2Px(0.5f))
            gridCount = getInt(R.styleable.GifView_gv_gridCount, 3)
            recycle()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        bitmapList.firstOrNull()?.let {
            setupBitmapMatrix(Size(it.width, it.height))
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.clipRect(showRect)
        drawBitmaps(canvas)
        if (isRatioMode) drawGrid(canvas)
    }

    private fun drawGrid(canvas: Canvas) {
        paint.style = Paint.Style.STROKE
        tempRect.set(showRect)
        tempRect.inset(borderWidth / 2, borderWidth / 2)
        paint.strokeWidth = borderWidth
        canvas.drawRect(tempRect, paint)

        val gridWidth = tempRect.width() / gridCount
        val gridHeight = tempRect.height() / gridCount
        paint.strokeWidth = gridStrokeWidth
        for (i in 1 until gridCount) {
            val startX = tempRect.left + i * gridWidth
            val startY = tempRect.top + i * gridHeight
            canvas.drawLine(startX, tempRect.top, startX, tempRect.bottom, paint)
            canvas.drawLine(tempRect.left, startY, tempRect.right, startY, paint)
        }
    }

    private fun drawBitmaps(canvas: Canvas) {
        if (bitmapList.isEmpty() || bitmapList.size != bitmapMatrix.size || currentBitmapIndex >= bitmapList.size) return
        val bitmap = bitmapList[currentBitmapIndex]
        val matrix = bitmapMatrix[currentBitmapIndex]
        canvas.drawBitmap(bitmap, matrix, bitmapPaint)
    }

    fun setGifBitmaps(bitmaps: List<Bitmap>?) {
        if (bitmaps.isNullOrEmpty()) return
        bitmapList.clear()
        bitmapList.addAll(bitmaps)
        val firstBitmap = bitmapList.first()
        setupBitmapMatrix(Size(firstBitmap.width, firstBitmap.height))
        startPlayGif()
        invalidate()
    }

    private fun updateBitmapMatrix() {
        if (bitmapList.isEmpty() || width == 0 || height == 0) return
        val viewRatio = width.toFloat() / height
        val gifSize = getGifSize(GifQuality.STANDARD)
        val sRatio = gifSize.width.toFloat() / gifSize.height
        if (viewRatio < sRatio) {
            val bHeight = width / sRatio
            showRect.set(0f, (height - bHeight) / 2, width.toFloat(), (height + bHeight) / 2)
        } else {
            val bWidth = height * sRatio
            showRect.set((width - bWidth) / 2, 0f, (width + bWidth) / 2, height.toFloat())
        }

        bitmapMatrix.clear()
        bitmapList.forEach { bitmap ->
            val scale: Float
            val dx: Float
            val dy: Float
            if (originRect.width() < originRect.height()) {
                scale = showRect.width() / bitmap.width
                dx = showRect.centerX() - bitmap.width * scale / 2
                dy = showRect.centerY() - bitmap.height * scale / 2
            } else {
                scale = showRect.height() / bitmap.height
                dx = showRect.centerX() - bitmap.width * scale / 2
                dy = showRect.centerY() - bitmap.height * scale / 2
            }
            val matrix = Matrix()
            matrix.postScale(scale, scale)
            matrix.postTranslate(dx, dy)
            bitmapMatrix.add(matrix)
        }
    }

    private fun setupBitmapMatrix(size: Size) {
        if (bitmapList.isEmpty() || width == 0 || height == 0) return
        val viewRatio = width.toFloat() / height
        val bRatio = size.width.toFloat() / size.height
        if (viewRatio < bRatio) {
            val bHeight = width / bRatio
            showRect.set(0f, (height - bHeight) / 2, width.toFloat(), (height + bHeight) / 2)
        } else {
            val bWidth = height * bRatio
            showRect.set((width - bWidth) / 2, 0f, (width + bWidth) / 2, height.toFloat())
        }
        originRect.set(showRect)
        bitmapMatrix.clear()
        bitmapList.forEach { bitmap ->
            // 将Bitmap进行缩放放置到showRect中间
            val matrix = Matrix()
            val scale = min(showRect.width() / bitmap.width, showRect.height() / bitmap.height)
            matrix.postScale(scale, scale)
            matrix.postTranslate(showRect.centerX() - bitmap.width * scale / 2, showRect.centerY() - bitmap.height * scale / 2)
            bitmapMatrix.add(matrix)
        }
    }

    private fun startPlayGif() {
        removeCallbacks(playRunnable)
        postDelayed(playRunnable, showMilSeconds)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        removeCallbacks(playRunnable)
    }

    fun setCropMode(@CropMode mode: Int) {
        if (cropMode == mode) return
        cropMode = mode
        bitmapList.firstOrNull()?.let {
            setupBitmapMatrix(Size(it.width, it.height))
        }
        startChangeCropModeAnim()
    }

    private fun startChangeCropModeAnim() {
        val startRect = RectF(showRect)
        val destRect = getDestRect(cropMode)
        resizeAnimator?.cancel()
        resizeAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 300
            addUpdateListener {
                val value = it.animatedValue as Float
                showRect.set(
                    startRect.left + (destRect.left - startRect.left) * value,
                    startRect.top + (destRect.top - startRect.top) * value,
                    startRect.right + (destRect.right - startRect.right) * value,
                    startRect.bottom + (destRect.bottom - startRect.bottom) * value
                )
                invalidate()
            }
            start()
        }
    }

    private fun getDestRect(@CropMode cropMode: Int = this.cropMode): RectF {
        return when (cropMode) {
            CropMode.RATIO_1_1 -> {
                if (originRect.width() > originRect.height()) {
                    val left = originRect.left + (originRect.width() - originRect.height()) / 2
                    val right = originRect.right - (originRect.width() - originRect.height()) / 2
                    RectF(left, originRect.top, right, originRect.bottom)
                } else {
                    val top = originRect.top + (originRect.height() - originRect.width()) / 2
                    val bottom = originRect.bottom - (originRect.height() - originRect.width()) / 2
                    RectF(originRect.left, top, originRect.right, bottom)
                }
            }

            CropMode.RATIO_4_3 -> {
                if (originRect.width() > originRect.height()) {
                    val left = originRect.left + (originRect.width() - originRect.height() * 4 / 3) / 2
                    val right = originRect.right - (originRect.width() - originRect.height() * 4 / 3) / 2
                    RectF(left, originRect.top, right, originRect.bottom)
                } else {
                    val top = originRect.top + (originRect.height() - originRect.width() * 3 / 4) / 2
                    val bottom = originRect.bottom - (originRect.height() - originRect.width() * 3 / 4) / 2
                    RectF(originRect.left, top, originRect.right, bottom)
                }
            }

            CropMode.RATIO_3_4 -> {
                if (originRect.width() > originRect.height()) {
                    val left = originRect.left + (originRect.width() - originRect.height() * 3 / 4) / 2
                    val right = originRect.right - (originRect.width() - originRect.height() * 3 / 4) / 2
                    RectF(left, originRect.top, right, originRect.bottom)
                } else {
                    val top = originRect.top + (originRect.height() - originRect.width() * 4 / 3) / 2
                    val bottom = originRect.bottom - (originRect.height() - originRect.width() * 4 / 3) / 2
                    RectF(originRect.left, top, originRect.right, bottom)
                }
            }

            CropMode.RATIO_16_9 -> {
                if (originRect.width() > originRect.height()) {
                    val left = originRect.left + (originRect.width() - originRect.height() * 16 / 9) / 2
                    val right = originRect.right - (originRect.width() - originRect.height() * 16 / 9) / 2
                    RectF(left, originRect.top, right, originRect.bottom)
                } else {
                    val top = originRect.top + (originRect.height() - originRect.width() * 9 / 16) / 2
                    val bottom = originRect.bottom - (originRect.height() - originRect.width() * 9 / 16) / 2
                    RectF(originRect.left, top, originRect.right, bottom)
                }
            }

            CropMode.RATIO_9_16 -> {
                if (originRect.width() > originRect.height()) {
                    val left = originRect.left + (originRect.width() - originRect.height() * 9 / 16) / 2
                    val right = originRect.right - (originRect.width() - originRect.height() * 9 / 16) / 2
                    RectF(left, originRect.top, right, originRect.bottom)
                } else {
                    val top = originRect.top + (originRect.height() - originRect.width() * 16 / 9) / 2
                    val bottom = originRect.bottom - (originRect.height() - originRect.width() * 16 / 9) / 2
                    RectF(originRect.left, top, originRect.right, bottom)
                }
            }

            else -> {
                originRect
            }
        }
    }

    fun getGifSize(@GifQuality quality: Int, @CropMode cropMode: Int = this.cropMode): Size {
        val destRect = getDestRect(cropMode)
        val maxSize = when (quality) {
            GifQuality.HIGH -> 960
            GifQuality.ULTRA_HIGH -> 1280
            else -> 640
        }
        val width: Int
        val height: Int
        if (destRect.width() > destRect.height()) {
            width = maxSize
            height = (maxSize * destRect.height() / destRect.width()).toInt()
        } else {
            width = (maxSize * destRect.width() / destRect.height()).toInt()
            height = maxSize
        }
        return Size(width, height)
    }

    fun applyCropMode(@CropMode cropMode: Int) {
        this.cropMode = cropMode
        updateBitmapMatrix()
        invalidate()
    }

    fun getOrigin(): RectF = originRect

    fun getGifBitmaps(): List<Bitmap> = bitmapList

    fun getGifDelay(): Long = showMilSeconds

    fun getCropMode(): Int = cropMode
}