package com.example.customviews.view

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.example.customviews.R
import com.example.customviews.utils.dp2Px
import com.example.customviews.utils.getThemeColor
import kotlin.math.max

class WatermarkMaterialView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var radius = 0f
    private var mode: Int = 0
    private var checked = false
    private var iconSize: Float = 0f
    private var borderColor: Int = 0
    private var borderWidth: Float = 0f
    private var borderDashGap: Float = 0f
    private var checkedBorderColor: Int = 0
    private var iconBitmap: Bitmap? = null
    private var imageBitmap: Bitmap? = null
    private var checkedOverlayColor: Int = 0
    private var bgImageBitmap: Bitmap? = null

    private var clipPath = Path()
    private var tempRect = RectF()
    private val bgMatrix = Matrix()
    private val imageMatrix = Matrix()

    private val dashPathEffect by lazy {
        DashPathEffect(floatArrayOf(borderDashGap, borderDashGap), 0f)
    }

    private val checkedIconColorFilter by lazy {
        PorterDuffColorFilter(checkedBorderColor, PorterDuff.Mode.SRC_IN)
    }

    private val paint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = borderWidth
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
        }
    }

    private val imagePaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isDither = true
            isFilterBitmap = true
        }
    }

    init {
        handleAttributes(context, attrs)

        val rippleColor = getThemeColor(context, com.google.android.material.R.attr.colorControlHighlight)
        val maskDrawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius
            setColor(Color.BLACK)
        }
        val rippleDrawable = RippleDrawable(ColorStateList.valueOf(rippleColor), null, maskDrawable)
        foreground = rippleDrawable
    }

    private fun handleAttributes(context: Context, attrs: AttributeSet?) {
        context.obtainStyledAttributes(attrs, R.styleable.WatermarkMaterialView).apply {
            mode = getInt(R.styleable.WatermarkMaterialView_wmv_mode, 0) // 0-icon mode 1-image mode
            iconSize = getDimension(R.styleable.WatermarkMaterialView_wmv_icon_size, dp2Px(24))
            val iconRes = getResourceId(R.styleable.WatermarkMaterialView_wmv_icon, R.drawable.ic_add_image)
            if (iconRes != 0) {
                iconBitmap = AppCompatResources.getDrawable(context, iconRes)?.toBitmap()?.let { bitmap ->
                    if (bitmap.width != iconSize.toInt() || bitmap.height != iconSize.toInt()) {
                        val b = Bitmap.createScaledBitmap(bitmap, iconSize.toInt(), iconSize.toInt(), true)
                        bitmap.recycle()
                        b
                    } else {
                        bitmap
                    }
                }
            }
            val imageRes = getResourceId(R.styleable.WatermarkMaterialView_wmv_image, R.drawable.img_sample)
            imageBitmap = BitmapFactory.decodeResource(context.resources, imageRes)
            borderWidth = getDimension(R.styleable.WatermarkMaterialView_wmv_border_width, dp2Px(1))
            borderDashGap = getDimension(R.styleable.WatermarkMaterialView_wmv_border_dash_gap, dp2Px(4))
            borderColor = getColor(R.styleable.WatermarkMaterialView_wmv_border_color, ContextCompat.getColor(context, R.color.color4C8C8B99))
            checkedBorderColor = getColor(R.styleable.WatermarkMaterialView_wmv_checked_border_color, ContextCompat.getColor(context, R.color.color5555FF))
            checkedOverlayColor = getColor(R.styleable.WatermarkMaterialView_wmv_checked_overlay_color, ContextCompat.getColor(context, R.color.color1A5555FF))
            checked = getBoolean(R.styleable.WatermarkMaterialView_wmv_checked, false)
            radius = getDimension(R.styleable.WatermarkMaterialView_wmv_radius, dp2Px(8))
            val bgRes = getResourceId(R.styleable.WatermarkMaterialView_wmv_bg_image, R.drawable.img_transparent_bg)
            if (bgRes != 0) {
                bgImageBitmap = BitmapFactory.decodeResource(context.resources, bgRes)
            }
            recycle()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        tempRect.set(0f, 0f, w.toFloat(), h.toFloat())
        clipPath.reset()
        clipPath.addRoundRect(tempRect, radius, radius, Path.Direction.CW)
        initialImageMatrix()
    }

    private fun initialImageMatrix() {
        imageMatrix.reset()
        if (mode == 0) {
            iconBitmap?.let { bitmap ->
                imageMatrix.postTranslate((width - bitmap.width) / 2f, (height - bitmap.height) / 2f)
            }
        } else {
            imageBitmap?.let { bitmap ->
                // 以centerCrop模式填充显示Bitmap
                val scale = max(width.toFloat() / bitmap.width, height.toFloat() / bitmap.height)
                val dx = (width - bitmap.width * scale) / 2f
                val dy = (height - bitmap.height * scale) / 2f
                imageMatrix.setScale(scale, scale)
                imageMatrix.postTranslate(dx, dy)
            }

            bgImageBitmap?.let { bitmap ->
                val scale = max(width.toFloat() / bitmap.width, height.toFloat() / bitmap.height)
                bgMatrix.setScale(scale, scale)
                bgMatrix.postTranslate((width - bitmap.width * scale) / 2f, (height - bitmap.height * scale) / 2f)
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.clipPath(clipPath)
        if (mode == 0) {
            drawIconMode(canvas)
        } else {
            drawImageMode(canvas)
        }

        // draw border
        tempRect.set(0f, 0f, width.toFloat(), height.toFloat())
        tempRect.inset(borderWidth / 2f, borderWidth / 2f)
        paint.color = if (checked) checkedBorderColor else borderColor
        paint.style = Paint.Style.STROKE
        paint.pathEffect = if (checked || mode == 1) null else dashPathEffect
        canvas.drawRoundRect(tempRect, radius, radius, paint)
    }

    private fun drawIconMode(canvas: Canvas) {
        if (checked) {
            paint.color = checkedOverlayColor
            paint.style = Paint.Style.FILL
            paint.pathEffect = null
            canvas.drawRoundRect(tempRect, radius, radius, paint)
        }

        iconBitmap?.let {
            imagePaint.colorFilter = if (checked) checkedIconColorFilter else null
            canvas.drawBitmap(it, imageMatrix, imagePaint)
        }


    }

    private fun drawImageMode(canvas: Canvas) {
        imagePaint.colorFilter = null
        bgImageBitmap?.let { bitmap ->
            canvas.drawBitmap(bitmap, bgMatrix, imagePaint)
        }
        imageBitmap?.let {
            canvas.drawBitmap(it, imageMatrix, imagePaint)
        }
    }

}