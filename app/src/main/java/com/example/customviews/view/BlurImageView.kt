package com.example.customviews.view

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.annotation.IntDef
import com.example.customviews.R
import com.example.customviews.utils.dp2Px
import com.example.customviews.utils.getThemeColor
import com.example.customviews.utils.getThemeColorWithAlpha
import com.example.customviews.utils.range
import com.github.chrisbanes.photoview.CustomGestureDetector
import com.github.chrisbanes.photoview.GestureListenerAdapter
import com.sqsong.opengllib.common.EglBuffer
import com.sqsong.opengllib.filters.GaussianBlurImageFilter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.math.min
import kotlin.math.sqrt

class BlurImageView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), CoroutineScope {

    @IntDef(BlurMode.CIRCLE, BlurMode.SQUARE, BlurMode.STRAIGHT_LINE, BlurMode.STAR)
    @Retention(AnnotationRetention.SOURCE)
    annotation class BlurMode {
        companion object {
            const val CIRCLE = 0
            const val SQUARE = 1
            const val STRAIGHT_LINE = 2
            const val STAR = 3
        }
    }

    private var blurRadius: Int = 0
    private var dashGap: Float = 0f
    private var dashColor: Int = 0
    private var minBlurSize: Float = 0f
    private var overlayColor: Int = 0
    private var dashStrokeWidth: Float = 0f
    private var blurMode: Int = BlurMode.CIRCLE
    private var defaultBlurSizeFactor: Float = 0.0f
    private var blurImageScaleFactor: Float = 0.0f

    private var blurSize: Float = 0f
    private var maxBlurSize: Float = 0f

