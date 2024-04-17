package com.example.customviews.view

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.animation.addListener
import com.example.customviews.R
import com.example.customviews.utils.dp2Px
import com.example.customviews.utils.generateRandomColor
import com.google.android.material.color.MaterialColors
import java.util.Locale
import java.util.Random
import kotlin.math.sqrt

data class TurntableInfo(
    val title: String,
    val color: Int = generateRandomColor(),
    val weight: Float = 1f
)

data class TurntableAngle(
    val startAngle: Float,
    val endAngle: Float,
    val info: TurntableInfo,
)

interface OnTurntableListener {
    fun onRotate(info: TurntableInfo)
    fun onRotateEnd(info: TurntableInfo)
}

class TurntableView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var bgColor = 0
    private var gapWidth = 0f
    private var borderWidth = 0f
    private var textColor = 0
    private var textSize = 0f
    private var overlayTextColor = 0
    private var overlayColor = 0
    private var startText = "开始"
    private var startTextSize = 0f
    private var startTextColor = 0
    private var startTextPadding = 0f
    private var startTextBorderWidth = 0f
    private var startTextBgColor = 0
    private var startTextBgColor1 = 0
    private var startTextPressedBgColor = 0
    private var startTextOutBgColor = 0
    private var triangleHypotenuseLength = 0f
    private var triangleVerticalLength = 0f

    private var isReset = true
    private var isPressed = false
    private var currentAngle = 0f
    private var tempPath = Path()
    private var isRotating = false
    private var tempRectF = RectF()
    private var turntableRadius = 0f
    private var textTouchRectF = RectF()
    private var textBgShader: Shader? = null
    private var rotateAnimator: ValueAnimator? = null
    private var destTurntableInfo: TurntableInfo? = null
    private var destTurntableAngle: TurntableAngle? = null
    private var currentTurntableInfo: TurntableInfo? = null
    private val turntableList = mutableListOf<TurntableInfo>()
    private var onTurntableListener: OnTurntableListener? = null
    private val turntableAngles = mutableListOf<TurntableAngle>()

    private var isPlayer1Finish = false
    private val rotateMediaPlayer2 by lazy {
        val resId = resources.getIdentifier("table_spin_1", "raw", context.packageName)
        MediaPlayer.create(context, resId).apply {
            setAudioAttributes(
                AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
        }
    }

    private val rotateMediaPlayer by lazy {
        val resId = resources.getIdentifier("table_spin", "raw", context.packageName)
        MediaPlayer.create(context, resId).apply {
//            val params = playbackParams
//            params.speed = 3f / 6.4f
//            playbackParams = params
            setAudioAttributes(AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
            setOnCompletionListener {
                isPlayer1Finish = true
            }
        }
    }

    private val doneMediaPlayer by lazy {
        val resId = resources.getIdentifier("table_stop", "raw", context.packageName)
        MediaPlayer.create(context, resId).apply {
            setAudioAttributes(AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
        }
    }

    private val textToSpeech by lazy {
        TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                setupSpeechLanguage()
            }
        }
    }

    private fun setupSpeechLanguage() {
        val result = textToSpeech.setLanguage(Locale.CHINA)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.e("sqsong", "The Language is not supported!")
        } else {
            Log.e("sqsong", "The Language is supported!")
        }
    }

    private val shadowPaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isDither = true
            color = bgColor
            setShadowLayer(dp2Px(5), 0f, 0f, Color.parseColor("#4D000000"))
        }
    }

    private val paint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isDither = true
            style = Paint.Style.FILL
            color = bgColor
        }
    }

    private val textPaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isDither = true
            color = textColor
            textSize = this@TurntableView.textSize
        }
    }

    init {
        handleAttributes(context, attrs)
        Log.w("sqsong", "TextSpeech isSpeaking: ${textToSpeech.isSpeaking}")
        turntableList.add(TurntableInfo("0", Color.parseColor("#089EEB")))
        turntableList.add(TurntableInfo("1", Color.parseColor("#F284B6")))
        turntableList.add(TurntableInfo("2", Color.parseColor("#7A65D3")))
        turntableList.add(TurntableInfo("3", Color.parseColor("#D12D7D")))
        turntableList.add(TurntableInfo("4", Color.parseColor("#D0DA05")))
        turntableList.add(TurntableInfo("5", Color.parseColor("#5DBEAC")))
        turntableList.add(TurntableInfo("6", Color.parseColor("#E60011")))
        turntableList.add(TurntableInfo("7", Color.parseColor("#EF8203")))
        turntableList.add(TurntableInfo("8", Color.parseColor("#A7CB0B")))
        turntableList.add(TurntableInfo("9", Color.parseColor("#E84FE6")))

        destTurntableInfo = turntableList[4]
    }

    private fun handleAttributes(context: Context, attrs: AttributeSet?) {
        context.obtainStyledAttributes(attrs, R.styleable.TurntableView).apply {
            borderWidth = getDimension(R.styleable.TurntableView_ttv_borderWidth, dp2Px(6))
            bgColor = getColor(R.styleable.TurntableView_ttv_bgColor, Color.WHITE)
            gapWidth = getDimension(R.styleable.TurntableView_ttv_gapWidth, dp2Px(2))
            textColor = getColor(R.styleable.TurntableView_ttv_textColor, Color.WHITE)
            textSize = getDimension(R.styleable.TurntableView_ttv_textSize, dp2Px(16))
            overlayTextColor = getColor(R.styleable.TurntableView_ttv_overlayTextColor, Color.BLACK)
            overlayColor = getColor(R.styleable.TurntableView_ttv_overlayColor, MaterialColors.compositeARGBWithAlpha(Color.WHITE, 150))
            startText = getString(R.styleable.TurntableView_ttv_startText) ?: "开始"
            startTextSize = getDimension(R.styleable.TurntableView_ttv_startTextSize, dp2Px(26))
            startTextColor = getColor(R.styleable.TurntableView_ttv_startTextColor, Color.WHITE)
            startTextPadding = getDimension(R.styleable.TurntableView_ttv_startTextPadding, dp2Px(14))
            startTextBorderWidth = getDimension(R.styleable.TurntableView_ttv_startTextBorderWidth, dp2Px(6))
            startTextBgColor = getColor(R.styleable.TurntableView_ttv_startTextBgColor, Color.parseColor("#F3C77E"))
            startTextBgColor1 = getColor(R.styleable.TurntableView_ttv_startTextBgColor1, Color.parseColor("#E98A62"))
            startTextPressedBgColor = getColor(R.styleable.TurntableView_ttv_startTextPressedBgColor, Color.parseColor("#D78258"))
            startTextOutBgColor = getColor(R.styleable.TurntableView_ttv_startTextOutBgColor, Color.WHITE)
            triangleHypotenuseLength = getDimension(R.styleable.TurntableView_ttv_triangleHypotenuseLength, dp2Px(24))
            triangleVerticalLength = getDimension(R.styleable.TurntableView_ttv_triangleVerticalLength, dp2Px(12))
            recycle()
        }
    }


    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        turntableRadius = minOf(w, h) / 2f
        textPaint.textSize = startTextSize
        val textWidth = textPaint.measureText(startText)
        val innerRadius = textWidth / 2f + startTextPadding
        tempRectF.set(w / 2f - innerRadius, h / 2f - innerRadius, w / 2f + innerRadius, h / 2f + innerRadius)
        textBgShader = LinearGradient(
            tempRectF.centerX(), tempRectF.centerY() - innerRadius,
            tempRectF.centerX(), tempRectF.centerY() + innerRadius,
            startTextBgColor, startTextBgColor1,
            Shader.TileMode.CLAMP
        )
        textTouchRectF.set(tempRectF)
        composeTurntableAngle()
        detectTurntableInfo(currentAngle)?.let {
            Log.e("sqsong", "onSizeChanged detectTurntableInfo: $it ")
        }
    }

    override fun onDraw(canvas: Canvas) {
        paint.color = bgColor
        paint.style = Paint.Style.FILL
        canvas.drawCircle(width / 2f, height / 2f, turntableRadius, shadowPaint)

        val totalWeight = turntableList.sumOf { it.weight.toDouble() }.toFloat()
        canvas.save()
        canvas.rotate(currentAngle, width / 2f, height / 2f)
        drawArc(canvas, totalWeight)
        drawDividerLine(canvas, totalWeight)
        canvas.restore()

        drawCenterStart(canvas)
    }

    private fun drawArc(canvas: Canvas, totalWeight: Float) {
        var startAngle = 0f
        turntableList.forEach { turntableInfo ->
            paint.style = Paint.Style.FILL
            paint.color = turntableInfo.color
            val sweepAngle = 360 * turntableInfo.weight / totalWeight
            tempRectF.set(width / 2f - turntableRadius, height / 2f - turntableRadius, width / 2f + turntableRadius, height / 2f + turntableRadius)
            tempRectF.inset(borderWidth, borderWidth)
            canvas.drawArc(tempRectF, startAngle, sweepAngle, true, paint)
            startAngle += sweepAngle
        }
    }

    private fun composeTurntableAngle() {
        turntableAngles.clear()
        var startAngle = 0f
        val totalWeight = turntableList.sumOf { it.weight.toDouble() }.toFloat()
        turntableList.forEach { turntableInfo ->
            val sweepAngle = 360 * turntableInfo.weight / totalWeight
            val angle = startAngle + sweepAngle

            val data = TurntableAngle(startAngle, angle, turntableInfo)
            if (destTurntableInfo?.title == turntableInfo.title) {
                destTurntableAngle = data
            }
            turntableAngles.add(data)
            startAngle = angle
        }
    }

    private fun drawDividerLine(canvas: Canvas, totalWeight: Float) {
        var startAngle = 0f
        turntableList.forEach { turntableInfo ->
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = gapWidth
            paint.color = bgColor
            val sweepAngle = 360 * turntableInfo.weight / totalWeight

            val isOverlayMode = !isReset && !isRotating && currentTurntableInfo != turntableInfo

            canvas.save()
            canvas.rotate(startAngle, width / 2f, height / 2f)
            // draw divider line
            canvas.drawLine(width / 2f, height / 2f, width - borderWidth, height / 2f, paint)
            textPaint.color = if (isOverlayMode) overlayTextColor else textColor
            textPaint.textSize = textSize
            val text = turntableInfo.title
            // draw title text
            val textWidth = textPaint.measureText(text)
            canvas.rotate(sweepAngle / 2f, width / 2f, height / 2f)
            val baseline = height / 2f - (textPaint.descent() + textPaint.ascent()) / 2
            val x = width - textWidth - dp2Px<Float>(16)
            canvas.drawText(text, x, baseline, textPaint)
            canvas.restore()

            if (isOverlayMode) {
                paint.color = overlayColor
                paint.style = Paint.Style.FILL
                tempRectF.set(width / 2f - turntableRadius, height / 2f - turntableRadius, width / 2f + turntableRadius, height / 2f + turntableRadius)
                tempRectF.inset(borderWidth, borderWidth)
                canvas.drawArc(tempRectF, startAngle, sweepAngle, true, paint)
            }

            startAngle += sweepAngle
        }
    }

    private fun drawCenterStart(canvas: Canvas) {
        textPaint.textSize = startTextSize
        val textWidth = textPaint.measureText(startText)
        val innerRadius = textWidth / 2f + startTextPadding
        val outRadius = innerRadius + startTextBorderWidth
        shadowPaint.color = startTextOutBgColor

        // draw triangle
        val hypotenuseLength = sqrt(triangleHypotenuseLength * triangleHypotenuseLength - triangleVerticalLength * triangleVerticalLength) / 2f
        tempPath.reset()
        val triangleHorizontalY = height / 2f - outRadius + dp2Px<Float>(2)
        tempPath.moveTo(width / 2f, triangleHorizontalY - triangleVerticalLength)
        tempPath.lineTo(width / 2f - hypotenuseLength, triangleHorizontalY)
        tempPath.lineTo(width / 2f + hypotenuseLength, triangleHorizontalY)

        tempPath.addCircle(width / 2f, height / 2f, outRadius, Path.Direction.CCW)
        tempPath.close()
        canvas.drawPath(tempPath, shadowPaint)

        paint.style = Paint.Style.FILL
        paint.alpha = 255
        if (isPressed) {
            paint.color = startTextPressedBgColor
            canvas.drawCircle(width / 2f, height / 2f, innerRadius, paint)
        } else {
            paint.shader = textBgShader
            canvas.drawCircle(width / 2f, height / 2f, innerRadius, paint)
            paint.shader = null
        }

        val baseline = height / 2f - (textPaint.descent() + textPaint.ascent()) / 2
        val x = width / 2f - textWidth / 2f
        textPaint.color = startTextColor
        canvas.drawText(startText, x, baseline, textPaint)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                if (textTouchRectF.contains(event.x, event.y)) {
                    isPressed = true
                    invalidate()
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (!textTouchRectF.contains(event.x, event.y)) {
                    isPressed = false
                    invalidate()
                }
            }

            MotionEvent.ACTION_UP -> {
                if (isPressed) {
                    isPressed = false
                    invalidate()
                    startRotate()
                }
            }
        }
        return true
    }

    private fun detectTurntableInfo(angle: Float): TurntableInfo? {
        // 将当前角度转换为0到360度范围内的值
        val normalizedAngle = ((angle % 360) + 360) % 360
        // 计算正上方位置对应的角度，这里假设是0度
        val targetAngle = (360 - normalizedAngle + 270) % 360 // 调整为以顶部为0度
        turntableAngles.forEach {
            // 调整每个扇形的起始和结束角度以匹配当前的旋转状态
            val adjustedStartAngle = it.startAngle % 360
            val adjustedEndAngle = if (it.endAngle % 360 > adjustedStartAngle) it.endAngle % 360 else it.endAngle % 360 + 360
            if (targetAngle >= adjustedStartAngle && targetAngle < adjustedEndAngle) {
                return it.info
            }
        }
        return null
    }

    private fun calculateDestAngle(): Float {
        val random = Random()
        val destAngle = destTurntableAngle ?: return random.nextInt(360).toFloat()
        val normalizedAngle = (currentAngle + 90f) % 360f
        val a = destAngle.startAngle + (destAngle.endAngle - destAngle.startAngle) / 2f + 90f
        val targetAngle = 306f - a + normalizedAngle

//        Log.w("sqsong", "calculateDestAngle, normalizedAngle: $normalizedAngle, targetAngle: $targetAngle, angle: $angle")
        return targetAngle
    }

    private fun startRotate() {
        // 定义动画开始和结束时的角度。这里示例为0度到360度，代表完整旋转。
        // 可以根据需要调整结束值来增加旋转圈数。
        val startAngle = currentAngle
        val endAngle = currentAngle + 360f * 6 + calculateDestAngle()
        rotateAnimator?.cancel()
        // 创建ValueAnimator对象
        rotateAnimator = ValueAnimator.ofFloat(startAngle, endAngle).apply {
            duration = 6400 // 设置动画持续时间，单位是毫秒。这里设置为5秒
            interpolator = DecelerateInterpolator(3f) // 设置减速插值器，使旋转速度逐渐减慢
            addUpdateListener { animation ->
                val angle = animation.animatedValue as Float
                // 更新视图的旋转角度
                currentAngle = angle
                detectTurntableInfo(currentAngle)?.let {
                    if (currentTurntableInfo != it) {
                        currentTurntableInfo = it
                        onTurntableListener?.onRotate(it)
                        if (isPlayer1Finish) {
                            rotateMediaPlayer2.start()
                        }
                        Log.d("sqsong", "currentTurntableInfo: $currentTurntableInfo, playSound.")
                    }
                }
                // 强制重绘View来更新旋转效果
                invalidate()
            }
            addListener(
                onStart = {
                    isPlayer1Finish = false
                    rotateMediaPlayer.start()
                    isReset = false
                    isRotating = true
                    invalidate()
                },
                onEnd = {
                    isRotating = false
                    detectTurntableInfo(currentAngle)?.let {
                        currentTurntableInfo = it
                        onTurntableListener?.onRotateEnd(it)
                        doneMediaPlayer.start()
                        textToSpeech.speak(it.title, TextToSpeech.QUEUE_FLUSH, null, null)
                        Log.w("sqsong", "onAnimationEnd currentTurntableInfo: $currentTurntableInfo")
                    }
                    invalidate()
                })
            start()
        }
    }

    fun setOnTurntableListener(listener: OnTurntableListener) {
        onTurntableListener = listener
    }

    fun resetTurntable() {
        isReset = true
        currentAngle = 0f
        currentTurntableInfo = null
        invalidate()
    }

    override fun onDetachedFromWindow() {
        textToSpeech.stop()
        textToSpeech.shutdown()
        super.onDetachedFromWindow()
    }

}