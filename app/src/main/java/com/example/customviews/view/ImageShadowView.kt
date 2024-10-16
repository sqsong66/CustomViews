package com.example.customviews.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import com.example.customviews.ui.OutlineInfo
import com.example.customviews.utils.ext.matrixRotateDegree
import com.example.customviews.utils.ext.matrixScale
import com.example.customviews.utils.resizeBitmap
import com.github.chrisbanes.photoview.CustomGestureDetector
import com.github.chrisbanes.photoview.GestureListenerAdapter


class ImageShadowView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : View(context, attrs, defStyle) {

    private var shadowColor = Color.BLACK

    private val imageRect = RectF()
    private val shadowTopPoint = PointF()
    private val shadowBottomPoint = PointF()
    private val imageMatrix = Matrix()
    private val shadowMatrix = Matrix()
    private val shadowCacheMatrix = Matrix()
    private var imageBitmap: Bitmap? = null
    private var shadowBitmap: Bitmap? = null

    private val viewRect = RectF()
    private val alphaGridDrawHelper by lazy {
        AlphaGridDrawHelper()
    }

    private var is3DMode = false
    private var shadowScale = 1f
    private var shadowOffsetX = 0f
    private var shadowOffsetY = 0f
    private var shortnessAlpha = 255
    private var shadowPaintAlpha = 255
    private var blurMaskFilter: BlurMaskFilter? = null
    private val tempCoordArray = FloatArray(2)

