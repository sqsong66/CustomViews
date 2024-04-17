package com.example.customviews.view

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ComposeShader
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.PorterDuff
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.annotation.IntDef
import androidx.core.animation.addListener
import androidx.core.graphics.component1
import androidx.core.graphics.component2
import com.example.customviews.R
import com.example.customviews.utils.dp2Px
import com.example.customviews.utils.getThemeColor
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

@IntDef(TouchType.TOUCH_DOWN, TouchType.TOUCH_MOVE, TouchType.TOUCH_UP)
@Retention(AnnotationRetention.SOURCE)
annotation class TouchType {
    companion object {
        const val TOUCH_DOWN = 0
        const val TOUCH_MOVE = 1
        const val TOUCH_UP = 2
    }
}

@IntDef(value = [TouchMode.TOUCH_NONE, TouchMode.TOUCH_HUE, TouchMode.TOUCH_SAT_VAL, TouchMode.TOUCH_ALPHA])
@Retention(AnnotationRetention.SOURCE)
annotation class TouchMode {
    companion object {
        const val TOUCH_NONE = 0
        const val TOUCH_HUE = 1
        const val TOUCH_SAT_VAL = 2
        const val TOUCH_ALPHA = 3
    }
}

interface OnColorChangedListener {
    fun onColorChanged(color: Int, @TouchType touchType: Int)
}

class ColorPickerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var huePanelStrokeWidth = 0f
    private var hueTrackStrokeWidth = 0f
    private var hueTrackStrokeColor = 0
    private var satValStrokeWidth = 0f
    private var satValStrokeColor = 0
    private var satValRadius = 0f
    private var hueSatValMargin = 0f
    private var minSatValRectLength = 0f
    private var showAlphaPanel = true
    private var alphaPanelGridSize = 0f
    private var hsvAlphaMargin = 0f
    private var alphaTrackColor = 0
    private var hsvHorizontalMargin = 0f
    private var alphaProgressTextColor = 0
    private var alphaProgressTextSize = 0f
    private var alphaProgressCircleRadius = 0f
    private var alphaProgressCircleTextMargin = 0f
    private var alphaProgressPadding = 0f
    private var alphaProgressBgColor = 0
    private var alphaProgressPanelMargin = 0f
    private var showAlphaProgressPanel = false

    private var hue = 0f
    private var sat = 0f
    private var value = 0f
    private var alpha = 255
    private var hsvRect = RectF()
    private var alphaRect = RectF()
    private var satValRect = RectF()
    private val alphaGridColumnCount = 4

    private val tempPath = Path()
    private val tempRect = Rect()
    private val tempPointF = PointF()
    private var progressAnimateAlpha = 255
    private var alphaProgressPanelRadius = 0f
    private var satValShader: Shader? = null
    private var alphaPatternDrawable: AlphaPatternDrawable? = null
    private var colorChangedListener: OnColorChangedListener? = null

    @TouchMode
    private var touchMode = TouchMode.TOUCH_NONE

    private val valShader by lazy {
        LinearGradient(satValRect.left, satValRect.top, satValRect.left, satValRect.bottom, Color.WHITE, Color.BLACK, Shader.TileMode.CLAMP)
    }

    private val hueColorArray by lazy {
        // 创建颜色数组，表示HSV颜色空间的360种颜色
        IntArray(361) { i ->
            Color.HSVToColor(floatArrayOf((360 - i).toFloat(), 1f, 1f))
        }
    }

    private val paint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isDither = true
        }
    }

    private val progressBgPaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isDither = true
            setShadowLayer(dp2Px(8), 0f, 0f, Color.parseColor("#16000000"))
        }
    }

    private val progressCirclePaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isDither = true
        }
    }

    private val huePaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isDither = true
            style = Paint.Style.STROKE
            strokeWidth = huePanelStrokeWidth
        }
    }

    private val satValPaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isDither = true
            setShadowLayer(dp2Px(4), 0f, 0f, Color.parseColor("#3D000000"))
        }
    }

    private val alphaPaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isDither = true
        }
    }

    private val textPaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isDither = true
            textSize = alphaProgressTextSize
            color = alphaProgressTextColor
        }
    }

    init {
        handleAttributes(context, attrs)
        viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                viewTreeObserver.removeOnGlobalLayoutListener(this)
                onInflateFinish()
            }
        })
    }

    private fun handleAttributes(context: Context, attrs: AttributeSet?) {
        context.obtainStyledAttributes(attrs, R.styleable.ColorPickerView).apply {
            huePanelStrokeWidth = getDimension(R.styleable.ColorPickerView_cpv_huePanelStrokeWidth, dp2Px(38))
            hueTrackStrokeWidth = getDimension(R.styleable.ColorPickerView_cpv_hueTrackStrokeWidth, dp2Px(6))
            hueTrackStrokeColor = getColor(R.styleable.ColorPickerView_cpv_hueTrackStrokeColor, Color.WHITE)
            satValStrokeWidth = getDimension(R.styleable.ColorPickerView_cpv_satValStrokeWidth, dp2Px(2))
            satValStrokeColor = getColor(R.styleable.ColorPickerView_cpv_satValStrokeColor, Color.WHITE)
            satValRadius = getDimension(R.styleable.ColorPickerView_cpv_satValRadius, dp2Px(6))
            hueSatValMargin = getDimension(R.styleable.ColorPickerView_cpv_hueSatValMargin, dp2Px(8))
            minSatValRectLength = getDimension(R.styleable.ColorPickerView_cpv_minSatValRectLength, dp2Px(50))
            showAlphaPanel = getBoolean(R.styleable.ColorPickerView_cpv_showAlphaPanel, true)
            alphaPanelGridSize = getDimension(R.styleable.ColorPickerView_cpv_alphaPanelGridSize, dp2Px(5))
            hsvAlphaMargin = getDimension(R.styleable.ColorPickerView_cpv_hsvAlphaMargin, dp2Px(24))
            alphaTrackColor = getColor(R.styleable.ColorPickerView_cpv_alphaTrackColor, getThemeColor(context, com.google.android.material.R.attr.colorSurfaceVariant))
            hsvHorizontalMargin = getDimension(R.styleable.ColorPickerView_cpv_hsvHorizontalMargin, dp2Px(24))
            alphaProgressTextColor = getColor(R.styleable.ColorPickerView_cpv_alphaProgressTextColor, getThemeColor(context, com.google.android.material.R.attr.colorOnSurface))
            alphaProgressTextSize = getDimension(R.styleable.ColorPickerView_cpv_alphaProgressTextSize, dp2Px(12))
            alphaProgressCircleRadius = getDimension(R.styleable.ColorPickerView_cpv_alphaProgressCircleRadius, dp2Px(4))
            alphaProgressCircleTextMargin = getDimension(R.styleable.ColorPickerView_cpv_alphaProgressCircleTextMargin, dp2Px(5))
            alphaProgressPadding = getDimension(R.styleable.ColorPickerView_cpv_alphaProgressPadding, dp2Px(10))
            alphaProgressBgColor = getColor(R.styleable.ColorPickerView_cpv_alphaProgressBgColor, getThemeColor(context, com.google.android.material.R.attr.colorSurfaceVariant))
            alphaProgressPanelMargin = getDimension(R.styleable.ColorPickerView_cpv_alphaProgressPanelMargin, dp2Px(12))
            showAlphaProgressPanel = getBoolean(R.styleable.ColorPickerView_cpv_showAlphaProgressPanel, false)
            recycle()
        }
    }

    private fun onInflateFinish() {
        (parent as? ViewGroup)?.clipChildren = false
        (parent as? ViewGroup)?.clipToPadding = false
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        val minWidth = calculateMinWidth()
        val destWidth = max(width, minWidth)
        val minHeight = destWidth - hsvHorizontalMargin.toInt() * 2 + (if (showAlphaPanel) (alphaGridColumnCount * alphaPanelGridSize + hsvAlphaMargin).toInt() else 0)
        val destHeight = min(height, minHeight)
        setMeasuredDimension(destWidth, destHeight)
    }

    /**
     * 根据色相环的半径、饱和度/明度矩形框的最小尺寸以及它们相应的距离，计算出最小宽度
     */
    private fun calculateMinWidth(): Int {
        val hueRectRadius = sqrt(minSatValRectLength / 2f * minSatValRectLength / 2f * 2)
        val hsvRadius = hueRectRadius + satValRadius + hueSatValMargin + satValStrokeWidth
        val halfWidth = sqrt(hsvRadius * hsvRadius / 2).toInt()
        return halfWidth * 2
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // 创建色相环的SweepGradient实例
        val sweepGradient = SweepGradient(w / 2f, h / 2f, hueColorArray, null)
        huePaint.shader = sweepGradient

        // 得到色相环的矩形区域，方便后续绘制获取到各部分控件的坐标
        val radius = width.coerceAtMost(height) / 2f - hsvHorizontalMargin
        hsvRect.set(width / 2f - radius, 0f, width / 2f + radius, radius * 2f)

        // 计算出饱和度/明度矩形框的矩形区域
        val length = radius - (huePanelStrokeWidth + hueSatValMargin + satValRadius)
        val halfLength = sqrt(length * length / 2)
        satValRect.set(hsvRect.centerX() - halfLength, hsvRect.centerY() - halfLength, hsvRect.centerX() + halfLength, hsvRect.centerY() + halfLength)

        // 根据饱和度/明度矩形框信息来得到饱和度/明度的颜色渐变Shader
        val rgb = Color.HSVToColor(floatArrayOf(hue, 1f, 1f))
        val satShader = LinearGradient(satValRect.left, satValRect.top, satValRect.right, satValRect.top, Color.WHITE, rgb, Shader.TileMode.CLAMP)
        satValShader = ComposeShader(valShader, satShader, PorterDuff.Mode.MULTIPLY)

        // 根据是否显示透明度面板来计算透明度面板的矩形区域
        if (showAlphaPanel) {
            alphaRect.set(0f, height - alphaGridColumnCount * alphaPanelGridSize, width.toFloat(), height.toFloat())
            alphaPatternDrawable = AlphaPatternDrawable(alphaPanelGridSize.roundToInt()).apply {
                setBounds(alphaRect.left.toInt(), alphaRect.top.toInt(), alphaRect.right.toInt(), alphaRect.bottom.toInt())
            }
            alphaProgressPanelRadius = calculateAlphaProgressPanelRadius()
        }
    }

    private fun calculateAlphaProgressPanelRadius(): Float {
        val text = "100%"
        textPaint.getTextBounds(text, 0, text.length, tempRect)
        val textWidth = tempRect.width()
        val textHeight = tempRect.height()
        val panelWidth = textWidth + alphaProgressPadding * 2
        val panelHeight = textHeight + alphaProgressCircleRadius * 2 + alphaProgressPadding * 2 + alphaProgressCircleTextMargin
        return panelWidth.coerceAtLeast(panelHeight) / 2f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val radius = hsvRect.width() / 2f - huePanelStrokeWidth / 2
        drawHuePanel(canvas, radius)
        drawHueTracker(canvas, radius)
        drawSatValPanel(canvas)
        if (showAlphaPanel) {
            drawAlphaPanel(canvas)
        }
    }

    private fun drawHuePanel(canvas: Canvas, radius: Float) {
        canvas.drawCircle(hsvRect.centerX(), hsvRect.centerY(), radius, huePaint)
    }

    private fun drawHueTracker(canvas: Canvas, radius: Float) {
        val angle = Math.toRadians(hue.toDouble())
        val x = hsvRect.centerX() + radius * cos(angle).toFloat()
        val y = hsvRect.centerY() - radius * sin(angle).toFloat()
        paint.color = hueTrackStrokeColor
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = hueTrackStrokeWidth
        val trackerRadius = huePanelStrokeWidth / 2 - hueTrackStrokeWidth / 2
        canvas.drawCircle(x, y, trackerRadius, paint)
    }

    private fun drawSatValPanel(canvas: Canvas) {
        // 绘制饱和度/明度矩形框
        satValShader?.let { shader ->
            satValPaint.shader = shader
            canvas.drawRect(satValRect, satValPaint)
        }

        // 绘制饱和度/明度的跟踪器
        // 绘制饱和度/明度的跟踪器的内圈颜色
        val satValCoor = satValToPointF(sat, value)
        val color = Color.HSVToColor(floatArrayOf(hue, sat, value))
        paint.color = color
        paint.style = Paint.Style.FILL
        canvas.drawCircle(satValCoor.x, satValCoor.y, satValRadius, paint)

        // 绘制饱和度/明度的跟踪器的外圈颜色
        paint.strokeWidth = satValStrokeWidth
        paint.color = satValStrokeColor
        paint.style = Paint.Style.STROKE
        canvas.drawCircle(satValCoor.x, satValCoor.y, satValRadius, paint)
    }

    private fun drawAlphaPanel(canvas: Canvas) {
        // 由于透明度面板是圆角矩形，所以需要对其进行圆角裁剪
        canvas.save()
        tempPath.reset()
        tempPath.addRoundRect(alphaRect, alphaRect.height() / 2f, alphaRect.height() / 2f, Path.Direction.CW)
        canvas.clipPath(tempPath)

        // 绘制透明度面板的背景
        alphaPatternDrawable?.draw(canvas)

        // 绘制渐变透明度颜色
        val hsv = floatArrayOf(hue, sat, value)
        val aColor = Color.HSVToColor(0, hsv)
        val color = Color.HSVToColor(hsv)
        val alphaShader = LinearGradient(alphaRect.left, alphaRect.top, alphaRect.right, alphaRect.top, aColor, color, Shader.TileMode.CLAMP)
        alphaPaint.shader = alphaShader
        canvas.drawRect(alphaRect, alphaPaint)

        // 绘制导航指示点
        val radius = alphaRect.height() / 3f
        val totalWidth = alphaRect.width() - radius * 2 - alphaPanelGridSize * 2
        val left = alphaRect.left + radius + alphaPanelGridSize
        val x = left + totalWidth * this.alpha / 255f
        val y = alphaRect.centerY()
        paint.style = Paint.Style.FILL
        paint.color = alphaTrackColor
        canvas.drawCircle(x, y, radius, paint)
        canvas.restore()

        // 根据配置判断是否显示进度面板
        if (showAlphaProgressPanel) {
            drawAlphaProgressPanel(canvas, x)
        }
    }

    private fun drawAlphaProgressPanel(canvas: Canvas, x: Float) {
        // draw panel background
        val panelCenterY = alphaRect.top - alphaProgressPanelMargin - alphaProgressPanelRadius
        progressBgPaint.style = Paint.Style.FILL
        progressBgPaint.color = Color.argb(progressAnimateAlpha, Color.red(alphaProgressBgColor), Color.green(alphaProgressBgColor), Color.blue(alphaProgressBgColor))
        canvas.drawCircle(x, panelCenterY, alphaProgressPanelRadius, progressBgPaint)

        // draw progress text
        val text = "${(alpha / 255f * 100).roundToInt()}%"
        textPaint.getTextBounds(text, 0, text.length, tempRect)
        val textWidth = tempRect.width()
        val textHeight = tempRect.height()
        val textX = x - textWidth / 2
        val textY = panelCenterY + textHeight / 2f - (textPaint.descent() + textPaint.ascent()) / 2
        textPaint.color = Color.argb(progressAnimateAlpha, Color.red(alphaProgressTextColor), Color.green(alphaProgressTextColor), Color.blue(alphaProgressTextColor))
        canvas.drawText(text, textX, textY, textPaint)

        // draw color circle
        val color = Color.HSVToColor(alpha, floatArrayOf(hue, sat, value))
        val newAlpha = (progressAnimateAlpha / 255f * alpha).toInt()
        progressCirclePaint.color = Color.argb(newAlpha, Color.red(color), Color.green(color), Color.blue(color))
        canvas.drawCircle(x, panelCenterY - alphaProgressCircleTextMargin - alphaProgressCircleRadius, alphaProgressCircleRadius, progressCirclePaint)
    }

    private fun satValToPointF(sat: Float, value: Float): PointF {
        val x = satValRect.left + satValRect.width() * sat
        val y = satValRect.bottom - satValRect.height() * value
        tempPointF.set(x, y)
        return tempPointF
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                // 判断触摸位置，确定是点击到了色相环、饱和度/明度矩形框还是透明度面板
                touchMode = detectTouchMode(event.x, event.y)
                // 如果触摸到以上位置则请求父View不要拦截触摸事件
                parent.requestDisallowInterceptTouchEvent(touchMode != TouchMode.TOUCH_NONE)
                // 如果触摸到透明度面板则显示进度面板
                showAlphaProgressPanel = touchMode == TouchMode.TOUCH_ALPHA
                if (showAlphaProgressPanel) showProgressShowAnim(true)
                moveTrackerIfNeeded(event)
                if (touchMode != TouchMode.TOUCH_NONE) {
                    colorChangedListener?.onColorChanged(Color.HSVToColor(alpha, floatArrayOf(hue, sat, value)), TouchType.TOUCH_DOWN)
                }
            }

            MotionEvent.ACTION_MOVE -> {
                moveTrackerIfNeeded(event)
                if (touchMode != TouchMode.TOUCH_NONE) {
                    colorChangedListener?.onColorChanged(Color.HSVToColor(alpha, floatArrayOf(hue, sat, value)), TouchType.TOUCH_MOVE)
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                parent.requestDisallowInterceptTouchEvent(false)
                moveTrackerIfNeeded(event)
                if (touchMode != TouchMode.TOUCH_NONE) {
                    colorChangedListener?.onColorChanged(Color.HSVToColor(alpha, floatArrayOf(hue, sat, value)), TouchType.TOUCH_UP)
                }
                // 如果透明度面板的进度面板显示中，则隐藏
                touchMode = TouchMode.TOUCH_NONE
                if (showAlphaProgressPanel) showProgressShowAnim(false)
            }
        }
        return touchMode != TouchMode.TOUCH_NONE
    }

    private fun showProgressShowAnim(isShow: Boolean) {
        val start = if (isShow) 0 else 255
        val end = if (isShow) 255 else 0
        ValueAnimator.ofInt(start, end).apply {
            duration = 300
            addUpdateListener {
                progressAnimateAlpha = it.animatedValue as Int
                invalidate()
            }
            addListener(onStart = {
                progressBgPaint.clearShadowLayer()
                invalidate()
            }, onEnd = {
                if (!isShow) showAlphaProgressPanel = false
                progressBgPaint.setShadowLayer(dp2Px(8), 0f, 0f, Color.parseColor("#16000000"))
                invalidate()
            })
            start()
        }
    }

    private fun moveTrackerIfNeeded(event: MotionEvent) {
        when (touchMode) {
            TouchMode.TOUCH_HUE -> {
                hue = calculateHueValue(event.x, event.y, width / 2f, height / 2f)
                val rgb = Color.HSVToColor(floatArrayOf(hue, 1f, 1f))
                val satShader = LinearGradient(satValRect.left, satValRect.top, satValRect.right, satValRect.top, Color.WHITE, rgb, Shader.TileMode.CLAMP)
                satValShader = ComposeShader(valShader, satShader, PorterDuff.Mode.MULTIPLY)
                invalidate()
            }

            TouchMode.TOUCH_SAT_VAL -> {
                val (sat, value) = pointToSatVal(event.x, event.y)
                this.sat = sat
                this.value = value
                invalidate()
            }

            TouchMode.TOUCH_ALPHA -> {
                var x = event.x
                val left = alphaRect.left + alphaRect.height() / 3f + alphaPanelGridSize
                val totalWidth = alphaRect.width() - alphaRect.height() / 3f * 2 - alphaPanelGridSize * 2
                x = max(left, min(x, left + totalWidth))
                val alpha = ((x - left) / totalWidth * 255).toInt()
                this.alpha = alpha
                invalidate()
            }
        }
    }

    private fun pointToSatVal(x: Float, y: Float): PointF {
        val width: Float = satValRect.width()
        val height: Float = satValRect.height()

        var dx = x
        var dy = y
        if (dx < satValRect.left) {
            dx = 0f
        } else if (dx > satValRect.right) {
            dx = width
        } else {
            dx -= satValRect.left
        }

        if (dy < satValRect.top) {
            dy = 0f
        } else if (dy > satValRect.bottom) {
            dy = height
        } else {
            dy -= satValRect.top
        }
        tempPointF.set(1f / width * dx, 1f - 1f / height * dy)
        return tempPointF
    }

    @TouchMode
    private fun detectTouchMode(x: Float, y: Float): Int {
        return when {
            isTouchInsideStrokeCircle(x, y, huePanelStrokeWidth) -> TouchMode.TOUCH_HUE
            satValRect.contains(x, y) -> TouchMode.TOUCH_SAT_VAL
            alphaRect.contains(x, y) -> TouchMode.TOUCH_ALPHA
            else -> TouchMode.TOUCH_NONE
        }
    }

    private fun isTouchInsideStrokeCircle(touchX: Float, touchY: Float, strokeWidth: Float): Boolean {
        val radius = hsvRect.width() / 2f
        val centerX = hsvRect.centerX()
        val centerY = hsvRect.centerY()
        val distance = sqrt(((touchX - centerX) * (touchX - centerX) + (touchY - centerY) * (touchY - centerY)).toDouble())
        val innerRadius = radius - strokeWidth
        return distance in innerRadius..radius
    }

    private fun calculateHueValue(touchX: Float, touchY: Float, circleCenterX: Float, circleCenterY: Float): Float {
        val dx = touchX - circleCenterX
        val dy = touchY - circleCenterY
        // atan2返回的是弧度值，需要转换成度
        var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
        // 将角度转换为0到360度的范围
        angle = if (angle < 0) angle + 360 else angle
        return 360f - angle
    }

    fun setColor(color: Int) {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hue = hsv[0]
        sat = hsv[1]
        value = hsv[2]
        alpha = Color.alpha(color)
        invalidate()
    }

    fun getColor(): Int {
        return Color.HSVToColor(alpha, floatArrayOf(hue, sat, value))
    }

    fun setOnColorChangedListener(listener: OnColorChangedListener) {
        colorChangedListener = listener
    }

}