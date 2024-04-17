package com.example.customviews.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Shader
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import com.example.customviews.utils.dp2Px
import com.example.customviews.utils.ext.matrixScale
import com.example.customviews.utils.ext.matrixTranslate
import kotlin.math.max

data class MosaicPath(val path: Path, val paint: Paint)

class MosaicView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ScalableImageView(context, attrs, defStyleAttr) {

    private var lastX = 0f
    private var lastY = 0f
    private var isScaleMode = false
    private val mosaicPath = Path()
    private val tempMatrix = Matrix()
    private val cacheMatrix = Matrix()
    private val tempPoint = FloatArray(2)
    private var mosaicBitmap: Bitmap? = null
    private val mosaicBitmapMatrix = Matrix()
    private var originMosaicScaleFactor = 1.0f

    private val mosaicPathList = mutableListOf<MosaicPath>()

    private val mosaicPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isDither = true
        isFilterBitmap = true
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        strokeWidth = dp2Px(10)
        color = Color.RED
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
    }

    private fun mapPoints(x: Float, y: Float): FloatArray {
        tempPoint[0] = x
        tempPoint[1] = y
        tempMatrix.reset()
        mosaicBitmapMatrix.invert(tempMatrix)
        tempMatrix.mapPoints(tempPoint)
        return tempPoint
    }

    override fun onViewFling(startX: Float, startY: Float, velocityX: Float, velocityY: Float) {

    }

    override fun onPointerDown(event: MotionEvent) {
        if (event.pointerCount > 1) {
            isScaleMode = true
        }
    }

    override fun onTouchDown(event: MotionEvent) {
        super.onTouchDown(event)
        isScaleMode = false
        mapPoints(event.x, event.y).let {
            mosaicPath.reset()
            mosaicPath.moveTo(it[0], it[1])
        }
        lastX = event.x
        lastY = event.y
    }

    override fun onTouchUp(event: MotionEvent) {
        super.onTouchUp(event)
        mosaicPathList.add(MosaicPath(Path(mosaicPath), Paint(mosaicPaint)))
        mosaicPath.reset()
        invalidate()
    }

    override fun onViewDrag(x: Float, y: Float, dx: Float, dy: Float, pointerCount: Int) {
        if (pointerCount > 1 || isScaleMode) {
            super.onViewDrag(x, y, dx, dy, pointerCount)
        } else {
            val start = mapPoints(lastX, lastY)
            val end = mapPoints(x, y)
            mosaicPath.quadTo(start[0], start[1], (start[0] + end[0]) / 2, (start[1] + end[1]) / 2)
            lastX = x
            lastY = y
            invalidate()
        }

    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.save()
        getDisplayRect()?.let { displayRect ->
            canvas.clipRect(displayRect)
        }
        canvas.setMatrix(mosaicBitmapMatrix)

        for (mosaicPath in mosaicPathList) {
            canvas.drawPath(mosaicPath.path, mosaicPath.paint)
        }

        canvas.drawPath(mosaicPath, mosaicPaint)
        canvas.restore()
    }

    override fun setImageBitmap(bitmap: Bitmap) {
        super.setImageBitmap(bitmap)
        mosaicBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
    }

    override fun onBaseImageTransformed(matrix: Matrix) {
        mosaicBitmapMatrix.set(cacheMatrix)
        mosaicBitmapMatrix.postConcat(matrix)
        mosaicPaint.strokeWidth = dp2Px<Float>(30) / mosaicBitmapMatrix.matrixScale()
    }

    fun setImageBitmaps(bitmap: Bitmap, blurBitmap: Bitmap) {
        super.setImageBitmap(bitmap)
        originMosaicScaleFactor = bitmap.width.toFloat() / blurBitmap.width
        mosaicBitmap = Bitmap.createBitmap(blurBitmap.width, blurBitmap.height, Bitmap.Config.ARGB_8888).apply {
            mosaicPaint.shader = BitmapShader(blurBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        }
        val blurRadius = max(blurBitmap.width, blurBitmap.height) / 1000f * 2f
        mosaicPaint.maskFilter = BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL)
        Log.d("songmao", "setImageBitmaps, blurRadius: $blurRadius.")
        resetMosaicBitmapMatrix()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        resetMosaicBitmapMatrix()
    }

    private fun resetMosaicBitmapMatrix() {
        if (showRect.isEmpty) return
        mosaicBitmapMatrix.reset()
        mosaicBitmap?.let { mosaicBitmap ->
            mosaicBitmapMatrix.postScale(showRect.width() / mosaicBitmap.width.toFloat(), showRect.height() / mosaicBitmap.height.toFloat())
            mosaicBitmapMatrix.postTranslate(showRect.left, showRect.top)
            cacheMatrix.set(mosaicBitmapMatrix)
            mosaicPaint.strokeWidth = dp2Px<Float>(30) / mosaicBitmapMatrix.matrixScale()
        }
    }

    fun getMosaicBitmap(): Bitmap? {
        val baseBitmap = viewBitmap ?: return null
        val newMosaicBitmap = mosaicBitmap ?: return null
        val bitmap = Bitmap.createBitmap(baseBitmap.width, baseBitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        canvas.drawBitmap(baseBitmap, 0f, 0f, Paint(Paint.ANTI_ALIAS_FLAG))

        val matrix = Matrix()
        val sx = newMosaicBitmap.width.toFloat() / showRect.width()
        val sy = newMosaicBitmap.height.toFloat() / showRect.height()
        matrix.postScale(sx * originMosaicScaleFactor, sy * originMosaicScaleFactor)
        matrix.postTranslate(-showRect.left * sx, -showRect.top * sy)
        matrix.postConcat(mosaicBitmapMatrix)

        // 由于图层是基于底图进行变换的，所以需要将矩阵基于底图矩阵进行逆变换还原
        val scale = suppMatrix.matrixScale()
        matrix.postScale(1 / scale, 1 / scale)
        val translateArray = suppMatrix.matrixTranslate()
        matrix.postTranslate(-translateArray[0] / scale, -translateArray[1] / scale)

        Log.d("songmao", "getMosaicBitmap, mosaicPathList size: ${mosaicPathList.size}, baseBitmap: ${baseBitmap.width}x${baseBitmap.height}, newMosaicBitmap: ${newMosaicBitmap.width}x${newMosaicBitmap.height}.")
        canvas.setMatrix(matrix)
        for (mosaicPath in mosaicPathList) {
            canvas.drawPath(mosaicPath.path, mosaicPath.paint)
        }
        canvas.drawPath(mosaicPath, mosaicPaint)
        return bitmap
    }

    fun clearMosaic() {
        mosaicPathList.clear()
        mosaicPath.reset()
        invalidate()
    }

}