    private val shadowPaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isFilterBitmap = true
            isDither = true
            color = shadowColor
            style = Paint.Style.STROKE
            setLayerType(LAYER_TYPE_SOFTWARE, this)
        }
    }

    private val imagePaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isFilterBitmap = true
            isDither = true
        }
    }

    private val borderPaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isDither = true
            strokeWidth = 5f
            style = Paint.Style.STROKE
            color = Color.RED
        }
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
        viewRect.set(0f, 0f, w.toFloat(), h.toFloat())
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        scaleDragDetector.onTouchEvent(event)
        return true
    }

    override fun onDraw(canvas: Canvas) {
        // Draw alpha grid
        alphaGridDrawHelper.drawAlphaGrid(canvas, viewRect)

        // Draw shadow bitmap
        shadowBitmap?.let {
            shadowPaint.maskFilter = blurMaskFilter
            shadowPaint.alpha = shadowPaintAlpha
            canvas.drawBitmap(it, shadowMatrix, shadowPaint)
            shadowPaint.maskFilter = null
            shadowPaint.alpha = 255
        }

        // Draw image bitmap
        imageBitmap?.let {
            canvas.drawBitmap(it, imageMatrix, imagePaint)
        }

        borderPaint.color = Color.RED
        borderPaint.style = Paint.Style.STROKE
        canvas.drawRect(imageRect, borderPaint)

        borderPaint.color = Color.CYAN
        borderPaint.style = Paint.Style.FILL
        canvas.drawCircle(shadowTopPoint.x, shadowTopPoint.y, 15f, borderPaint)
        canvas.drawCircle(shadowBottomPoint.x, shadowBottomPoint.y, 15f, borderPaint)
    }

    private fun onViewDrag(x: Float, y: Float, dx: Float, dy: Float, event: MotionEvent) {
        if (is3DMode) { // 3D Move
            shadowOffsetX -= dx * 1.2f
            shadowOffsetY -= dy * 1.2f

            val skewX = shadowOffsetX / imageRect.width()
            val scaleY = 1f + shadowOffsetY / imageRect.height()
            val angle = imageMatrix.matrixRotateDegree()
            shadowMatrix.set(shadowCacheMatrix)
            shadowMatrix.postRotate(angle, shadowBottomPoint.x, shadowBottomPoint.y)
            shadowMatrix.postSkew(skewX, 0f, shadowBottomPoint.x, shadowBottomPoint.y)
            shadowMatrix.postScale(1f, scaleY, shadowBottomPoint.x, shadowBottomPoint.y)
            shadowMatrix.postRotate(-angle, shadowBottomPoint.x, shadowBottomPoint.y)
        } else { // Move
            shadowBitmap?.let { bitmap ->
                shadowMatrix.postTranslate(dx, dy)
                shadowCacheMatrix.set(shadowMatrix)

                tempCoordArray[0] = bitmap.width / 2f
                tempCoordArray[1] = bitmap.height.toFloat()
                shadowMatrix.mapPoints(tempCoordArray)
                shadowBottomPoint.set(tempCoordArray[0], tempCoordArray[1])
                tempCoordArray[0] = bitmap.width / 2f
                tempCoordArray[1] = 0f
                shadowMatrix.mapPoints(tempCoordArray)
                shadowTopPoint.set(tempCoordArray[0], tempCoordArray[1])

                shadowScale = shadowMatrix.matrixScale()
                shadowOffsetX = 0f
                shadowOffsetY = 0f
            }
        }
        invalidate()
    }

    fun setImageBitmap(outlineInfo: OutlineInfo) {
        val bitmap = outlineInfo.bitmap
        imageBitmap = bitmap
        shadowBitmap = resizeBitmap(bitmap.extractAlpha(), 512)
        layoutImage(bitmap)
        Log.d("ImageOutlineView", "bitmap size: ${bitmap.width}x${bitmap.height}, alpha size: ${shadowBitmap?.width}x${shadowBitmap?.height}")
    }

    private fun layoutImage(bitmap: Bitmap) {
        if (width == 0 || height == 0) return
        // 居中摆放图片
        val scale: Float
        val dx: Float
        val dy: Float
        var destWidth = width * 0.5f
        var destHeight = destWidth * bitmap.height / bitmap.width
        if (destHeight > height * 0.5f) {
            destHeight = height * 0.5f
            destWidth = destHeight * bitmap.width / bitmap.height
        }
        val angle = 0f
        scale = destWidth / bitmap.width
        dx = (width - destWidth) / 2f
        dy = (height - destHeight) / 2f
        imageMatrix.reset()
        imageMatrix.postScale(scale, scale)
        imageMatrix.postRotate(angle)
        imageMatrix.postTranslate(dx, dy)

        // 获取到图片的显示区域
        imageMatrix.mapRect(imageRect, RectF(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat()))

        shadowBitmap?.let { newBitmap ->
            val alphaScale = bitmap.width.toFloat() / newBitmap.width
            shadowMatrix.set(imageMatrix)
            shadowMatrix.postScale(alphaScale, alphaScale, dx, dy)
            shadowCacheMatrix.set(shadowMatrix)

            // val array = floatArrayOf(newBitmap.width / 2f, newBitmap.height.toFloat())
            tempCoordArray[0] = newBitmap.width / 2f
            tempCoordArray[1] = newBitmap.height.toFloat()
            shadowMatrix.mapPoints(tempCoordArray)
            shadowBottomPoint.set(tempCoordArray[0], tempCoordArray[1])
            tempCoordArray[0] = newBitmap.width / 2f
            tempCoordArray[1] = 0f
            shadowMatrix.mapPoints(tempCoordArray)
            shadowTopPoint.set(tempCoordArray[0], tempCoordArray[1])

            shadowScale = shadowMatrix.matrixScale()
            val color = Color.argb(shortnessAlpha, Color.red(shadowColor), Color.green(shadowColor), Color.blue(shadowColor))
            val gradientShader = LinearGradient(0f, 0f, 0f, imageRect.height(), color, shadowColor, Shader.TileMode.CLAMP)
            shadowPaint.shader = gradientShader
        }

        invalidate()
    }

    fun  setBlurRadius(blurRadius: Float) {
        blurMaskFilter = if (blurRadius > 0f) {
            BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL)
        } else {
            null
        }
        invalidate()
    }

    fun setShadowAlpha(alpha: Int) {
        shadowPaintAlpha = alpha
        invalidate()
    }

    fun setShortnessAlpha(value: Float) {
        val alpha = (255 * value).toInt()
        shortnessAlpha = alpha
        val color = Color.argb(alpha, Color.red(shadowColor), Color.green(shadowColor), Color.blue(shadowColor))
        val gradientShader = LinearGradient(
            0f, 0f, 0f, imageRect.height(),
            intArrayOf(Color.TRANSPARENT, color, shadowColor, shadowColor),
            floatArrayOf(-0.2f, 1.1f - value, 0.9f, 1f),
            Shader.TileMode.CLAMP
        )
        shadowPaint.shader = gradientShader
        invalidate()
    }

    fun setMoveMode(checked: Boolean) {
        is3DMode = checked
        invalidate()
    }

}