    private var rotateAngle = 0f
    private var overlayAlpha = 0f
    private val tempRect = RectF()
    private val tempPath = Path()
    private val imageRect = RectF()
    private var isTouchDown = false
    private var coroutineJob = Job()
    private var isScaleGesture = true
    private val centerPoint = PointF()
    private var sqrtLength: Float = 0f
    private var isChangingRadius = false
    private var blurCanvas: Canvas? = null
    private var imageBitmap: Bitmap? = null
    private var imageMatrix: Matrix = Matrix()
    private var blurImageMatrix: Matrix = Matrix()
    private var tempMatrix: Matrix = Matrix()
    private var blurBitmap: Bitmap? = null
    private val newPath by lazy { Path() }
    private var displayBlurBitmap: Bitmap? = null
    private var alphaAnimator: ValueAnimator? = null
    private val clearXfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)

    private val eglBuffer by lazy { EglBuffer() }

    private val dashPathEffect by lazy {
        DashPathEffect(floatArrayOf(dashGap, dashGap), 0f)
    }

    private val overlayPaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isDither = true
            strokeWidth = dashStrokeWidth
        }
    }

    private val imagePaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isDither = true
            isFilterBitmap = true
        }
    }

    private val blurPaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isDither = true
            isFilterBitmap = true
            xfermode = clearXfermode
        }
    }

    private val onGestureListener = object : GestureListenerAdapter() {
        override fun onDrag(x: Float, y: Float, dx: Float, dy: Float, pointerCount: Int) {
            onViewDrag(dx, dy)
        }

        override fun onScale(scaleFactor: Float, focusX: Float, focusY: Float) {
            onScale(scaleFactor, focusX, focusY, 0f, 0f)
        }

        override fun onScale(scaleFactor: Float, focusX: Float, focusY: Float, dx: Float, dy: Float) {
            isScaleGesture = true
            onViewScale(scaleFactor, dx, dy)
        }

        override fun onRotate(deltaAngle: Float, focusX: Float, focusY: Float) {
            rotateAngle = (rotateAngle + deltaAngle) % 360
            invalidate()
            Log.w("BlurImageView", "onRotate, rotateAngle: $rotateAngle, deltaAngle: $deltaAngle")
        }
    }

    private val scaleDragDetector by lazy {
        CustomGestureDetector(context, onGestureListener)
    }

    override val coroutineContext: CoroutineContext = Dispatchers.Main + coroutineJob

    init {
        handleAttributes(context, attrs)
        setBackgroundColor(Color.TRANSPARENT)
    }

    private fun handleAttributes(context: Context, attrs: AttributeSet?) {
        context.obtainStyledAttributes(attrs, R.styleable.BlurImageView).apply {
            blurRadius = getInt(R.styleable.BlurImageView_biv_blurRadius, 0)
            defaultBlurSizeFactor = getFloat(R.styleable.BlurImageView_biv_defaultBlurSizeFactor, 0.6f)
            check(defaultBlurSizeFactor > 0 && defaultBlurSizeFactor <= 1f) { "defaultBlurSizeFactor must be in (0.0, 1.0]" }
            minBlurSize = getDimension(R.styleable.BlurImageView_biv_minBlurSize, dp2Px(20))
            overlayColor = getColor(R.styleable.BlurImageView_biv_overlayColor, getThemeColorWithAlpha(context, com.google.android.material.R.attr.colorOnSurface, 120))
            dashStrokeWidth = getDimension(R.styleable.BlurImageView_biv_dashStrokeWidth, dp2Px(1))
            dashGap = getDimension(R.styleable.BlurImageView_biv_dashGap, dp2Px(3))
            dashColor = getColor(R.styleable.BlurImageView_biv_dashColor, getThemeColor(context, com.google.android.material.R.attr.colorPrimary))
            blurImageScaleFactor = getFloat(R.styleable.BlurImageView_biv_blurImageScaleFactor, 0.5f)
            check(blurImageScaleFactor > 0 && blurImageScaleFactor <= 1f) { "blurImageScaleFactor must be in (0.0, 1.0]" }
            blurMode = getInt(R.styleable.BlurImageView_biv_blurMode, BlurMode.STAR)
            recycle()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        coroutineJob = Job()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        imageBitmap?.recycle()
        coroutineJob.cancel()
        alphaAnimator?.cancel()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        maxBlurSize = min(width.toFloat(), height.toFloat())
        if (blurSize == 0f) {
            blurSize = maxBlurSize * defaultBlurSizeFactor
        }
        imageBitmap?.let { calculateImageLayout(it) }
        centerPoint.set(width / 2f, height / 2f)
        sqrtLength = sqrt((width * width + height * height).toFloat())
    }

    override fun onDraw(canvas: Canvas) {
        canvas.clipRect(imageRect)
        imageBitmap?.let { canvas.drawBitmap(it, imageMatrix, imagePaint) }
        if ((!isTouchDown || isChangingRadius) && blurRadius > 0) {
            displayBlurBitmap?.let { display ->
                blurBitmap?.let { blur ->
                    drawBlurImage(canvas, display, blur)
                }
            }
        }
        drawOverlay(canvas)
    }

    private fun drawBlurImage(canvas: Canvas, displayBlurBitmap: Bitmap, blurBitmap: Bitmap) {
        blurCanvas?.let { blurCanvas ->
            displayBlurBitmap.eraseColor(Color.TRANSPARENT)
            blurCanvas.drawBitmap(blurBitmap, 0f, 0f, imagePaint)

            tempPath.reset()
            when (blurMode) {
                BlurMode.CIRCLE -> {
                    tempPath.addCircle(centerPoint.x, centerPoint.y, blurSize / 2f, Path.Direction.CW)
                }

                BlurMode.SQUARE -> {
                    tempPath.addRect(centerPoint.x - blurSize / 2f, centerPoint.y - blurSize / 2f, centerPoint.x + blurSize / 2f, centerPoint.y + blurSize / 2f, Path.Direction.CW)
                }

                BlurMode.STRAIGHT_LINE -> {
                    tempRect.set(centerPoint.x - sqrtLength, centerPoint.y - blurSize / 2f, centerPoint.x + sqrtLength, centerPoint.y + blurSize / 2f)
                    tempPath.addRect(tempRect, Path.Direction.CCW)
                }

                BlurMode.STAR -> {
                    tempRect.set(centerPoint.x - blurSize / 2f, centerPoint.y - blurSize / 2f, centerPoint.x + blurSize / 2f, centerPoint.y + blurSize / 2f)
                    val path = getStarPath(newPath, tempRect)
//                    tempMatrix.setRotate(rotateAngle, centerPoint.x, centerPoint.y)
                    tempPath.fillType = Path.FillType.EVEN_ODD
                    tempPath.addPath(path)
                }
            }

            tempMatrix.reset()
            tempMatrix.set(blurImageMatrix)
            tempMatrix.postRotate(-rotateAngle, centerPoint.x, centerPoint.y)
            tempMatrix.invert(tempMatrix)
            tempPath.transform(tempMatrix)
            blurCanvas.drawPath(tempPath, blurPaint)
        }
        canvas.drawBitmap(displayBlurBitmap, blurImageMatrix, imagePaint)
    }

    private fun drawOverlay(canvas: Canvas) {
        Log.e("BlurImageView", "drawOverlay, centerPoint: $centerPoint")
        val destOverlayAlpha: Int = ((overlayAlpha * Color.alpha(overlayColor) / 255f) * 255).toInt()
        overlayPaint.color = Color.argb(destOverlayAlpha, Color.red(overlayColor), Color.green(overlayColor), Color.blue(overlayColor))
        overlayPaint.style = Paint.Style.FILL
        overlayPaint.pathEffect = null

        // 绘制蒙板镂空区域
        tempPath.reset()
        tempPath.addRect(imageRect, Path.Direction.CW)
        when (blurMode) {
            BlurMode.CIRCLE -> {
                tempPath.addCircle(centerPoint.x, centerPoint.y, blurSize / 2f, Path.Direction.CCW)
            }

            BlurMode.STRAIGHT_LINE -> {
                newPath.reset()
                tempRect.set(centerPoint.x - sqrtLength, centerPoint.y - blurSize / 2f, centerPoint.x + sqrtLength, centerPoint.y + blurSize / 2f)
                newPath.addRect(tempRect, Path.Direction.CCW)
                tempMatrix.setRotate(rotateAngle, centerPoint.x, centerPoint.y)
                tempPath.addPath(newPath, tempMatrix)
            }

            BlurMode.SQUARE -> {
                tempRect.set(centerPoint.x - blurSize / 2f, centerPoint.y - blurSize / 2f, centerPoint.x + blurSize / 2f, centerPoint.y + blurSize / 2f)
                newPath.reset()
                newPath.addRect(tempRect, Path.Direction.CCW)
                tempMatrix.setRotate(rotateAngle, centerPoint.x, centerPoint.y)
                tempPath.addPath(newPath, tempMatrix)
            }

            BlurMode.STAR -> {
                tempRect.set(centerPoint.x - blurSize / 2f, centerPoint.y - blurSize / 2f, centerPoint.x + blurSize / 2f, centerPoint.y + blurSize / 2f)
                val path = getStarPath(newPath, tempRect)
                tempMatrix.setRotate(rotateAngle, centerPoint.x, centerPoint.y)
                tempPath.fillType = Path.FillType.EVEN_ODD
                tempPath.addPath(path, tempMatrix)
            }
        }
        canvas.drawPath(tempPath, overlayPaint)

        // 绘制蒙板镂空边框线
        val dashAlpha = ((overlayAlpha * (Color.alpha(dashColor) / 255f)) * 255).toInt()
        overlayPaint.color = Color.argb(dashAlpha, Color.red(dashColor), Color.green(dashColor), Color.blue(dashColor))
        overlayPaint.style = Paint.Style.STROKE
        overlayPaint.pathEffect = dashPathEffect
        canvas.save()
        canvas.rotate(rotateAngle, centerPoint.x, centerPoint.y)
        when (blurMode) {
            BlurMode.CIRCLE -> {
                canvas.drawCircle(centerPoint.x, centerPoint.y, blurSize / 2f - dashStrokeWidth / 2f, overlayPaint)
            }

            BlurMode.SQUARE -> {
                canvas.drawRect(tempRect, overlayPaint)
            }

            BlurMode.STRAIGHT_LINE -> {
                canvas.drawRect(tempRect, overlayPaint)
            }

            BlurMode.STAR -> {
                val path = getStarPath(newPath, tempRect)
                canvas.drawPath(path, overlayPaint)
            }
        }
        canvas.restore()
    }

    private fun getStarPath(path: Path, rectF: RectF): Path {
        val width = rectF.width()
        val height = rectF.height()
        path.reset()
        path.moveTo(rectF.left + width / 2f, rectF.top + height / 5f)
        // Upper left path
        path.cubicTo(
            rectF.left + 5f * width / 14f, rectF.top + 0f,
            rectF.left + 0f, rectF.top + height / 15f,
            rectF.left + width / 28f, rectF.top + 2f * height / 5f
        )

        // Lower left path
        path.cubicTo(
            rectF.left + width / 14f, rectF.top + 2f * height / 3f,
            rectF.left + 3f * width / 7f, rectF.top + 5f * height / 6f,
            rectF.left + width / 2f, rectF.top + height
        )

        // Lower right path
        path.cubicTo(
            rectF.left + 4f * width / 7f, rectF.top + 5f * height / 6f,
            rectF.left + 13f * width / 14f, rectF.top + 2f * height / 3f,
            rectF.left + 27f * width / 28f, rectF.top + 2f * height / 5f
        )

        // Upper right path
        path.cubicTo(
            rectF.left + width, rectF.top + height / 15f,
            rectF.left + 9f * width / 14f, rectF.top + 0f,
            rectF.left + width / 2f, rectF.top + height / 5f
        )
        return path
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                alphaAnimator?.cancel()
                isScaleGesture = false
                isTouchDown = true
                overlayAlpha = 1f
                invalidate()
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isTouchDown = false
                startAlphaAnimation()
            }
        }
        scaleDragDetector.onTouchEvent(event)
        return true
    }

    private fun startAlphaAnimation() {
        alphaAnimator?.cancel()
        alphaAnimator = ValueAnimator.ofFloat(1f, 0f).apply {
            duration = 1000
            interpolator = LinearInterpolator()
            addUpdateListener {
                overlayAlpha = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun onViewDrag(dx: Float, dy: Float) {
        Log.w("BlurImageView", "onViewDrag, dx: $dx, dy: $dy, isScaleGesture: $isScaleGesture")
        if (isScaleGesture) return
        moveCenterPoint(dx, dy)
        invalidate()
    }

    private fun onViewScale(scaleFactor: Float, dx: Float, dy: Float) {
        blurSize *= scaleFactor
        blurSize = blurSize.coerceIn(minBlurSize, maxBlurSize)

        moveCenterPoint(dx, dy)
        invalidate()
    }

    private fun moveCenterPoint(dx: Float, dy: Float) {
        centerPoint.offset(dx, dy)
        // 检测centerPoint是否在imageRect内
        if (!imageRect.contains(centerPoint.x, centerPoint.y)) {
            centerPoint.set(
                centerPoint.x.coerceAtLeast(imageRect.left),
                centerPoint.y.coerceAtLeast(imageRect.top)
            )
            centerPoint.set(
                centerPoint.x.coerceAtMost(imageRect.right),
                centerPoint.y.coerceAtMost(imageRect.bottom)
            )
        }
    }

    private fun calculateImageLayout(bitmap: Bitmap) {
        if (width == 0 || height == 0) return
        val bRatio = bitmap.width.toFloat() / bitmap.height
        val vRatio = width.toFloat() / height
        val scale = if (bRatio > vRatio) {
            width.toFloat() / bitmap.width
        } else {
            height.toFloat() / bitmap.height
        }
        val tx = (width - bitmap.width * scale) / 2
        val ty = (height - bitmap.height * scale) / 2
        imageMatrix.reset()
        imageMatrix.postScale(scale, scale)
        imageMatrix.postTranslate(tx, ty)
        imageRect.set(tx, ty, tx + bitmap.width * scale, ty + bitmap.height * scale)
    }

    fun setImageBitmap(bitmap: Bitmap) {
        imageBitmap = bitmap
        calculateImageLayout(bitmap)
        blurImage(bitmap)
        invalidate()
    }

    private fun getBlurImageBitmap(srcBitmap: Bitmap): Bitmap? {
        val start = System.currentTimeMillis()
        val matrix = Matrix()
        matrix.postScale(blurImageScaleFactor, blurImageScaleFactor)
        val scaledBitmap = Bitmap.createBitmap(srcBitmap, 0, 0, srcBitmap.width, srcBitmap.height, matrix, true)
        val blurBitmap = eglBuffer.getRenderedBitmap(scaledBitmap, GaussianBlurImageFilter(context, blurRadius, maxBlurRadius = 30))
        scaledBitmap.recycle()
        Log.d("BlurImageView", "scaledBitmap: ${scaledBitmap.width}, ${scaledBitmap.height}, cost time: ${System.currentTimeMillis() - start}ms.")
        return blurBitmap
    }

    private fun blurImage(bitmap: Bitmap) {
        launch {
            withContext(Dispatchers.IO) {
                getBlurImageBitmap(bitmap)
            }?.let {
                blurBitmap = it.copy(it.config, true)
                displayBlurBitmap = it
                calculateBlurImageLayout(it)
                blurCanvas = Canvas(it)
                invalidate()
            }
        }
    }

    private fun calculateBlurImageLayout(bitmap: Bitmap) {
        if (width == 0 || height == 0) return
        val bRatio = bitmap.width.toFloat() / bitmap.height
        val vRatio = width.toFloat() / height
        val scale = if (bRatio > vRatio) {
            width.toFloat() / bitmap.width
        } else {
            height.toFloat() / bitmap.height
        }
        val tx = (width - bitmap.width * scale) / 2
        val ty = (height - bitmap.height * scale) / 2
        blurImageMatrix.reset()
        blurImageMatrix.postScale(scale, scale)
        blurImageMatrix.postTranslate(tx, ty)
    }

    fun setBlurRadius(radius: Float, isChangeEnd: Boolean) { // radius: 0 ~ 1
        Log.w("BlurImageView", "setBlurRadius: $radius, isChangeEnd: $isChangeEnd")
        val newRadius = range(radius, 0f, 30f).toInt()
        if (newRadius == blurRadius && !isChangeEnd) return
        blurRadius = newRadius
        if (!isChangeEnd) {
            alphaAnimator?.cancel()
            isChangingRadius = true
            overlayAlpha = 1f
            invalidate()
        } else {
            imageBitmap?.let {
                blurImage(it)
                isChangingRadius = false
                startAlphaAnimation()
            }
        }
    }

    fun setBlurMode(@BlurMode mode: Int) {
        blurMode = mode
        invalidate()
    }

    fun getResultBlurBitmap(): Bitmap? {
        if (blurRadius == 0) return imageBitmap
        val srcBitmap = imageBitmap?.copy(Bitmap.Config.ARGB_8888, true) ?: return null
        val blurBitmap = this.blurBitmap?.copy(Bitmap.Config.ARGB_8888, true) ?: return null
        val canvas = Canvas(srcBitmap)

        val matrix = Matrix().apply {
            postScale(1f / blurImageScaleFactor, 1f / blurImageScaleFactor)
        }
        val blurCanvas = Canvas(blurBitmap)
        tempPath.reset()
        when (blurMode) {
            BlurMode.CIRCLE -> {
                tempPath.addCircle(centerPoint.x, centerPoint.y, blurSize / 2f, Path.Direction.CW)
            }

            BlurMode.SQUARE -> {
                tempPath.addRect(centerPoint.x - blurSize / 2f, centerPoint.y - blurSize / 2f, centerPoint.x + blurSize / 2f, centerPoint.y + blurSize / 2f, Path.Direction.CW)
            }

            BlurMode.STRAIGHT_LINE -> {
                tempRect.set(centerPoint.x - sqrtLength, centerPoint.y - blurSize / 2f, centerPoint.x + sqrtLength, centerPoint.y + blurSize / 2f)
                tempPath.addRect(tempRect, Path.Direction.CCW)
            }

            BlurMode.STAR -> {
                tempRect.set(centerPoint.x - blurSize / 2f, centerPoint.y - blurSize / 2f, centerPoint.x + blurSize / 2f, centerPoint.y + blurSize / 2f)
                val path = getStarPath(newPath, tempRect)
                tempPath.fillType = Path.FillType.EVEN_ODD
                tempPath.addPath(path)
            }
        }
        tempMatrix.reset()
        tempMatrix.set(blurImageMatrix)
        tempMatrix.postRotate(-rotateAngle, centerPoint.x, centerPoint.y)
        tempMatrix.invert(tempMatrix)
        tempPath.transform(tempMatrix)
        blurCanvas.drawPath(tempPath, blurPaint)
        canvas.drawBitmap(blurBitmap, matrix, imagePaint)
        return srcBitmap
    }
}