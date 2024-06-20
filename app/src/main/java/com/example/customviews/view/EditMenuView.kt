package com.example.customviews.view

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.DrawableCompat
import com.example.customviews.R
import com.example.customviews.utils.dp2Px
import com.example.customviews.utils.ext.setRippleForeground
import com.example.customviews.utils.getThemeColor
import com.example.customviews.utils.getThemeColorWithAlpha
import kotlin.math.max
import kotlin.math.min

class EditMenuView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var bgColor = 0
    private var iconRes = 0
    private var iconTint = 0
    private var iconSize = 0f
    private var iconPadding = 0f
    private var checkedBgColor = 0
    private var checkedIconTint = 0
    private var enableCheck = false
    private var enableOverlay = false
    private var overlayColor = 0
    private var overlayText = ""
    private var overlayTextSize = 0f
    private var overlayTextColor = 0
    var checked = false
        private set

    private val iconMatrix = Matrix()
    private val cacheMatrix = Matrix()
    private var iconBitmap: Bitmap? = null
    private var checkedIconBitmap: Bitmap? = null

    private val paint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isDither = true
            isFilterBitmap = true
        }
    }

    private val textPaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isDither = true
            textSize = overlayTextSize
            color = overlayTextColor
        }
    }

    init {
        handleAttributes(context, attrs)
    }

    private fun handleAttributes(context: Context, attrs: AttributeSet?) {
        context.obtainStyledAttributes(attrs, R.styleable.EditMenuView).apply {
            bgColor = getColor(R.styleable.EditMenuView_emv_bgColor, getThemeColorWithAlpha(context, com.google.android.material.R.attr.colorSurfaceVariant, 200))
            iconPadding = getDimension(R.styleable.EditMenuView_emv_iconPadding, dp2Px(16))
            iconSize = getDimension(R.styleable.EditMenuView_emv_iconSize, dp2Px(24))
            iconTint = getColor(R.styleable.EditMenuView_emv_iconTint, getThemeColor(context, com.google.android.material.R.attr.colorOnSurfaceVariant))
            checkedIconTint = getColor(R.styleable.EditMenuView_emv_checkedIconTint, getThemeColor(context, com.google.android.material.R.attr.colorOnPrimary))
            checkedBgColor = getColor(R.styleable.EditMenuView_emv_checkedBgColor, getThemeColorWithAlpha(context, com.google.android.material.R.attr.colorPrimary, 200))
            checked = getBoolean(R.styleable.EditMenuView_emv_checked, false)
            enableCheck = getBoolean(R.styleable.EditMenuView_emv_enableCheck, false)
            enableOverlay = getBoolean(R.styleable.EditMenuView_emv_enableOverlay, false)
            overlayColor = getColor(R.styleable.EditMenuView_emv_overlayColor, getThemeColor(context, com.google.android.material.R.attr.colorPrimary))
            overlayText = getString(R.styleable.EditMenuView_emv_overlayText) ?: "0"
            overlayTextSize = getDimension(R.styleable.EditMenuView_emv_overlayTextSize, dp2Px(16))
            overlayTextColor = getColor(R.styleable.EditMenuView_emv_overlayTextColor, Color.WHITE)
            iconRes = getResourceId(R.styleable.EditMenuView_emv_iconRes, R.drawable.ic_crop_rotate)
            AppCompatResources.getDrawable(context, iconRes)?.let { drawable ->
                DrawableCompat.setTint(drawable, iconTint)
                val bitmap = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
                if (bitmap.width != iconSize.toInt() || bitmap.height != iconSize.toInt()) {
                    iconBitmap = Bitmap.createScaledBitmap(bitmap, iconSize.toInt(), iconSize.toInt(), true)
                    bitmap.recycle()
                } else {
                    iconBitmap = bitmap
                }

                if (enableCheck) {
                    DrawableCompat.setTint(drawable, checkedIconTint)
                    val checkedBitmap = Bitmap.createBitmap(iconBitmap!!.width, iconBitmap!!.height, Bitmap.Config.ARGB_8888)
                    val checkedCanvas = Canvas(checkedBitmap)
                    drawable.draw(checkedCanvas)
                    checkedIconBitmap = checkedBitmap
                }
            }
            resetIconMatrix()
            recycle()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        resetIconMatrix()
        setRippleForeground(max(w, h) / 2f)
    }

    private fun resetIconMatrix() {
        if (width == 0 || height == 0) {
            iconMatrix.reset()
            iconMatrix.postTranslate(iconPadding, iconPadding)
        } else {
            iconBitmap?.let {
                val dx = (width - it.width) / 2f
                val dy = (height - it.height) / 2f
                iconMatrix.reset()
                iconMatrix.postTranslate(dx, dy)
            }
        }
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val size = (iconSize + iconPadding * 2).toInt()
        val width = measureSize(widthMeasureSpec, size)
        val height = measureSize(heightMeasureSpec, size)
        setMeasuredDimension(width, height)
    }

    private fun measureSize(measureSpec: Int, defaultSize: Int): Int {
        val mode = MeasureSpec.getMode(measureSpec)
        val size = MeasureSpec.getSize(measureSpec)
        return when (mode) {
            MeasureSpec.EXACTLY -> size
            MeasureSpec.AT_MOST -> min(defaultSize, size)
            else -> defaultSize
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        paint.color = if (checked) checkedBgColor else bgColor
        val radius = max(width, height) / 2f
        canvas.drawRoundRect(0f, 0f, width.toFloat(), height.toFloat(), radius, radius, paint)
        (if (checked) checkedIconBitmap else iconBitmap)?.let {
            paint.color = iconTint
            canvas.drawBitmap(it, iconMatrix, paint)
        }
        if (enableOverlay) drawOverlay(canvas, radius)
    }

    private fun drawOverlay(canvas: Canvas, radius: Float) {
        paint.color = overlayColor
        canvas.drawRoundRect(0f, 0f, width.toFloat(), height.toFloat(), radius, radius, paint)
        val textWidth = textPaint.measureText(overlayText)
        val x = width / 2f - textWidth / 2
        val baseline = height / 2f - (textPaint.descent() + textPaint.ascent()) / 2
        canvas.drawText(overlayText, x, baseline, textPaint)
    }

    fun setIconRes(iconRes: Int, enableOverlay: Boolean = false, overlayText: String = "") {
        this.iconRes = iconRes
        this.enableOverlay = enableOverlay
        this.overlayText = overlayText
        AppCompatResources.getDrawable(context, iconRes)?.let { drawable ->
            DrawableCompat.setTint(drawable, iconTint)
            val bitmap = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            if (bitmap.width != iconSize.toInt() || bitmap.height != iconSize.toInt()) {
                iconBitmap = Bitmap.createScaledBitmap(bitmap, iconSize.toInt(), iconSize.toInt(), true)
                bitmap.recycle()
            } else {
                iconBitmap = bitmap
            }

            if (enableCheck) {
                DrawableCompat.setTint(drawable, checkedIconTint)
                val checkedBitmap = Bitmap.createBitmap(iconBitmap!!.width, iconBitmap!!.height, Bitmap.Config.ARGB_8888)
                val checkedCanvas = Canvas(checkedBitmap)
                drawable.draw(checkedCanvas)
                checkedIconBitmap = checkedBitmap
            }
            resetIconMatrix()
        }
        invalidate()
    }

    fun flipHorizontal(isFlipHorizontal: Boolean) {
        val start: Float
        val end: Float
        if (isFlipHorizontal) {
            start = 1f
            end = -1f
        } else {
            start = -1f
            end = 1f
        }
        ObjectAnimator.ofFloat(start, end).apply {
            duration = 200
            addUpdateListener {
                val value = it.animatedValue as Float
                iconMatrix.reset()
                iconMatrix.postScale(value, 1f, iconPadding, iconPadding)
                iconMatrix.postTranslate(iconPadding, iconPadding)
                invalidate()
            }
            start()
        }
    }

    fun flipVertical(isFlipVertical: Boolean) {
        val start: Float
        val end: Float
        if (isFlipVertical) {
            start = 1f
            end = -1f
        } else {
            start = -1f
            end = 1f
        }
        ObjectAnimator.ofFloat(start, end).apply {
            duration = 200
            addUpdateListener {
                val value = it.animatedValue as Float
                iconMatrix.reset()
                iconMatrix.postScale(1f, value, iconPadding, iconPadding)
                iconMatrix.postTranslate(iconPadding, iconPadding)
                invalidate()
            }
            start()
        }
    }

    fun rotate(degree: Float = -90f) {
        cacheMatrix.set(iconMatrix)
        ObjectAnimator.ofFloat(0f, degree).apply {
            duration = 200
            addUpdateListener {
                val value = it.animatedValue as Float
                iconMatrix.set(cacheMatrix)
                iconMatrix.postRotate(value, width / 2f, height / 2f)
                invalidate()
            }
            start()
        }
    }

    fun reset() = resetIconMatrix()

    fun setChecked(checked: Boolean) {
        this.checked = checked
        invalidate()
    }

}