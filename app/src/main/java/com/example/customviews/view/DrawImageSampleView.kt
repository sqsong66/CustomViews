package com.example.customviews.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View
import com.example.customviews.R

class DrawImageSampleView@JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val b1 = BitmapFactory.decodeResource(resources, R.drawable.img_sample)
    private var b2: Bitmap? = null

    init {
        val b = BitmapFactory.decodeResource(resources, R.drawable.img_sample02)
        b2 = Bitmap.createBitmap( b.width, b.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(b2!!)
        canvas.drawBitmap(b, 0f, 0f, null)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawBitmap(b1, 0f, 0f, null)
        b2?.let { bitmap ->
            canvas.drawBitmap(bitmap, width / 2f - bitmap.width / 2f, height / 2f - bitmap.height / 2f, null)
        }
    }

    fun getBitmap(): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        draw(canvas)
        return bitmap
    }

}