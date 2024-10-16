package com.example.customviews.view

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.PixelCopy
import androidx.annotation.IntDef
import androidx.annotation.RequiresApi
import androidx.core.graphics.ColorUtils
import com.example.customviews.R
import com.example.customviews.utils.dp2Px
import com.example.customviews.utils.ext.matrixScale
import com.example.customviews.utils.getThemeColor
import com.wangxutech.picwish.libnative.NativeLib
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

@IntDef(value = [SmearMode.BRUSH, SmearMode.LASSO, SmearMode.POLYGON, SmearMode.RECTANGLE])
@Retention(AnnotationRetention.SOURCE)
annotation class SmearMode {
    companion object {
        const val BRUSH = 0
        const val LASSO = 1
        const val POLYGON = 2
        const val RECTANGLE = 3
    }
}

@IntDef(BrushMode.ERASE, BrushMode.RESERVE)
@Retention(AnnotationRetention.SOURCE)
annotation class BrushMode {
    companion object {
        const val ERASE = 0
        const val RESERVE = 1
    }
}

data class LocusPathInfo(val path: Path, val maskPaint: Paint, val cutoutPaint: Paint, val brushMode: Int, val isReset: Boolean = false)

interface ManualCutoutActionListener {
    fun onUndoRedoStateChanged(isUndoEnable: Boolean, isRedoEnable: Boolean)
    fun onPolygonStateChanged(isPolygonUndoEnable: Boolean, isPolyApplyEnable: Boolean)
}

class NewManualCutoutView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ScalableImageView(context, attrs, defStyleAttr) {

    private var brushSize = 0f
    private var minBrushSize = 0f
    private var maxBrushSize = 0f
    private var eraseMaskColor = 0
    private var reserveMaskColor = 0
    private var brushMode = BrushMode.ERASE
    private var touchCircleStrokeWidth = 0f
    private var touchCircleStrokeColor = 0
    private var touchCircleSolidColor = 0
    private var touchPreviewSize = 0f
    private var touchPreviewStrokeWidth = 0f
    private var touchPreviewStrokeColor = 0
    private var touchPreviewRadius = 0f
    private var touchHorizontalMargin = 0f
    private var touchVerticalMargin = 0f
    private var dashLineColor = 0

    private var isTouched = false
    private val locusPath = Path()
    private var maskBitmap: Bitmap? = null
    private var maskCanvas: Canvas? = null
    private val dashLinePath by lazy { Path() }
    private var cutoutBitmap: Bitmap? = null
    private var cutoutCanvas: Canvas? = null
    private var previewBitmap: Bitmap? = null
    private var drawMaskBitmap: Bitmap? = null
    private var drawCutoutBitmap: Bitmap? = null
    private val touchDownPoint by lazy { PointF() }
    private val mappedTouchDownPoint by lazy { PointF() }
    private val polygonPoints by lazy { mutableListOf<PointF>() }

    private var lastX = 0f
    private var lastY = 0f
    private var polygonLength = 0f
    private val tempPath = Path()
    private val tempRectF = RectF()
    private val tempMatrix = Matrix()

    private val tempPoint = FloatArray(2)

    // 放大镜预览图在屏幕上截图Rect
    private val viewShotRect = Rect()

    @SmearMode
    private var smearMode = SmearMode.BRUSH

    private var showBrushSize = false

    var isDrawPath = false
        private set

    // 当前View在屏幕上的位置
    private val viewLocation = IntArray(2)
    private val undoPathList = mutableListOf<LocusPathInfo>()
    private val redoPathList = mutableListOf<LocusPathInfo>()
    private var manualCutoutActionListener: ManualCutoutActionListener? = null
    private val alphaGridDrawHelper by lazy {
        AlphaGridDrawHelper(
            lightColor = Color.parseColor("#23252F"),
            darkColor = Color.parseColor("#151621")
        )
    }
    private val previewHandler by lazy {
        Handler(Looper.getMainLooper())
    }
    private val viewShotBitmap by lazy {
        Bitmap.createBitmap(touchPreviewSize.toInt(), touchPreviewSize.toInt(), Bitmap.Config.ARGB_8888)
    }

