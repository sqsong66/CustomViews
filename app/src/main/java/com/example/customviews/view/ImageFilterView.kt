package com.example.customviews.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.util.AttributeSet
import android.view.View
import com.example.customviews.R

class ImageFilterView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var srcBitmap: Bitmap? = null
    private var filterBitmap: Bitmap? = null

    private var bitmapCanvas = Canvas()

    private val paint  by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isDither = true
            isFilterBitmap = true
        }
    }

    init {
        val bitmap = BitmapFactory.decodeResource(resources, R.drawable.img_sample02, BitmapFactory.Options().apply { inMutable = true })
        // 创建与bitmap尺寸相同的filterBitmap
        val fBitmap = BitmapFactory.decodeResource(resources, R.drawable.filter_01)
        val sr = bitmap.width.toFloat() / bitmap.height.toFloat()
        val fr = fBitmap.width.toFloat() / fBitmap.height.toFloat()
        val scale = if (sr > fr) {
            bitmap.width.toFloat() / fBitmap.width.toFloat()
        } else {
            bitmap.height.toFloat() / fBitmap.height.toFloat()
        }
        val matrix = Matrix()
        matrix.postScale(scale, scale)
        val scaledBitmap = Bitmap.createBitmap(fBitmap, 0, 0, fBitmap.width, fBitmap.height, matrix, true)
        filterBitmap = Bitmap.createBitmap(scaledBitmap, 0, 0, bitmap.width, bitmap.height)
        srcBitmap = bitmap

        bitmapCanvas = Canvas(srcBitmap!!)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.OVERLAY)
        bitmapCanvas.drawBitmap(filterBitmap!!, 0f, 0f, paint)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        paint.xfermode = null
        srcBitmap?.let {
            canvas.drawBitmap(it, 0f, 0f, paint)
        }
    }

}