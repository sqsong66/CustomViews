package com.example.customviews.view

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.OverScroller
import com.example.customviews.R
import com.example.customviews.utils.decodeBitmapFromResource
import com.example.customviews.utils.ext.matrixScale
import com.example.customviews.utils.getCenterFromRect
import com.example.customviews.utils.getCornersFromRect
import com.github.chrisbanes.photoview.CustomGestureDetector
import com.github.chrisbanes.photoview.OnGestureListener
import kotlin.math.max

open class ScalableImageView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val baseMatrix = Matrix()
    private val drawMatrix = Matrix()
    private val cacheMatrix = Matrix()
    protected var viewBitmap: Bitmap? = null

    private var maxScaleSize = 5f
    private var flingRunnable: FlingRunnable? = null
    private var scaleAnimator: ValueAnimator? = null

    private var imageInitialCenter: FloatArray? = null
    private var imageInitialCorners: FloatArray? = null

    protected val showRect = RectF()
    protected val imageRect = RectF()
    protected var isScaleGesture = true
    protected val suppMatrix = Matrix()
    private val imageTransformCorners = FloatArray(8)
    private val imageTransformCenter = FloatArray(2)


    private val paint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isDither = true
            isFilterBitmap = true
        }
    }

    private val onGestureListener = object : OnGestureListener {
        override fun onDrag(x: Float, y: Float, dx: Float, dy: Float, pointerCount: Int) {
            onViewDrag(x, y, dx, dy, pointerCount)
        }

        override fun onFling(startX: Float, startY: Float, velocityX: Float, velocityY: Float) {
            onViewFling(startX, startY, velocityX, velocityY)
        }

        override fun onScale(scaleFactor: Float, focusX: Float, focusY: Float) {
            onScale(scaleFactor, focusX, focusY, 0f, 0f)
        }

        override fun onScale(scaleFactor: Float, focusX: Float, focusY: Float, dx: Float, dy: Float) {
            isScaleGesture = true
            onViewScale(scaleFactor, focusX, focusY, dx, dy)
        }
    }

    private val scaleDragDetector by lazy {
        CustomGestureDetector(context, onGestureListener)
    }

    init {
        handleAttributes(context, attrs)
    }

    private fun handleAttributes(context: Context, attrs: AttributeSet?) {
        context.obtainStyledAttributes(attrs, R.styleable.ScalableImageView).apply {
            val imageRes = getResourceId(R.styleable.ScalableImageView_siv_imageSrc, 0)
            if (imageRes != 0) {
                viewBitmap = decodeBitmapFromResource(context, imageRes, 512)
            }
            recycle()
        }
    }

    protected open fun onViewDrag(x: Float, y: Float, dx: Float, dy: Float, pointerCount: Int) {
        suppMatrix.postTranslate(dx, dy)
        checkAndDisplayMatrix()
    }

    protected open fun onViewFling(startX: Float, startY: Float, velocityX: Float, velocityY: Float) {
        viewBitmap?.let {
            flingRunnable = FlingRunnable(context)
            flingRunnable?.fling(velocityX.toInt(), velocityY.toInt())
            post(flingRunnable)
        }
    }

    protected open fun onViewScale(scaleFactor: Float, focusX: Float, focusY: Float, dx: Float, dy: Float) {
        val scale = suppMatrix.matrixScale()
        if (scale < maxScaleSize || scaleFactor < 1f) {
            suppMatrix.postScale(scaleFactor, scaleFactor, focusX, focusY)
            suppMatrix.postTranslate(dx, dy)
            checkAndDisplayMatrix()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        viewBitmap?.let {
            onImageLaidOut()
            updateImageMatrix(it)
        }
    }

    override fun onDraw(canvas: Canvas) {
        viewBitmap?.let { bitmap ->
            canvas.drawBitmap(bitmap, drawMatrix, paint)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> onTouchDown(event)
            MotionEvent.ACTION_POINTER_DOWN -> onPointerDown(event)
            MotionEvent.ACTION_MOVE -> onTouchMove(event)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> onTouchUp(event)
        }
        scaleDragDetector.onTouchEvent(event)
        return true
    }

    protected open fun onPointerDown(event: MotionEvent) {}

    protected open fun onTouchMove(event: MotionEvent) {}

    protected open fun onTouchDown(event: MotionEvent) {
        isScaleGesture = false
        flingRunnable?.cancelFling()
        parent.requestDisallowInterceptTouchEvent(true)
    }

    protected open fun onTouchUp(event: MotionEvent) {
        parent.requestDisallowInterceptTouchEvent(false)
        if (isScaleGesture) {
            scaleImageToFitFrameRect()
        }
    }

    private fun scaleImageToFitFrameRect() {
        val currentDisplayRect = getDisplayRect() ?: return // 获取当前图片显示区域
        if (currentDisplayRect.contains(showRect)) return

        val scaleFactor = calculateScaleFactor(currentDisplayRect) // 计算需要的缩放因子

        // 计算平移距离以居中图片
        val deltaX = showRect.centerX() - currentDisplayRect.centerX()
        val deltaY = showRect.centerY() - currentDisplayRect.centerY()

        cacheMatrix.set(suppMatrix)
        // 创建动画平滑地缩放和平移图片
        scaleAnimator = ObjectAnimator.ofFloat(0f, 1f).apply {
            duration = 200 // 动画持续时间
            interpolator = AccelerateDecelerateInterpolator() // 加速减速插值器
            addUpdateListener { animation ->
                val animatedValue = animation.animatedValue as Float
                val tempScale = 1 + (scaleFactor - 1) * animatedValue // 计算当前帧的缩放因子
                val tempDx = deltaX * animatedValue // 计算当前帧的X轴平移距离
                val tempDy = deltaY * animatedValue // 计算当前帧的Y轴平移距离

                suppMatrix.set(cacheMatrix)
                // 应用缩放和平移
                suppMatrix.postScale(tempScale, tempScale, showRect.centerX(), showRect.centerY())
                suppMatrix.postTranslate(tempDx, tempDy)
                checkAndDisplayMatrix()
            }
            start() // 启动动画
        }
    }

    private fun calculateScaleFactor(rect: RectF): Float {
        val widthRatio = showRect.width() / rect.width()
        val heightRatio = showRect.height() / rect.height()
        return max(widthRatio, heightRatio) // 选择较大的比例以确保完全覆盖
    }

    private fun updateImageMatrix(bitmap: Bitmap) {
        val bRatio = bitmap.width.toFloat() / bitmap.height
        val vRatio = width.toFloat() / height
        if (bRatio > vRatio) {
            val scale = width.toFloat() / bitmap.width
            val dy = (height - bitmap.height * scale) / 2
            baseMatrix.setScale(scale, scale)
            baseMatrix.postTranslate(0f, dy)
            showRect.set(0f, dy, width.toFloat(), height - dy)
        } else {
            val scale = height.toFloat() / bitmap.height
            val dx = (width - bitmap.width * scale) / 2
            baseMatrix.setScale(scale, scale)
            baseMatrix.postTranslate(dx, 0f)
            showRect.set(dx, 0f, width - dx, height.toFloat())
        }
        checkAndDisplayMatrix()
    }

    protected fun getDrawMatrix(): Matrix {
        drawMatrix.set(baseMatrix)
        drawMatrix.postConcat(suppMatrix)
        onBaseImageTransformed(suppMatrix)
        updateTransformImagePoints(drawMatrix)
        return drawMatrix
    }

    /**
     * 当基图进行变换时，通知图层，图层基于基图的变换来进行变换的。
     * @param matrix 基图的变换矩阵
     */
    protected open fun onBaseImageTransformed(matrix: Matrix) {}

    private fun checkAndDisplayMatrix(checkBounds: Boolean = true) {
        if (checkBounds) {
            if (checkMatrixBounds()) {
                getDrawMatrix()
                invalidate()
            }
        } else {
            getDrawMatrix()
            invalidate()
        }
    }

    private fun checkMatrixBounds(): Boolean {
        val rect = getDisplayRect() ?: return false
        val deltaX: Float
        val deltaY: Float

        // 缩放后图片中心与frameRectF中心的差值
        val diffX = showRect.centerX() - rect.centerX()
        val diffY = showRect.centerY() - rect.centerY()

        // 根据图片与frameRectF的大小关系计算deltaX和deltaY
        deltaX = when {
            rect.width() <= showRect.width() -> diffX
            rect.left > showRect.left -> showRect.left - rect.left
            rect.right < showRect.right -> showRect.right - rect.right
            else -> 0f
        }

        deltaY = when {
            rect.height() <= showRect.height() -> diffY
            rect.top > showRect.top -> showRect.top - rect.top
            rect.bottom < showRect.bottom -> showRect.bottom - rect.bottom
            else -> 0f
        }

        // 应用计算出的平移量，以保证缩放后图片以frameRectF的中心为中心
        if (deltaX != 0f || deltaY != 0f) {
            suppMatrix.postTranslate(deltaX, deltaY)
        }
        return true
    }

    fun getDisplayRect(): RectF? {
        val bitmap = viewBitmap ?: return null
        val matrix = getDrawMatrix()
        imageRect.set(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat())
        matrix.mapRect(imageRect)
        return imageRect
    }

    private fun onImageLaidOut() {
        val bitmap = viewBitmap ?: return
        val rect = RectF(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat())
        imageInitialCorners = getCornersFromRect(rect)
        imageInitialCenter = getCenterFromRect(rect)
    }

    private fun updateTransformImagePoints(matrix: Matrix) {
        imageInitialCorners?.let {
            matrix.mapPoints(imageTransformCorners, it)
        }
        imageInitialCenter?.let {
            matrix.mapPoints(imageTransformCenter, it)
        }
    }

    open fun setImageBitmap(bitmap: Bitmap) {
        viewBitmap = bitmap
        onImageLaidOut()
        updateImageMatrix(bitmap)
    }

    private inner class FlingRunnable(context: Context) : Runnable {
        private val mScroller: OverScroller
        private var mCurrentX = 0
        private var mCurrentY = 0

        init {
            mScroller = OverScroller(context)
        }

        fun cancelFling() {
            mScroller.forceFinished(true)
        }

        fun fling(velocityX: Int, velocityY: Int) {
            val rect: RectF = getDisplayRect() ?: return  // 获取当前显示区域

            // 计算滑动起点
            val startX = rect.left.toInt()
            val startY = rect.top.toInt()

            // 设置当前滑动位置
            mCurrentX = startX
            mCurrentY = startY

            // 开始滑动
            mScroller.fling(startX, startY, velocityX, velocityY, Int.MIN_VALUE, Int.MAX_VALUE, Int.MIN_VALUE, Int.MAX_VALUE, 0, 0)
        }

        override fun run() {
            if (mScroller.isFinished) {
                return  // remaining post that should not be handled
            }
            if (mScroller.computeScrollOffset()) {
                val newX = mScroller.currX
                val newY = mScroller.currY
                suppMatrix.postTranslate((mCurrentX - newX).toFloat(), (mCurrentY - newY).toFloat())
                checkAndDisplayMatrix()
                mCurrentX = newX
                mCurrentY = newY
                // Post On animation
                postOnAnimation(this)
            }
        }
    }

}