    private val paint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isDither = true
        }
    }

    private val dashLinePaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = dashLineColor
            style = Paint.Style.STROKE
            strokeWidth = dp2Px(2)
            pathEffect = DashPathEffect(floatArrayOf(dp2Px(4), dp2Px(4)), 0f)
        }
    }

    private val imagePaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isDither = true
            isFilterBitmap = true
        }
    }

    private val maskPaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isDither = true
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            strokeWidth = brushSize
            color = if (brushMode == BrushMode.ERASE) eraseMaskColor else reserveMaskColor
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC)
        }
    }

    private val brushPaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isDither = true
            strokeWidth = brushSize
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        }
    }

    init {
        handleAttrs(context, attrs)
    }

    private fun handleAttrs(context: Context, attrs: AttributeSet?) {
        context.obtainStyledAttributes(attrs, R.styleable.NewManualCutoutView).apply {
            brushSize = getDimension(R.styleable.NewManualCutoutView_nmcv_brushSize, dp2Px(20))
            minBrushSize = getDimension(R.styleable.NewManualCutoutView_nmcv_minBrushSize, dp2Px(10))
            maxBrushSize = getDimension(R.styleable.NewManualCutoutView_nmcv_maxBrushSize, dp2Px(50))
            eraseMaskColor = getColor(R.styleable.NewManualCutoutView_nmcv_eraseMaskColor, ColorUtils.setAlphaComponent(Color.WHITE, 200))
            reserveMaskColor = getColor(R.styleable.NewManualCutoutView_nmcv_reserveMaskColor, ColorUtils.setAlphaComponent(Color.WHITE, 120))
            brushMode = getInt(R.styleable.NewManualCutoutView_nmcv_brushMode, BrushMode.ERASE)
            touchCircleStrokeWidth = getDimension(R.styleable.NewManualCutoutView_nmcv_touchCircleStrokeWidth, dp2Px(1))
            touchCircleStrokeColor = getColor(R.styleable.NewManualCutoutView_nmcv_touchCircleStrokeColor, Color.WHITE)
            touchCircleSolidColor = getColor(R.styleable.NewManualCutoutView_nmcv_touchCircleSolidColor, ColorUtils.setAlphaComponent(Color.BLACK, 80))
            touchPreviewSize = getDimension(R.styleable.NewManualCutoutView_nmcv_touchPreviewSize, dp2Px(100))
            touchPreviewStrokeWidth = getDimension(R.styleable.NewManualCutoutView_nmcv_touchPreviewStrokeWidth, dp2Px(2))
            touchPreviewStrokeColor = getColor(R.styleable.NewManualCutoutView_nmcv_touchPreviewStrokeColor, Color.WHITE)
            touchPreviewRadius = getDimension(R.styleable.NewManualCutoutView_nmcv_touchPreviewRadius, dp2Px(8))
            touchHorizontalMargin = getDimension(R.styleable.NewManualCutoutView_nmcv_touchHorizontalMargin, dp2Px(8))
            touchVerticalMargin = getDimension(R.styleable.NewManualCutoutView_nmcv_touchVerticalMargin, dp2Px(8))
            dashLineColor = getColor(R.styleable.NewManualCutoutView_nmcv_dashLineColor, getThemeColor(context, com.google.android.material.R.attr.colorPrimary))
            recycle()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        getLocationOnScreen(viewLocation)
    }

    override fun onDraw(canvas: Canvas) {
        // 绘制透明网格
        if (isScaleGesture) {
            tempRectF.set(0f, 0f, width.toFloat(), height.toFloat())
            alphaGridDrawHelper.drawAlphaGrid(canvas, tempRectF)
        }
        // 非缩放状态绘制原图
        val drawMatrix = getDrawMatrix()
        if (!isScaleGesture) {
            super.onDraw(canvas)
        }
        // 绘制抠图
        drawCutoutBitmap?.let { bitmap ->
            canvas.drawBitmap(bitmap, drawMatrix, imagePaint)
        }
        // 非缩放状态绘制蒙版
        if (!isScaleGesture) {
            drawMaskBitmap?.let { bitmap ->
                canvas.drawBitmap(bitmap, drawMatrix, imagePaint)
            }
        }

        // 绘制区域
        drawDashPath(canvas)

        // 绘制触摸圆圈
        if (isTouched && !isScaleGesture) {
            previewBitmap?.let { drawTouchPreview(canvas, it) }
        }
        if (showBrushSize) drawBrushSize(canvas)
    }

    private fun drawBrushSize(canvas: Canvas) {
        val x = width / 2f
        val y = height / 2f
        val radius = brushSize / 2f
        paint.color = touchCircleSolidColor
        paint.style = Paint.Style.FILL
        canvas.drawCircle(x, y, radius, paint)

        paint.color = touchCircleStrokeColor
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = touchCircleStrokeWidth
        canvas.drawCircle(x, y, radius, paint)
    }

    private fun drawDashPath(canvas: Canvas) {
        when (smearMode) {
            SmearMode.BRUSH -> {
                if (!isTouched || isScaleGesture) return
                drawTouchCircle(canvas)
            }

            SmearMode.LASSO, SmearMode.RECTANGLE -> {
                if (!isTouched || isScaleGesture) return
                canvas.drawPath(dashLinePath, dashLinePaint)
            }

            SmearMode.POLYGON -> {
                Log.d("ManualCutoutView", "drawShape POLYGON: $polygonLength, polygonPoints.size: ${polygonPoints.size}")
                if (polygonPoints.isNotEmpty() && polygonLength < 5f) { // 初始触摸只有一个点则绘制一个点
                    dashLinePaint.style = Paint.Style.FILL
                    canvas.drawCircle(polygonPoints[0].x, polygonPoints[0].y, dp2Px(2), dashLinePaint)
                    dashLinePaint.style = Paint.Style.STROKE
                } else {
                    canvas.drawPath(dashLinePath, dashLinePaint)
                }
            }
        }
    }

    private fun drawTouchCircle(canvas: Canvas) {
        val radius = brushSize / 2f
        paint.color = touchCircleSolidColor
        paint.style = Paint.Style.FILL
        canvas.drawCircle(lastX, lastY, radius, paint)

        paint.color = touchCircleStrokeColor
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = touchCircleStrokeWidth
        canvas.drawCircle(lastX, lastY, radius, paint)
    }

    private fun drawTouchPreview(canvas: Canvas, previewBitmap: Bitmap) {
        canvas.save()
        val left: Float
        val right: Float
        val sizeHeight = touchPreviewSize * 3 / 2
        if (lastX < touchHorizontalMargin + sizeHeight && lastY < touchVerticalMargin + sizeHeight) { // 右侧
            right = width - touchHorizontalMargin
            left = right - touchPreviewSize
        } else { // 左侧
            left = touchHorizontalMargin
            right = left + touchPreviewSize
        }
        tempRectF.set(left, touchVerticalMargin, right, touchVerticalMargin + touchPreviewSize)
        tempPath.reset()
        tempPath.addRoundRect(tempRectF, touchPreviewRadius, touchPreviewRadius, Path.Direction.CW)
        canvas.clipPath(tempPath)
        canvas.drawBitmap(previewBitmap, null, tempRectF, imagePaint)
        canvas.restore()

        paint.color = touchPreviewStrokeColor
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = touchPreviewStrokeWidth
        canvas.drawRoundRect(tempRectF, touchPreviewRadius, touchPreviewRadius, paint)
    }

    override fun onBaseImageTransformed(matrix: Matrix, drawMatrix: Matrix) {
        val matrixScale = drawMatrix.matrixScale()
        maskPaint.strokeWidth = brushSize / matrixScale
        brushPaint.strokeWidth = brushSize / matrixScale
    }

    override fun onTouchDown(event: MotionEvent) {
        super.onTouchDown(event)
        isTouched = true
        doTouchDownAction(event.x, event.y)
        lastX = event.x
        lastY = event.y
    }

    private fun doTouchDownAction(x: Float, y: Float) {
        when (smearMode) {
            SmearMode.BRUSH -> { // 笔刷模式
                maskPaint.style = Paint.Style.STROKE
                brushPaint.style = Paint.Style.STROKE
                mapPoints(x, y).let {
                    locusPath.reset()
                    locusPath.moveTo(it[0], it[1])
                }
            }

            SmearMode.LASSO, SmearMode.RECTANGLE -> {
                touchDownPoint.set(x, y)
                maskPaint.style = Paint.Style.FILL
                brushPaint.style = Paint.Style.FILL
                dashLinePath.reset()
                dashLinePath.moveTo(x, y)
                mapPoints(x, y).let {
                    locusPath.reset()
                    locusPath.moveTo(it[0], it[1])
                    mappedTouchDownPoint.set(it[0], it[1])
                }
            }

            SmearMode.POLYGON -> {
                maskPaint.style = Paint.Style.FILL
                brushPaint.style = Paint.Style.FILL
                linePolygonPath(x, y, polygonPoints.isEmpty())
            }
        }
    }

    private fun doTouchMoveAction(x: Float, y: Float) {
        when (smearMode) {
            SmearMode.BRUSH -> { // 笔刷模式
                val start = mapPoints(lastX, lastY)
                val end = mapPoints(x, y)
                locusPath.quadTo(start[0], start[1], (start[0] + end[0]) / 2, (start[1] + end[1]) / 2)
                drawMaskAndCutout()
            }

            SmearMode.LASSO -> { // 套索模式
                dashLinePath.quadTo(lastX, lastY, (lastX + x) / 2, (lastY + y) / 2)
                val start = mapPoints(lastX, lastY)
                val end = mapPoints(x, y)
                locusPath.quadTo(start[0], start[1], (start[0] + end[0]) / 2, (start[1] + end[1]) / 2)
            }

            SmearMode.POLYGON -> {
                linePolygonPath(x, y, false)
            }

            SmearMode.RECTANGLE -> {
                dashLinePath.reset()
                val startX = min(touchDownPoint.x, x)
                val endX = max(touchDownPoint.x, x)
                val startY = min(touchDownPoint.y, y)
                val endY = max(touchDownPoint.y, y)
                dashLinePath.addRect(startX, startY, endX, endY, Path.Direction.CW)

                locusPath.reset()
                mapPoints(x, y).let { point ->
                    val mapStartX = min(mappedTouchDownPoint.x, point[0])
                    val mapEndX = max(mappedTouchDownPoint.x, point[0])
                    val mapStartY = min(mappedTouchDownPoint.y, point[1])
                    val mapEndY = max(mappedTouchDownPoint.y, point[1])
                    locusPath.addRect(mapStartX, mapStartY, mapEndX, mapEndY, Path.Direction.CW)
                }
            }
        }
    }

    private fun doTouchUpAction(x: Float, y: Float) {
        when (smearMode) {
            SmearMode.BRUSH -> { // 笔刷模式
                mapPoints(x, y).let { locusPath.lineTo(it[0], it[1]) }
                isDrawPath = true
                undoPathList.add(LocusPathInfo(Path(locusPath), Paint(maskPaint), Paint(brushPaint), brushMode))
                redoPathList.clear()
                locusPath.reset()
                manualCutoutActionListener?.onUndoRedoStateChanged(isUndoEnable = undoPathList.isNotEmpty(), isRedoEnable = redoPathList.isNotEmpty())
            }

            SmearMode.LASSO -> { // 套索模式
                mapPoints(x, y).let { point ->
                    locusPath.lineTo(point[0], point[1])
                    locusPath.close()
                    isDrawPath = true
                    undoPathList.add(LocusPathInfo(Path(locusPath), Paint(maskPaint), Paint(brushPaint), brushMode))
                    redoPathList.clear()
                    drawMaskAndCutout()
                    manualCutoutActionListener?.onUndoRedoStateChanged(isUndoEnable = undoPathList.isNotEmpty(), isRedoEnable = redoPathList.isNotEmpty())
                }
            }

            SmearMode.POLYGON -> {
                linePolygonPath(x, y, polygonPoints.isNotEmpty() && polygonLength > 5f)
                manualCutoutActionListener?.onPolygonStateChanged(polygonPoints.isNotEmpty(), polygonPoints.size >= 1 && polygonLength > 5f)
                manualCutoutActionListener?.onUndoRedoStateChanged(isUndoEnable = false, isRedoEnable = false)
            }

            SmearMode.RECTANGLE -> {
                isDrawPath = true
                undoPathList.add(LocusPathInfo(Path(locusPath), Paint(maskPaint), Paint(brushPaint), brushMode))
                redoPathList.clear()
                drawMaskAndCutout()
                manualCutoutActionListener?.onUndoRedoStateChanged(isUndoEnable = undoPathList.isNotEmpty(), isRedoEnable = redoPathList.isNotEmpty())
            }
        }
    }

    private fun drawMaskAndCutout() {
        // 根据模式不同，执行不同的操作
        if (brushMode == BrushMode.RESERVE) {
            // 在 RESERVE 模式下，清除路径上的蒙版
            maskPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            maskCanvas?.drawPath(locusPath, maskPaint)
        } else {
            // 在 ERASE 模式下，绘制蒙版
            maskPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC)
            maskCanvas?.drawPath(locusPath, maskPaint)
        }
        // 绘制抠图图层
        cutoutCanvas?.drawPath(locusPath, brushPaint)
    }

    override fun onTouchUp(event: MotionEvent) {
        super.onTouchUp(event)
        if (!isScaleGesture) {
            doTouchUpAction(event.x, event.y)
        }
        lastX = event.x
        lastY = event.y
        isTouched = false
        isScaleGesture = false
        invalidate()
    }

    override fun onViewFling(startX: Float, startY: Float, velocityX: Float, velocityY: Float) {
        if (isScaleGesture) { // 双指才允许滑动
            super.onViewFling(startX, startY, velocityX, velocityY)
        }
    }

    override fun onViewDrag(x: Float, y: Float, dx: Float, dy: Float, event: MotionEvent) {
        if (event.pointerCount > 1 || isScaleGesture) { // 双指移动
            if (smearMode == SmearMode.POLYGON) {
                if (polygonPoints.size <= 1 && polygonLength < 5f) {
                    dashLinePath.reset()
                    locusPath.reset()
                    polygonPoints.clear()
                    polygonLength = 0f
                } else {
                    linePolygonPath(Float.MIN_VALUE, Float.MIN_VALUE, false)
                }
            }
            super.onViewDrag(x, y, dx, dy, event)
        } else { // 单指绘制
            doTouchMoveAction(x, y)
            lastX = x
            lastY = y
            loadPreviewBitmap(event)
            invalidate()
        }
    }

    private fun linePolygonPath(x: Float, y: Float, addCoord: Boolean) {
        Log.d("ManualCutoutView", "linePolygonPath: $x, $y, addCoord: $addCoord, polygonPoints size: ${polygonPoints.size}")
        dashLinePath.reset()
        if (addCoord) polygonPoints.add(PointF(x, y))

        polygonLength = 0f
        polygonPoints.forEachIndexed { index, pointF ->
            if (index == 0) {
                dashLinePath.moveTo(pointF.x, pointF.y)
            } else {
                dashLinePath.lineTo(pointF.x, pointF.y)
                if (index > 0) {
                    val lastPoint = polygonPoints[index - 1]
                    polygonLength += calculateSpacing(pointF.x, pointF.y, lastPoint.x, lastPoint.y)
                }
            }
        }
        if (!addCoord && x != Float.MIN_VALUE && y != Float.MIN_VALUE) {
            dashLinePath.lineTo(x, y)
            polygonLength += calculateSpacing(x, y, (polygonPoints.lastOrNull()?.x ?: 0f), (polygonPoints.lastOrNull()?.y ?: 0f))
        }
    }

    private fun mapPoints(x: Float, y: Float): FloatArray {
        tempPoint[0] = x
        tempPoint[1] = y
        tempMatrix.reset()
        getDrawMatrix().invert(tempMatrix)
        tempMatrix.mapPoints(tempPoint)
        return tempPoint
    }

    private fun calculateSpacing(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val x = x1 - x2
        val y = y1 - y2
        return sqrt((x * x + y * y).toDouble()).toFloat()
    }

    private fun loadPreviewBitmap(event: MotionEvent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { // 使用PixelCopy来进行截图
            loadPreviewByPixelCopy(event)
        } else {
            loadPreviewByView(event)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadPreviewByPixelCopy(event: MotionEvent) {
        val window = (context as? Activity)?.window ?: return
        // 清除截图Bitmap
        viewShotBitmap.eraseColor(Color.TRANSPARENT)
        // 根据手指触摸点计算出屏幕截图的限制区域，viewShotRect
        limitViewShotRect(event)
        try {
            // 参数1：window即绘制当前界面的窗口
            // 参数2：需要裁剪的窗口区域Rect
            // 参数3：屏幕裁剪区域的图片bitmap
            // 参数4：图片截取成功失败回调
            // 参数5：处理回调(线程)所需的Handler对象
            PixelCopy.request(window, viewShotRect, viewShotBitmap, {
                if (it == PixelCopy.SUCCESS) {
                    previewBitmap = viewShotBitmap
                    invalidate()
                }
            }, previewHandler)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    /**
     * 根据手指触摸位置限制截图区域
     * @param event MotionEvent 手指触摸事件
     */
    private fun limitViewShotRect(event: MotionEvent) {
        val destPreviewSize = touchPreviewSize.toInt()
        var left: Int = (event.rawX - destPreviewSize / 2).toInt()
        var top: Int = (event.rawY - destPreviewSize / 2).toInt()
        if (left < viewLocation[0]) left = viewLocation[0]
        if (top < viewLocation[1]) top = viewLocation[1]
        var right = left + destPreviewSize
        var bottom = top + destPreviewSize
        if (right > viewLocation[0] + width) {
            right = viewLocation[0] + width
            left = right - destPreviewSize
        }
        if (bottom > viewLocation[1] + height) {
            bottom = viewLocation[1] + height
            top = bottom - destPreviewSize
        }
        viewShotRect.set(left, top, right, bottom)
    }

    private fun loadPreviewByView(event: MotionEvent) {
        isDrawingCacheEnabled = true
        drawingCacheQuality = DRAWING_CACHE_QUALITY_HIGH
        val viewShot = drawingCache
        val rectSize = touchPreviewSize
        // 截图bitmap边界检测判断
        var x = event.x - rectSize / 2
        if (x < 0) x = 0f
        if (x > viewShot.width - rectSize) x = viewShot.width - rectSize

        var y = event.y - rectSize / 2
        if (y < 0) y = 0f
        if (y > viewShot.height - rectSize) y = viewShot.height - rectSize

        // 以手指触摸点为中心裁剪截图bitmap进行绘制展示
        previewBitmap = Bitmap.createBitmap(viewShot, x.toInt(), y.toInt(), rectSize.toInt(), rectSize.toInt())
        destroyDrawingCache()
        isDrawingCacheEnabled = false
    }

    fun setCutoutAndMaskBitmap(cutoutBitmap: Bitmap, maskBitmap: Bitmap) {
        this.maskBitmap = maskBitmap
        this.cutoutBitmap = cutoutBitmap
        drawMaskBitmap = Bitmap.createBitmap(maskBitmap.width, maskBitmap.height, Bitmap.Config.ARGB_8888).apply {
            maskCanvas = Canvas(this).apply { drawBitmap(maskBitmap, 0f, 0f, maskPaint) }
        }
        drawCutoutBitmap = Bitmap.createBitmap(cutoutBitmap.width, cutoutBitmap.height, Bitmap.Config.ARGB_8888).apply {
            cutoutCanvas = Canvas(this).apply { drawBitmap(cutoutBitmap, 0f, 0f, imagePaint) }
        }
        invalidate()
    }

    fun resetPaint() {
        resetMaskAndCutout()
        isDrawPath = false
        locusPath.reset()
        redoPathList.clear()
        undoPathList.add(LocusPathInfo(Path(locusPath), Paint(maskPaint), Paint(brushPaint), brushMode, isReset = true))
        manualCutoutActionListener?.onUndoRedoStateChanged(isUndoEnable = undoPathList.isNotEmpty(), isRedoEnable = redoPathList.isNotEmpty())

        invalidate()
    }

    private fun resetMaskAndCutout() {
        // 根据笔刷模式绘制蒙版图层
        maskPaint.color = if (brushMode == BrushMode.ERASE) eraseMaskColor else reserveMaskColor
        maskPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC)

        maskBitmap?.let {
            // 清空画布
            drawMaskBitmap?.eraseColor(Color.TRANSPARENT)
            // 绘制mask蒙版需要使用maskPaint，因为mask蒙版图的颜色是根据maskPaint的颜色来定的
            maskCanvas?.drawBitmap(it, 0f, 0f, maskPaint)
        }

        cutoutBitmap?.let {
            // 清空画布
            drawCutoutBitmap?.eraseColor(Color.TRANSPARENT)
            // 绘制抠图图层的笔刷为普通笔刷或者null都可以
            cutoutCanvas?.drawBitmap(it, 0f, 0f, imagePaint)
        }
    }

    fun setBrushMode(@BrushMode mode: Int) {
        this.brushMode = mode

        // 根据笔刷模式绘制蒙版图层
        /*maskPaint.color = if (brushMode == BrushMode.ERASE) eraseMaskColor else reserveMaskColor
        maskPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC)*/

        // 根据笔刷模式设置抠图笔刷类型：
        // 1. 清除模式设置xfermode为Clear模式，清除掉笔刷的shader
        // 2. 保留模式设置xfermode为null，设置笔刷的shader为原图，则可以通过笔刷将原图绘制到抠图图层上
        if (brushMode == BrushMode.ERASE) {
            brushPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            brushPaint.shader = null
        } else {
            brushPaint.xfermode = null
            brushPaint.shader = viewBitmap?.let { bitmap ->
                BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            }
        }

        // 清空重置蒙版图层和抠图图层
        resetMaskAndCutout()

        // 查找到最近一次重置过的节点，然后将其绘制到蒙版和抠图图层上
        val resetIndex = undoPathList.indexOfLast { it.isReset }
        val resultList = if (resetIndex == -1) {
            undoPathList
        } else {
            undoPathList.subList(resetIndex, undoPathList.size)
        }
        resultList.forEach {
            // 绘制遮罩轨迹
            maskCanvas?.drawPath(it.path, it.maskPaint.apply {
                color = maskPaint.color
                xfermode = if (it.brushMode == BrushMode.ERASE) PorterDuffXfermode(PorterDuff.Mode.SRC) else PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            })
            // 绘制抠图轨迹
            cutoutCanvas?.drawPath(it.path, it.cutoutPaint)
        }
        invalidate()
    }

    fun setSmearMode(@SmearMode mode: Int) {
        smearMode = mode
        dashLinePath.reset()
        locusPath.reset()
        polygonPoints.clear()
        polygonLength = 0f
        manualCutoutActionListener?.onPolygonStateChanged(isPolygonUndoEnable = false, isPolyApplyEnable = false)
        invalidate()
    }

    fun setManualCutoutActionListener(listener: ManualCutoutActionListener) {
        this.manualCutoutActionListener = listener
    }

    fun onPolygonUndo() {
        if (polygonPoints.isEmpty()) return
        polygonPoints.removeLast()
        linePolygonPath(Float.MIN_VALUE, Float.MIN_VALUE, false)
        invalidate()
        manualCutoutActionListener?.onPolygonStateChanged(polygonPoints.isNotEmpty(), polygonPoints.isNotEmpty() && polygonLength > 5f)
        if (polygonPoints.isNotEmpty()) {
            manualCutoutActionListener?.onUndoRedoStateChanged(isUndoEnable = false, isRedoEnable = false)
        } else {
            manualCutoutActionListener?.onUndoRedoStateChanged(undoPathList.isNotEmpty(), redoPathList.isNotEmpty())
        }
    }

    fun onPolygonApply() {
        if (polygonPoints.size < 1 || polygonLength < 5f) return
        locusPath.reset()
        polygonPoints.forEachIndexed { index, pointF ->
            val coor = mapPoints(pointF.x, pointF.y)
            if (index == 0) {
                locusPath.moveTo(coor[0], coor[1])
            } else {
                locusPath.lineTo(coor[0], coor[1])
            }
        }
        locusPath.close()

        drawMaskAndCutout()
        isDrawPath = true
        undoPathList.add(LocusPathInfo(Path(locusPath), Paint(maskPaint), Paint(brushPaint), brushMode))
        redoPathList.clear()
        manualCutoutActionListener?.onUndoRedoStateChanged(undoPathList.isNotEmpty(), redoPathList.isNotEmpty())

        dashLinePath.reset()
        locusPath.reset()
        polygonPoints.clear()
        polygonLength = 0f
        manualCutoutActionListener?.onPolygonStateChanged(isPolygonUndoEnable = false, isPolyApplyEnable = false)
        invalidate()
    }

    fun undo() {
        if (undoPathList.isEmpty()) return
        val latestUndoPath = undoPathList.removeLast()
        redoPathList.add(latestUndoPath)
        resetMaskAndCutout()
        // 查找到最近一次重置过的节点，然后将其绘制到蒙版和抠图图层上
        val resetIndex = undoPathList.indexOfLast { it.isReset }
        val resultList = if (resetIndex == -1) {
            undoPathList
        } else {
            undoPathList.subList(resetIndex, undoPathList.size)
        }
        isDrawPath = resultList.isNotEmpty()
        resultList.forEach {
            // 绘制遮罩轨迹
            maskCanvas?.drawPath(it.path, it.maskPaint.apply {
                color = maskPaint.color
                xfermode = if (it.brushMode == BrushMode.ERASE) PorterDuffXfermode(PorterDuff.Mode.SRC) else PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            })
            // 绘制抠图轨迹
            cutoutCanvas?.drawPath(it.path, it.cutoutPaint)
        }
        invalidate()
        manualCutoutActionListener?.onUndoRedoStateChanged(isUndoEnable = undoPathList.isNotEmpty(), isRedoEnable = redoPathList.isNotEmpty())
    }

    fun redo() {
        if (redoPathList.isEmpty()) return
        val latestRedoPath = redoPathList.removeLast()
        undoPathList.add(latestRedoPath)
        if (latestRedoPath.isReset) {
            isDrawPath = false
            resetMaskAndCutout()
        } else {
            isDrawPath = true
        }

        // 根据模式不同，执行不同的操作
        if (brushMode == BrushMode.RESERVE) {
            // 在 RESERVE 模式下，清除路径上的蒙版
            maskPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            maskCanvas?.drawPath(latestRedoPath.path, latestRedoPath.maskPaint)
        } else {
            // 在 ERASE 模式下，绘制蒙版
            maskPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC)
            maskCanvas?.drawPath(latestRedoPath.path, latestRedoPath.maskPaint)
        }
        // 绘制抠图图层
        cutoutCanvas?.drawPath(latestRedoPath.path, latestRedoPath.cutoutPaint)

        invalidate()
        manualCutoutActionListener?.onUndoRedoStateChanged(isUndoEnable = undoPathList.isNotEmpty(), isRedoEnable = redoPathList.isNotEmpty())
    }

    fun setBrushSize(sizePercent: Float, showBrush: Boolean) {
        brushSize = minBrushSize + (maxBrushSize - minBrushSize) * sizePercent
        showBrushSize = showBrush
        val matrixScale = getDrawMatrix().matrixScale()
        maskPaint.strokeWidth = brushSize / matrixScale
        brushPaint.strokeWidth = brushSize / matrixScale
        invalidate()
    }

    fun getResultCutoutBitmap(): Bitmap? {
        val bitmap = drawCutoutBitmap ?: return null
        val start = System.currentTimeMillis()
        val array = NativeLib.cropPNGImageBitmap(bitmap) // 得到裁剪区域x/y/w/h
        Log.d("songmao", "Find crop region cost: ${System.currentTimeMillis() - start}ms, array: ${array.contentToString()}, bitmap size: ${bitmap.width}x${bitmap.height}")
        if (array.size != 4) return null
        if (array[0] == 0 && array[1] == 0 && array[2] == bitmap.width && array[3] == bitmap.height) {
            return bitmap
        }
        val resultBitmap = Bitmap.createBitmap(bitmap, array[0], array[1], array[2], array[3])
        Log.w("songmao", "getResultCutoutBitmap total cost: ${System.currentTimeMillis() - start}ms")
        return resultBitmap
    }
}