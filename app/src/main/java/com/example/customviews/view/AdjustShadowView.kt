package com.example.customviews.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.core.graphics.applyCanvas
import com.example.customviews.utils.ext.matrixScaleX
import com.example.customviews.utils.ext.matrixScaleY
import com.github.chrisbanes.photoview.CustomGestureDetector
import com.github.chrisbanes.photoview.GestureListenerAdapter
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.min

class AdjustShadowView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var shadowMatrix = Matrix()
    private var cacheMatrix = Matrix()
    private val initialShadowRect = RectF()
    private var shadowBitmap: Bitmap? = null
    private val imageRect = RectF()
    private val tempRect = RectF()
    private val viewRect = RectF()
    private val bottomCenterPoints = FloatArray(2)

    private var totalDx = 0f
    private var totalDy = 0f
    private var initialScaleY = 1f
    private var isMoveMode = false

    private var shadowCanvas: Canvas? = null
    private var destShadowBitmap: Bitmap? = null

    private val paint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isDither = true
            isFilterBitmap = true
        }
    }
    private val bitmapPaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isDither = true
            isFilterBitmap = true
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        }
    }

    private val drawPaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isDither = true
            isFilterBitmap = true
            style = Paint.Style.STROKE
            color = Color.RED
            strokeWidth = 2f
        }
    }

    private val alphaGridDrawHelper by lazy {
        AlphaGridDrawHelper()
    }

    private val onGestureListener = object : GestureListenerAdapter() {
        override fun onDrag(x: Float, y: Float, dx: Float, dy: Float, event: MotionEvent) {
            onViewDrag(x, y, dx, dy, event)
        }

        override fun onFling(startX: Float, startY: Float, velocityX: Float, velocityY: Float) {

        }

        override fun onScale(scaleFactor: Float, focusX: Float, focusY: Float) {
            onScale(scaleFactor, focusX, focusY, 0f, 0f)
        }

        override fun onScale(scaleFactor: Float, focusX: Float, focusY: Float, dx: Float, dy: Float) {

        }
    }

    private val scaleDragDetector by lazy {
        CustomGestureDetector(context, onGestureListener)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        shadowBitmap?.let { initBitmapParams(it) }
        viewRect.set(0f, 0f, w.toFloat(), h.toFloat())
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        alphaGridDrawHelper.drawAlphaGrid(canvas, viewRect)
        Log.e("songmao", "onDraw: shadowBitmap = $shadowBitmap")
        destShadowBitmap?.let {
            canvas.drawBitmap(it, shadowMatrix, paint)
        }
        drawPaint.color = Color.RED
        drawPaint.style = Paint.Style.STROKE
        canvas.drawRect(tempRect, drawPaint)

        drawPaint.color = Color.BLUE
        drawPaint.style = Paint.Style.FILL
        canvas.drawCircle(bottomCenterPoints[0], bottomCenterPoints[1], 10f, drawPaint)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDragDetector.onTouchEvent(event)
        return true
    }

    private fun onViewDrag(x: Float, y: Float, dx: Float, dy: Float, event: MotionEvent) {
        if (isMoveMode) { // 移动图片
            shadowMatrix.postTranslate(dx, dy)
            shadowMatrix.mapRect(tempRect, imageRect)
            cacheMatrix.set(shadowMatrix)
            updateBottomCenterPoints()
            invalidate()
        } else { // 调整阴影3D效果
            totalDx -= dx * 3f // 调整系数让移动更灵敏
            totalDy -= dy * 3f // 调整系数让移动更灵敏

            val skewFactorX = totalDx / width
            val scaleFactorY = 1 + totalDy / height

            shadowMatrix.set(cacheMatrix)

            // 应用斜切变换，并使用底部中心点；如果图片进行了旋转，则需要先旋转图片，再斜切变换缩放，最后再旋转回来
            val angle = 0f
            shadowMatrix.postRotate(angle, bottomCenterPoints[0], bottomCenterPoints[1])
            shadowMatrix.postSkew(skewFactorX, 0f, bottomCenterPoints[0], bottomCenterPoints[1])
            shadowMatrix.postScale(1f, scaleFactorY, bottomCenterPoints[0], bottomCenterPoints[1])
            shadowMatrix.postRotate(-angle, bottomCenterPoints[0], bottomCenterPoints[1])

            shadowMatrix.mapRect(tempRect, imageRect)
            updatePaintGradient()
            invalidate()
        }
    }

    private fun updatePaintGradient() {
        val matrixScaleY = abs(shadowMatrix.matrixScaleY())
        // 根据y轴的缩放大小调整渐变透明度。缩放值越接近原始图片的缩放值，则渐变透明度越高
        val progress = when {
            matrixScaleY < initialScaleY -> {
                val ratio = matrixScaleY / initialScaleY
                exp((-15f * (1f - ratio)).toDouble()).toFloat()
            }
            matrixScaleY > initialScaleY -> {
                val ratio = initialScaleY / matrixScaleY
                exp((-15f * (1f - ratio)).toDouble()).toFloat()
            }
            else -> 1f
        }
        Log.d("startColor", "progress: $progress，tempRect: $tempRect")
        // 根据渐变比例计算渐变起始颜色
        val startColor = Color.argb((progress * 255).toInt(), 0, 0, 0)
        val gradient = LinearGradient(0f, 0f, 0f, destShadowBitmap!!.height.toFloat(), startColor, Color.BLACK, Shader.TileMode.CLAMP)

        // 清空画布图片
        destShadowBitmap?.eraseColor(Color.TRANSPARENT)
        destShadowBitmap?.applyCanvas {
            // 绘制阴影图片并跟渐变颜色混合，以达到背景图片透明度渐变的效果
            drawBitmap(shadowBitmap!!, 0f, 0f, null)
            bitmapPaint.shader = gradient
            drawRect(imageRect, bitmapPaint)
        }
    }

    fun setShadowBitmap(bitmap: Bitmap) {
        shadowBitmap = bitmap
        initBitmapParams(bitmap)
        invalidate()
    }

    private fun initBitmapParams(bitmap: Bitmap) {
        if (width == 0 || height == 0) return
        destShadowBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        shadowCanvas = Canvas(destShadowBitmap!!).apply {
            drawBitmap(bitmap, 0f, 0f, null)
        }

        imageRect.set(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat())
        val maxSize = min(width, height) / 2f
        val imageWidth: Float
        val imageHeight: Float
        if (bitmap.width > bitmap.height) {
            imageWidth = maxSize
            imageHeight = maxSize * bitmap.height / bitmap.width
        } else {
            imageHeight = maxSize
            imageWidth = maxSize * bitmap.width / bitmap.height
        }
        // Center the image
        val left = (width - imageWidth) / 2
        val top = (height - imageHeight) / 2
        initialShadowRect.set(left, top, left + imageWidth, top + imageHeight)
        val scale = min(imageWidth / bitmap.width, imageHeight / bitmap.height)
        shadowMatrix.reset()
        shadowMatrix.postScale(scale, scale)
        shadowMatrix.postTranslate(left, top)
//        shadowMatrix.postRotate(-30f, left + imageWidth / 2, top + imageHeight / 2)
        initialScaleY = scale
        cacheMatrix.set(shadowMatrix)

        // 更新底部中心点
        updateBottomCenterPoints()

        Log.e("songmao", "initBitmapParams initialScaleY: $initialScaleY, shadowMatrix scaleX: ${shadowMatrix.matrixScaleX()}, shadowMatrix scaleY: ${shadowMatrix.matrixScaleY()}")
    }

    private fun updateBottomCenterPoints() {
        bottomCenterPoints[0] = imageRect.centerX()
        bottomCenterPoints[1] = imageRect.bottom
        shadowMatrix.mapPoints(bottomCenterPoints)
    }

    fun updateShadowBitmap(bitmap: Bitmap?) {
        Log.d("songmao", "updateShadowBitmap: bitmap = $bitmap")
        if (bitmap != null) {
            shadowBitmap = bitmap
            onViewDrag(0f, 0f, 0f, 0f, MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 0f, 0f, 0))
            invalidate()
        }
    }

    fun setShadowAlpha(progress: Float) {
        paint.alpha = (progress * 255).toInt()
        invalidate()
    }

    fun setMoveMode(checked: Boolean) {
        isMoveMode = checked
    }
}

