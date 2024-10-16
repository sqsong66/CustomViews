package com.example.customviews.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.VectorDrawable
import android.util.AttributeSet
import android.view.View
import androidx.annotation.IntDef
import androidx.appcompat.content.res.AppCompatResources
import com.example.customviews.R
import com.example.customviews.utils.dp2Px
import com.example.customviews.utils.getThemeColor

@IntDef(value = [PrivateMode.MODE_SET, PrivateMode.MODE_UNLOCK])
@Retention(AnnotationRetention.SOURCE)
annotation class PrivateMode {
    companion object {
        const val MODE_SET = 0
        const val MODE_UNLOCK = 1
    }
}

class PinPatternSetView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var lockRes = -1
    private var setModeRes = -1
    private var unlockModeRes = -1
    private var modeColor: Int = 0
    private var lockSize: Float = 0f
    private var modeSize: Float = 0f
    private var modeBgColor: Int = 0
    private var modePadding: Float = 0f
    private var setMode: Int = PrivateMode.MODE_SET

    private val lockRectF = RectF()
    private var lockDrawable: VectorDrawable? = null
    private var setModeDrawable: VectorDrawable? = null
    private var unlockModeDrawable: VectorDrawable? = null

    private val paint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isDither = true
            style = Paint.Style.FILL
            color = modeBgColor
        }
    }

    init {
        handleAttributes(context, attrs)
    }

    private fun handleAttributes(context: Context, attrs: AttributeSet?) {
        context.obtainStyledAttributes(attrs, R.styleable.PinPatternSetView).apply {
            setMode = getInt(R.styleable.PinPatternSetView_ppsv_setMode, PrivateMode.MODE_SET)
            lockSize = getDimension(R.styleable.PinPatternSetView_ppsv_lockSize, dp2Px(100))
            modeSize = getDimension(R.styleable.PinPatternSetView_ppsv_modeSize, dp2Px(32))
            modeColor = getColor(R.styleable.PinPatternSetView_ppsv_modeColor, getThemeColor(context, com.google.android.material.R.attr.colorOnSurface))
            modeBgColor = getColor(R.styleable.PinPatternSetView_ppsv_modeBgColor, getThemeColor(context, com.google.android.material.R.attr.colorSurface))
            modePadding = getDimension(R.styleable.PinPatternSetView_ppsv_modePadding, dp2Px(4))
            lockRes = getResourceId(R.styleable.PinPatternSetView_ppsv_lockRes, R.drawable.ic_lock_emoji)
            setModeRes = getResourceId(R.styleable.PinPatternSetView_ppsv_setModeRes, R.drawable.ic_setting_fill)
            unlockModeRes = getResourceId(R.styleable.PinPatternSetView_ppsv_unlockModeRes, R.drawable.ic_fingerprint)

            lockDrawable = AppCompatResources.getDrawable(context, lockRes) as VectorDrawable
            setModeDrawable = (AppCompatResources.getDrawable(context, setModeRes) as VectorDrawable).apply {
                setTint(modeColor)
            }
            unlockModeDrawable = (AppCompatResources.getDrawable(context, unlockModeRes) as VectorDrawable).apply {
                setTint(modeColor)
            }
            recycle()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val modeSizePercent = 0.4f
        val offset = (modeSize + modePadding * 2) * modeSizePercent
        val radius = modeSize / 2f + modePadding
        val size = lockSize + radius - offset
        setMeasuredDimension(size.toInt(), size.toInt())
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        lockDrawable?.setBounds(0, 0, lockSize.toInt(), lockSize.toInt())
        lockRectF.set(0f, 0f, lockSize, lockSize)
        val modeSizePercent = 0.4f
        val offset = (modeSize + modePadding * 2) * modeSizePercent
        lockRectF.inset(offset, offset)
        setModeDrawable?.setBounds((lockRectF.right - modeSize / 2f).toInt(), (lockRectF.bottom - modeSize / 2f).toInt(), (lockRectF.right + modeSize / 2f).toInt(), (lockRectF.bottom + modeSize / 2f).toInt())
        unlockModeDrawable?.setBounds((lockRectF.right - modeSize / 2f).toInt(), (lockRectF.bottom - modeSize / 2f).toInt(), (lockRectF.right + modeSize / 2f).toInt(), (lockRectF.bottom + modeSize / 2f).toInt())
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        lockDrawable?.draw(canvas)
        drawMode(canvas)
    }

    private fun drawMode(canvas: Canvas) {
        val radius = modeSize / 2f + modePadding
        canvas.drawCircle(lockRectF.right, lockRectF.bottom, radius, paint)
        val modeDrawable = if (setMode == PrivateMode.MODE_SET) setModeDrawable else unlockModeDrawable
        modeDrawable?.draw(canvas)
    }
}